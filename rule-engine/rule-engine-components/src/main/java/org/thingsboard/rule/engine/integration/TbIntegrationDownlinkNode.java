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
package org.thingsboard.rule.engine.integration;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "integration downlink",
        configClazz = TbIntegrationDownlinkConfiguration.class,
        nodeDescription = "Pushes downlink message to selected integration",
        nodeDetails = "Will push downlink message to the selected integration queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeIntegrationDownlinkConfig",
        icon = "input"
)
public class TbIntegrationDownlinkNode implements TbNode {

    private TbIntegrationDownlinkConfiguration config;
    private IntegrationId integrationId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbIntegrationDownlinkConfiguration.class);
        if (config.getIntegrationId() == null) {
            throw new TbNodeException("Integration id is not set in the rule node configuration!");
        }
        this.integrationId = new IntegrationId(config.getIntegrationId());
        Integration integration = ctx.getPeContext().getIntegrationService().findIntegrationById(ctx.getTenantId(), integrationId);
        if (integration == null) {
            throw new TbNodeException("Integration with ID [" + integrationId + "] not found!");
        } else if (!integration.getTenantId().equals(ctx.getTenantId())) {
            throw new TbNodeException("Integration with ID [" + integrationId + "] belongs to different tenant!");
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (integrationId != null) {
            ctx.getPeContext().pushToIntegration(integrationId, msg, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    ctx.tellNext(msg, TbRelationTypes.SUCCESS);
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }
            });
        } else {
            ctx.tellNext(msg, TbRelationTypes.FAILURE);
        }
    }

    @Override
    public void destroy() {
        integrationId = null;
    }
}
