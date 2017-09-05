/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
const varsRegex = /\$\{([^\}]*)\}/g;

const linkActionRegex = /\<link-act name=['"]([^['"]*)['"]\>([^\<]*)\<\/link-act\>/g;
const buttonActionRegex = /\<button-act name=['"]([^['"]*)['"]\>([^\<]*)\<\/button-act\>/g;

export function processPattern(pattern, datasources, dsIndex) {
    var match = varsRegex.exec(pattern);
    var replaceInfo = {};
    replaceInfo.variables = [];
    while (match !== null) {
        var variableInfo = {};
        variableInfo.dataKeyIndex = -1;
        var variable = match[0];
        var label = match[1];
        var valDec = 2;
        var splitVals = label.split(':');
        if (splitVals.length > 1) {
            label = splitVals[0];
            valDec = parseFloat(splitVals[1]);
        }
        variableInfo.variable = variable;
        variableInfo.valDec = valDec;

        var keyIndex = -1;

        if (label.startsWith('#')) {
            var keyIndexStr = label.substring(1);
            var n = Math.floor(Number(keyIndexStr));
            if (String(n) === keyIndexStr && n >= 0) {
                keyIndex = n;
            }
        }
        var offset = 0;
        for (var i=0;i<datasources.length;i++) {
            var datasource = datasources[i];
            if (dsIndex == i) {
                if (keyIndex > -1) {
                    if (keyIndex < datasource.dataKeys.length) {
                        variableInfo.dataKeyIndex = offset + keyIndex;
                    }
                } else {
                    for (var k = 0; k < datasource.dataKeys.length; k++) {
                        var dataKey = datasource.dataKeys[k];
                        if (dataKey.label === label) {
                            variableInfo.dataKeyIndex = offset + k;
                            break;
                        }
                    }
                }
            }
            offset += datasource.dataKeys.length;
        }
        replaceInfo.variables.push(variableInfo);
        match = varsRegex.exec(pattern);
    }
    return replaceInfo;
}

export function fillPattern(pattern, replaceInfo, data) {
    var text = angular.copy(pattern);
    for (var v = 0; v < replaceInfo.variables.length; v++) {
        var variableInfo = replaceInfo.variables[v];
        var txtVal = '';
        if (variableInfo.dataKeyIndex > -1 && data[variableInfo.dataKeyIndex]) {
            var varData = data[variableInfo.dataKeyIndex].data;
            if (varData.length > 0) {
                var val = varData[varData.length - 1][1];
                if (isNumber(val)) {
                    txtVal = padValue(val, variableInfo.valDec, 0);
                } else {
                    txtVal = val;
                }
            }
        }
        text = text.split(variableInfo.variable).join(txtVal);
    }
    return text;
}

function createLink(actionName, actionText, actionCallbackName, additionalArgs) {
    var args = 'event,\''+actionName+'\'';
    if (additionalArgs && additionalArgs.length) {
        args += ','+additionalArgs.join();
    }
    return '<a href="#" onclick="angular.element(this).scope().'+actionCallbackName+'('+args+'); return false;">'+actionText+'</a>';
}

function createButton(actionName, actionText, actionCallbackName, additionalArgs) {
    var args = 'event,\''+actionName+'\'';
    if (additionalArgs && additionalArgs.length) {
        args += ','+additionalArgs.join();
    }
    return '<button onclick="angular.element(this).scope().'+actionCallbackName+'('+args+'); return false;">'+actionText+'</button>';
}

export function fillPatternWithActions(pattern, actionCallbackName, additionalArgs) {
    var text = angular.copy(pattern);
    var match = linkActionRegex.exec(pattern);
    var actionTags;
    var actionName;
    var actionText;
    var actionHtml;
    while (match !== null) {
        actionTags = match[0];
        actionName = match[1];
        actionText = match[2];
        actionHtml = createLink(actionName, actionText, actionCallbackName, additionalArgs);
        text = text.split(actionTags).join(actionHtml);
        match = linkActionRegex.exec(pattern);
    }
    match = buttonActionRegex.exec(pattern);
    while (match !== null) {
        actionTags = match[0];
        actionName = match[1];
        actionText = match[2];
        actionHtml = createButton(actionName, actionText, actionCallbackName, additionalArgs);
        text = text.split(actionTags).join(actionHtml);
        match = buttonActionRegex.exec(pattern);
    }
    return text;
}

export function toLabelValueMap(data, datasources) {
    var dataMap = {};
    var dsDataMap = [];
    for (var d=0;d<datasources.length;d++) {
        dsDataMap[d] = {};
    }
    for (var i = 0; i < data.length; i++) {
        var dataKey = data[i].dataKey;
        var label = dataKey.label;
        var keyData = data[i].data;
        var val = null;
        if (keyData.length > 0) {
            val = keyData[keyData.length-1][1];
        }
        dataMap[label] = val;
        var dsIndex = datasources.indexOf(data[i].datasource);
        dsDataMap[dsIndex][label] = val;
    }
    return {
        dataMap: dataMap,
        dsDataMap: dsDataMap
    };
}

export function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

export function padValue(val, dec, int) {
    var i = 0;
    var s, strVal, n;

    val = parseFloat(val);
    n = (val < 0);
    val = Math.abs(val);

    if (dec > 0) {
        strVal = val.toFixed(dec).toString().split('.');
        s = int - strVal[0].length;

        for (; i < s; ++i) {
            strVal[0] = '0' + strVal[0];
        }

        strVal = (n ? '-' : '') + strVal[0] + '.' + strVal[1];
    }

    else {
        strVal = Math.round(val).toString();
        s = int - strVal.length;

        for (; i < s; ++i) {
            strVal = '0' + strVal;
        }

        strVal = (n ? '-' : '') + strVal;
    }

    return strVal;
}

export function arraysEqual(a, b) {
    if (a === b) return true;
    if (a === null || b === null) return false;
    if (a.length != b.length) return false;

    for (var i = 0; i < a.length; ++i) {
        if (!a[i].equals(b[i])) return false;
    }
    return true;
}
