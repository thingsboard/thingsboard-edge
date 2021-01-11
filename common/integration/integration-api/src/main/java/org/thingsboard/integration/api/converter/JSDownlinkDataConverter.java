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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 02.12.17.
 */
public class JSDownlinkDataConverter extends AbstractDownlinkDataConverter {

    private final JsInvokeService jsInvokeService;
    private JSDownlinkEvaluator evaluator;

    public JSDownlinkDataConverter(JsInvokeService jsInvokeService) {
        this.jsInvokeService = jsInvokeService;
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        String encoder = configuration.getConfiguration().get("encoder").asText();
        this.evaluator = new JSDownlinkEvaluator(configuration.getTenantId(), jsInvokeService, configuration.getId(), encoder);
    }

    @Override
    public void update(Converter configuration) {
        destroy();
        init(configuration);
    }

    @Override
    public void destroy() {
        if (this.evaluator != null) {
            this.evaluator.destroy();
        }
    }

    @Override
    protected JsonNode doConvertDownlink(TbMsg msg, IntegrationMetaData metadata) throws Exception {
        return evaluator.execute(msg, metadata);
    }

}
