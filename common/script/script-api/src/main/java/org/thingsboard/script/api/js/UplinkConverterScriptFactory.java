/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.script.api.js;

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

    private static final String LOCAL_JS_WRAPPER_SUFFIX = "}" +
            "    function convertBytesBase64(bytesBase64) {" +
            "        var binary_string = atob(bytesBase64);" +
            "        try {" +
            "            binary_string = decodeURIComponent(binary_string.split('').map(function (c) {" +
            "                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);" +
            "            }).join(''));" +
            "        } catch (ignored) {}" + // catching URIError: failed to decode input as UTF-8 string, returning raw binary payload
            "        var payload = [];" +
            "        for (var i = 0; i < binary_string.length; i++) {" +
            "            payload.push(binary_string.charCodeAt(i));" +
            "        }" +
            "        return payload;" +
            "    }" +
            "}";

    private static final String REMOTE_JS_WRAPPER_SUFFIX = "}" +
            "    function convertBytesBase64(bytesBase64) {" +
            "        var binary_string = atob(bytesBase64);" +
            "        var raw_payload = Uint8Array.from(binary_string, c => c.charCodeAt(0));" +
            "        binary_string = new TextDecoder().decode(raw_payload);" +
            "        var payload = [];" +
            "        for (var i = 0; i < binary_string.length; i++) {" +
            "            var c = binary_string.charCodeAt(i);" +
            "            if (c === 65533) {" + // 65533 is a replacement char for an unknown char in UTF-8
            "                return Array.from(raw_payload);" + // we failed to properly decode input as text, returning raw payload
            "            }" +
            "            payload.push(c)" +
            "        }" +
            "        return payload;" +
            "    }" +
            "}";

    public static String generateUplinkConverterScript(String functionName, String scriptBody, boolean isLocal) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        String result = jsWrapperPrefix + scriptBody;
        if (isLocal) {
            result = JS_HELPERS_PREFIX_TEMPLATE + result + LOCAL_JS_WRAPPER_SUFFIX;
        } else {
            result = result + REMOTE_JS_WRAPPER_SUFFIX;
        }
        return result;
    }

}
