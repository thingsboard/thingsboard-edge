/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.js.api;

/**
 * Created by igor on 5/24/18.
 */
public class UplinkConverterScriptFactory {

    private static final String JS_HELPERS_PREFIX_TEMPLATE = "load('classpath:js/converter-helpers.js'); ";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(bytesBase64, metadataStr) { " +
            "    var payload = convertBytesBase64(bytesBase64); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    return JSON.stringify(Decoder(payload, metadata));" +
            "    function Decoder(payload, metadata) {";

    private static final String JS_WRAPPER_SUFFIX = "}" +
            "    function convertBytesBase64(bytesBase64) {\n" +
            "       var binary_string = decodeURIComponent(escape(atob(bytesBase64)));\n" +
            "       var len = binary_string.length;\n"+
            "       var payload = [];\n" +
            "       for (var i = 0; i < len; i++) {\n" +
            "           payload.push(binary_string.charCodeAt(i));\n" +
            "       }\n" +
            "       return payload;\n" +
            "    }\n" +
            "\n}";

    public static String generateUplinkConverterScript(String functionName, String scriptBody, boolean isLocal) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        String result = jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
        if (isLocal) {
            result = JS_HELPERS_PREFIX_TEMPLATE + result;
        }
        return result;
    }
}
