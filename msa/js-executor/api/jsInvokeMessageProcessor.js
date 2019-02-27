/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
'use strict';

const config = require('config'),
      logger = require('../config/logger')('JsInvokeMessageProcessor'),
      Utils = require('./utils'),
      js = require('./jsinvoke.proto').js,
      KeyedMessage = require('kafka-node').KeyedMessage,
      JsExecutor = require('./jsExecutor');

const scriptBodyTraceFrequency = Number(config.get('script.script_body_trace_frequency'));

function JsInvokeMessageProcessor(producer) {
    this.producer = producer;
    this.executor = new JsExecutor();
    this.scriptMap = {};
    this.executedScriptsCounter = 0;
}

JsInvokeMessageProcessor.prototype.onJsInvokeMessage = function(message) {

    var requestId;
    try {
        var request = js.RemoteJsRequest.decode(message.value);
        requestId = getRequestId(request);

        logger.debug('[%s] Received request, responseTopic: [%s]', requestId, request.responseTopic);

        if (request.compileRequest) {
            this.processCompileRequest(requestId, request.responseTopic, request.compileRequest);
        } else if (request.invokeRequest) {
            this.processInvokeRequest(requestId, request.responseTopic, request.invokeRequest);
        } else if (request.releaseRequest) {
            this.processReleaseRequest(requestId, request.responseTopic, request.releaseRequest);
        } else {
            logger.error('[%s] Unknown request recevied!', requestId);
        }

    } catch (err) {
        logger.error('[%s] Failed to process request: %s', requestId, err.message);
        logger.error(err.stack);
    }
}

JsInvokeMessageProcessor.prototype.processCompileRequest = function(requestId, responseTopic, compileRequest) {
    var scriptId = getScriptId(compileRequest);
    logger.debug('[%s] Processing compile request, scriptId: [%s]', requestId, scriptId);

    this.executor.compileScript(compileRequest.scriptBody).then(
        (script) => {
            this.scriptMap[scriptId] = script;
            var compileResponse = createCompileResponse(scriptId, true);
            logger.debug('[%s] Sending success compile response, scriptId: [%s]', requestId, scriptId);
            this.sendResponse(requestId, responseTopic, scriptId, compileResponse);
        },
        (err) => {
            var compileResponse = createCompileResponse(scriptId, false, js.JsInvokeErrorCode.COMPILATION_ERROR, err);
            logger.debug('[%s] Sending failed compile response, scriptId: [%s]', requestId, scriptId);
            this.sendResponse(requestId, responseTopic, scriptId, compileResponse);
        }
    );
}

JsInvokeMessageProcessor.prototype.processInvokeRequest = function(requestId, responseTopic, invokeRequest) {
    var scriptId = getScriptId(invokeRequest);
    logger.debug('[%s] Processing invoke request, scriptId: [%s]', requestId, scriptId);
    this.executedScriptsCounter++;
    if ( this.executedScriptsCounter >= scriptBodyTraceFrequency ) {
        this.executedScriptsCounter = 0;
        if (logger.levels[logger.level] >= logger.levels['debug']) {
            logger.debug('[%s] Executing script body: [%s]', scriptId, invokeRequest.scriptBody);
        }
    }
    this.getOrCompileScript(scriptId, invokeRequest.scriptBody).then(
        (script) => {
            this.executor.executeScript(script, invokeRequest.args, invokeRequest.timeout).then(
                (result) => {
                    var invokeResponse = createInvokeResponse(result, true);
                    logger.debug('[%s] Sending success invoke response, scriptId: [%s]', requestId, scriptId);
                    this.sendResponse(requestId, responseTopic, scriptId, null, invokeResponse);
                },
                (err) => {
                    var errorCode;
                    if (err.message.includes('Script execution timed out')) {
                        errorCode = js.JsInvokeErrorCode.TIMEOUT_ERROR;
                    } else {
                        errorCode = js.JsInvokeErrorCode.RUNTIME_ERROR;
                    }
                    var invokeResponse = createInvokeResponse("", false, errorCode, err);
                    logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, errorCode);
                    this.sendResponse(requestId, responseTopic, scriptId, null, invokeResponse);
                }
            )
        },
        (err) => {
            var invokeResponse = createInvokeResponse("", false, js.JsInvokeErrorCode.COMPILATION_ERROR, err);
            logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, js.JsInvokeErrorCode.COMPILATION_ERROR);
            this.sendResponse(requestId, responseTopic, scriptId, null, invokeResponse);
        }
    );
}

