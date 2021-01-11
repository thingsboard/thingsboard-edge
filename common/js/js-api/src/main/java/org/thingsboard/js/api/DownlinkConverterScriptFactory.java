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
public class DownlinkConverterScriptFactory {

    private static final String JS_HELPERS_PREFIX_TEMPLATE = "load('classpath:js/converter-helpers.js'); ";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(msgStr, metadataStr, msgType, integrationMetadataStr) { " +
            "    var msg = JSON.parse(msgStr); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    var integrationMetadata = JSON.parse(integrationMetadataStr); " +
            "    return JSON.stringify(Encoder(msg, metadata, msgType, integrationMetadata));" +
            "    function Encoder(msg, metadata, msgType, integrationMetadata) {";

    private static final String JS_WRAPPER_SUFFIX = "}\n}";

    public static String generateDownlinkConverterScript(String functionName, String scriptBody, boolean isLocal) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        String result = jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
        if (isLocal) {
            result = JS_HELPERS_PREFIX_TEMPLATE + result;
        }
        return result;
    }
}
