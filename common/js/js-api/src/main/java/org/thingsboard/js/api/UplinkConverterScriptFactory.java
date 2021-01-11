/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            "       var binary_string = atob(bytesBase64);\n" +
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
