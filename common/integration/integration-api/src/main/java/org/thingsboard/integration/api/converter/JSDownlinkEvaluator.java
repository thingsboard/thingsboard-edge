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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.ScriptException;
import java.util.concurrent.ExecutionException;

@Slf4j
public class JSDownlinkEvaluator extends AbstractJSEvaluator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public JSDownlinkEvaluator(TenantId tenantId, JsInvokeService jsInvokeService, EntityId entityId, String script) {
        super(tenantId, jsInvokeService, entityId, JsScriptType.DOWNLINK_CONVERTER_SCRIPT, script);
    }

    public JsonNode execute(TbMsg msg, IntegrationMetaData metadata) throws ScriptException {
        try {
            validateSuccessfulScriptLazyInit();
            String[] inArgs = prepareArgs(msg, metadata);
            String eval = jsInvokeService.invokeFunction(this.tenantId, this.scriptId, inArgs[0], inArgs[1], inArgs[2], inArgs[3]).get().toString();
            return mapper.readTree(eval);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException)e.getCause();
            } else {
                throw new ScriptException("Failed to execute js script: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ScriptException("Failed to execute js script: " + e.getMessage());
        }
    }

    private static String[] prepareArgs(TbMsg msg, IntegrationMetaData metadata) {
        try {
            String[] args = new String[4];
            if (msg.getData() != null) {
                args[0] = msg.getData();
            } else {
                args[0] = "";
            }
            args[1] = mapper.writeValueAsString(msg.getMetaData().getData());
            args[2] = msg.getType();
            args[3] = mapper.writeValueAsString(metadata.getKvMap());
            return args;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

}
