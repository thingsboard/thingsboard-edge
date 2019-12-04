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

const vm = require('vm');
const btoa = require('btoa');
const atob = require('atob');

function JsExecutor(useSandbox) {
    this.useSandbox = useSandbox;
}

JsExecutor.prototype.compileScript = function(code) {
    if (this.useSandbox) {
        return createScript(code);
    } else {
        return createFunction(code);
    }
}

JsExecutor.prototype.executeScript = function(script, args, timeout) {
    if (this.useSandbox) {
        return invokeScript(script, args, timeout);
    } else {
        return invokeFunction(script, args);
    }
}

function createScript(code) {
    return new Promise((resolve, reject) => {
        try {
            code = "("+code+")(...args)";
            var script = new vm.Script(code);
            resolve(script);
        } catch (err) {
            reject(err);
        }
    });
}

function invokeScript(script, args, timeout) {
    return new Promise((resolve, reject) => {
        try {
            var sandbox = Object.create(null);
            sandbox.args = args;
            sandbox.btoa = btoa;
            sandbox.atob = atob;
            var result = script.runInNewContext(sandbox, {timeout: timeout});
            resolve(result);
        } catch (err) {
            reject(err);
        }
    });
}


function createFunction(code) {
    return new Promise((resolve, reject) => {
        try {
            code = "return ("+code+")(...args)";
            const parsingContext = vm.createContext({btoa: btoa, atob: atob});
            const func = vm.compileFunction(code, ['args'], {parsingContext: parsingContext});
            resolve(func);
        } catch (err) {
            reject(err);
        }
    });
}

function invokeFunction(func, args) {
    return new Promise((resolve, reject) => {
        try {
            var result = func(args);
            resolve(result);
        } catch (err) {
            reject(err);
        }
    });
}

module.exports = JsExecutor;
