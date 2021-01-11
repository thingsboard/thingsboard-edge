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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
public class JSUplinkEvaluator extends AbstractJSEvaluator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public JSUplinkEvaluator(TenantId tenantId, JsInvokeService jsInvokeService, EntityId entityId, String script) {
        super(tenantId, jsInvokeService, entityId, JsScriptType.UPLINK_CONVERTER_SCRIPT, script);
    }

    public String execute(byte[] data, UplinkMetaData metadata) throws Exception {
        validateSuccessfulScriptLazyInit();
        String[] inArgs = prepareArgs(data, metadata);
        return jsInvokeService.invokeFunction(tenantId, this.scriptId, inArgs[0], inArgs[1]).get().toString();
    }

    private static String[] prepareArgs(byte[] data, UplinkMetaData metadata) {
        try {
            String[] args = new String[2];
            args[0] = Base64Utils.encodeToString(data);
            args[1] = mapper.writeValueAsString(metadata.getKvMap());
            return args;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

}
