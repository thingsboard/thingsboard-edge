/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

public class AttributesScriptFactory {

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(attributesStr) { " +
            "    var attributes = JSON.parse(attributesStr); " +
            "    return JSON.stringify(attributesFunc(attributes));" +
            "    function attributesFunc(attributes) {";
    private static final String JS_WRAPPER_SUFFIX = "}" +
            "\n}";

    public static String generateAttributesScript(String functionName, String scriptBody) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        return jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
    }

}
