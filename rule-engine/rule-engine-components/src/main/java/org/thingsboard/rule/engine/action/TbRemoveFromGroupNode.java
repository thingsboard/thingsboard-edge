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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "remove from group",
        configClazz = TbRemoveFromGroupConfiguration.class,
        nodeDescription = "Removes Message Originator Entity from Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then removes Originator Entity from this group.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRemoveFromGroupConfig",
        icon = "remove_circle"
)
public class TbRemoveFromGroupNode extends TbAbstractGroupActionNode<TbRemoveFromGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return false;
    }

    @Override
    protected TbRemoveFromGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbRemoveFromGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        ctx.getPeContext().getEntityGroupService().removeEntityFromEntityGroup(ctx.getTenantId(), entityGroupId, msg.getOriginator());
    }
}
