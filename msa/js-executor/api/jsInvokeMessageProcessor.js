/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

const COMPILATION_ERROR = 0;
const RUNTIME_ERROR = 1;
const TIMEOUT_ERROR = 2;
const UNRECOGNIZED = -1;

const config = require('config'),
      logger = require('../config/logger')._logger('JsInvokeMessageProcessor'),
      Utils = require('./utils'),
      JsExecutor = require('./jsExecutor');

const scriptBodyTraceFrequency = Number(config.get('script.script_body_trace_frequency'));
const useSandbox = config.get('script.use_sandbox') === 'true';
const maxActiveScripts = Number(config.get('script.max_active_scripts'));

function JsInvokeMessageProcessor(producer) {
    this.producer = producer;
    this.executor = new JsExecutor(useSandbox);
    this.scriptMap = {};
    this.scriptIds = [];
    this.executedScriptsCounter = 0;
}

JsInvokeMessageProcessor.prototype.onJsInvokeMessage = function(message) {

    var requestId;
    var responseTopic;
    try {
        var request = JSON.parse(message.value.toString('utf8'));
        var buf = message.headers['requestId'];
        requestId = Utils.UUIDFromBuffer(buf);
        buf = message.headers['responseTopic'];
        responseTopic = buf.toString('utf8');

        logger.debug('[%s] Received request, responseTopic: [%s]', requestId, responseTopic);

        if (request.compileRequest) {
            this.processCompileRequest(requestId, responseTopic, request.compileRequest);
        } else if (request.invokeRequest) {
            this.processInvokeRequest(requestId, responseTopic, request.invokeRequest);
        } else if (request.releaseRequest) {
            this.processReleaseRequest(requestId, responseTopic, request.releaseRequest);
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
            this.cacheScript(scriptId, script);
            var compileResponse = createCompileResponse(scriptId, true);
            logger.debug('[%s] Sending success compile response, scriptId: [%s]', requestId, scriptId);
            this.sendResponse(requestId, responseTopic, scriptId, compileResponse);
        },
        (err) => {
            var compileResponse = createCompileResponse(scriptId, false, COMPILATION_ERROR, err);
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
                        errorCode = TIMEOUT_ERROR;
                    } else {
                        errorCode = RUNTIME_ERROR;
                    }
                    var invokeResponse = createInvokeResponse("", false, errorCode, err);
                    logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, errorCode);
                    this.sendResponse(requestId, responseTopic, scriptId, null, invokeResponse);
                }
            )
        },
        (err) => {
            var invokeResponse = createInvokeResponse("", false, COMPILATION_ERROR, err);
            logger.debug('[%s] Sending failed invoke response, scriptId: [%s], errorCode: [%s]', requestId, scriptId, COMPILATION_ERROR);
            this.sendResponse(requestId, responseTopic, scriptId, null, invokeResponse);
        }
    );
}

JsInvokeMessageProcessor.prototype.processReleaseRequest = function(requestId, responseTopic, releaseRequest) {
    var scriptId = getScriptId(releaseRequest);
    logger.debug('[%s] Processing release request, scriptId: [%s]', requestId, scriptId);
    if (this.scriptMap[scriptId]) {
        var index = this.scriptIds.indexOf(scriptId);
        if (index > -1) {
            this.scriptIds.splice(index, 1);
        }
        delete this.scriptMap[scriptId];
    }
    var releaseResponse = createReleaseResponse(scriptId, true);
    logger.debug('[%s] Sending success release response, scriptId: [%s]', requestId, scriptId);
    this.sendResponse(requestId, responseTopic, scriptId, null, null, releaseResponse);
}

JsInvokeMessageProcessor.prototype.sendResponse = function (requestId, responseTopic, scriptId, compileResponse, invokeResponse, releaseResponse) {
    var remoteResponse = createRemoteResponse(requestId, compileResponse, invokeResponse, releaseResponse);
    var rawResponse = Buffer.from(JSON.stringify(remoteResponse), 'utf8');
    this.producer.send(
        {
            topic: responseTopic,
            messages: [
                {
                    key: scriptId,
                    value: rawResponse
                }
            ]
        }
    ).then(
        () => {},
        (err) => {
            if (err) {
                logger.error('[%s] Failed to send response to kafka: %s', requestId, err.message);
                logger.error(err.stack);
            }
        }
    );
}

JsInvokeMessageProcessor.prototype.getOrCompileScript = function(scriptId, scriptBody) {
    var self = this;
    return new Promise(function(resolve, reject) {
        if (self.scriptMap[scriptId]) {
            resolve(self.scriptMap[scriptId]);
        } else {
            self.executor.compileScript(scriptBody).then(
                (script) => {
                    self.cacheScript(scriptId, script);
                    resolve(script);
                },
                (err) => {
                    reject(err);
                }
            );
        }
    });
}

JsInvokeMessageProcessor.prototype.cacheScript = function(scriptId, script) {
    if (!this.scriptMap[scriptId]) {
        this.scriptIds.push(scriptId);
        while (this.scriptIds.length > maxActiveScripts) {
            logger.info('Active scripts count [%s] exceeds maximum limit [%s]', this.scriptIds.length, maxActiveScripts);
            const prevScriptId = this.scriptIds.shift();
            logger.info('Removing active script with id [%s]', prevScriptId);
            delete this.scriptMap[prevScriptId];
        }
    }
    this.scriptMap[scriptId] = script;
}

function createRemoteResponse(requestId, compileResponse, invokeResponse, releaseResponse) {
    const requestIdBits = Utils.UUIDToBits(requestId);
    return {
            requestIdMSB: requestIdBits[0],
            requestIdLSB: requestIdBits[1],
            compileResponse: compileResponse,
            invokeResponse: invokeResponse,
            releaseResponse: releaseResponse
    };
}

function createCompileResponse(scriptId, success, errorCode, err) {
    const scriptIdBits = Utils.UUIDToBits(scriptId);
    return  {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            scriptIdMSB: scriptIdBits[0],
            scriptIdLSB: scriptIdBits[1]
    };
}

function createInvokeResponse(result, success, errorCode, err) {
    return  {
            errorCode: errorCode,
            success: success,
            errorDetails: parseJsErrorDetails(err),
            result: result
    };
}

function createReleaseResponse(scriptId, success) {
    const scriptIdBits = Utils.UUIDToBits(scriptId);
    return {
            success: success,
            scriptIdMSB: scriptIdBits[0],
            scriptIdLSB: scriptIdBits[1]
    };
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

module.exports = JsInvokeMessageProcessor;