JsInvokeMessageProcessor.prototype.processReleaseRequest = function(requestId, responseTopic, releaseRequest) {
    var scriptId = getScriptId(releaseRequest);
    logger.debug('[%s] Processing release request, scriptId: [%s]', requestId, scriptId);
    if (this.scriptMap[scriptId]) {
        delete this.scriptMap[scriptId];
    }
    var releaseResponse = createReleaseResponse(scriptId, true);
    logger.debug('[%s] Sending success release response, scriptId: [%s]', requestId, scriptId);
    this.sendResponse(requestId, responseTopic, scriptId, null, null, releaseResponse);
}

JsInvokeMessageProcessor.prototype.sendResponse = function (requestId, responseTopic, scriptId, compileResponse, invokeResponse, releaseResponse) {
    var remoteResponse = createRemoteResponse(requestId, compileResponse, invokeResponse, releaseResponse);
    var rawResponse = js.RemoteJsResponse.encode(remoteResponse).finish();
    const message = new KeyedMessage(scriptId, rawResponse);
    const payloads = [ { topic: responseTopic, messages: message, key: scriptId } ];
    this.producer.send(payloads, function (err, data) {
        if (err) {
            logger.error('[%s] Failed to send response to kafka: %s', requestId, err.message);
            logger.error(err.stack);
        }
    });
}

JsInvokeMessageProcessor.prototype.getOrCompileScript = function(scriptId, scriptBody) {
    var self = this;
    return new Promise(function(resolve, reject) {
        if (self.scriptMap[scriptId]) {
            resolve(self.scriptMap[scriptId]);
        } else {
            self.executor.compileScript(scriptBody).then(
                (script) => {
                    self.scriptMap[scriptId] = script;
                    resolve(script);
                },
                (err) => {
                    reject(err);
                }
            );
        }
    });
}

function createRemoteResponse(requestId, compileResponse, invokeResponse, releaseResponse) {
    const requestIdBits = Utils.UUIDToBits(requestId);
    return js.RemoteJsResponse.create(
        {
            requestIdMSB: requestIdBits[0],
            requestIdLSB: requestIdBits[1],
            compileResponse: compileResponse,
            invokeResponse: invokeResponse,
            releaseResponse: releaseResponse
        }
    );
}

function createCompileResponse(scriptId, success, errorCode, err) {
    const scriptIdBits = Utils.UUIDToBits(scriptId);
    return js.JsCompileResponse.create(
        {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            scriptIdMSB: scriptIdBits[0],
            scriptIdLSB: scriptIdBits[1]
        }
    );
}

function createInvokeResponse(result, success, errorCode, err) {
    return js.JsInvokeResponse.create(
        {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            result: result
        }
    );
}

function createReleaseResponse(scriptId, success) {
    const scriptIdBits = Utils.UUIDToBits(scriptId);
    return js.JsReleaseResponse.create(
        {
            success: success,
            scriptIdMSB: scriptIdBits[0],
            scriptIdLSB: scriptIdBits[1]
        }
    );
}

function parseJsErrorDetails(err) {
    if (!err) {
        return '';
    }
    var details = err.name + ': ' + err.message;
    if (err.stack) {
        var lines = err.stack.split('\n');
        if (lines && lines.length) {
            var line = lines[0];
            var splitted = line.split(':');
            if (splitted && splitted.length === 2) {
                if (!isNaN(splitted[1])) {
                    details += ' in at line number ' + splitted[1];
                }
            }
        }
    }
    return details;
}

function getScriptId(request) {
    return Utils.toUUIDString(request.scriptIdMSB, request.scriptIdLSB);
}

function getRequestId(request) {
    return Utils.toUUIDString(request.requestIdMSB, request.requestIdLSB);
}

module.exports = JsInvokeMessageProcessor;