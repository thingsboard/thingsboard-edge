/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.service.converter.js;

import com.fasterxml.jackson.core.JsonProcessingException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.UplinkMetaData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

@Slf4j
public class JSDownlinkEvaluator extends AbstractJSEvaluator {

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "load('classpath:js/converter-helpers.js'); function %s(jsonStr, metadata) { " +
            "    var payload = JSON.parse(jsonStr); " +
            "    return JSON.stringify(Encoder(payload, metadata));" +
            "    function Encoder(payload, metadata) {";

    private static final String JS_WRAPPER_SUFFIX = "}\n}";

    private final String functionName;

    public JSDownlinkEvaluator(String encoder) {
        this.functionName = "encodeInternal" + this.hashCode();
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, this.functionName);
        compileScript(jsWrapperPrefix
                + encoder
                + JS_WRAPPER_SUFFIX);
    }

    public void destroy() {
        //engine = null;
    }

    public String execute(String payload, DownLinkMetaData metadata) throws ScriptException, NoSuchMethodException, JsonProcessingException {
        return ((Invocable)engine).invokeFunction(this.functionName, payload, metadata.getKvMap()).toString();
    }

}
