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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group",
        configClazz = TbDuplicateMsgToGroupNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to specific Entity Group",
        nodeDetails = "Entities are fetched from Entity Group detected according to the configuration. Entity Group can be specified directly or can be message originator entity itself. " +
                "For each entity from group new message is created with entity as originator and message parameters copied from original message.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDuplicateToGroupConfig",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupNode extends TbAbstractDuplicateMsgToOriginatorsNode {

    private TbDuplicateMsgToGroupNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, EntityId original) {
        return ctx.getPeContext().getEntityGroupService().findAllEntityIds(ctx.getTenantId(), detectTargetEntityGroupId(original), new PageLink(Integer.MAX_VALUE));
    }

    private EntityGroupId detectTargetEntityGroupId(EntityId original) {
        if (config.isEntityGroupIsMessageOriginator()) {
            if (original.getEntityType() == EntityType.ENTITY_GROUP) {
                return new EntityGroupId(original.getId());
            } else {
                throw new RuntimeException("Message originator is not an entity group!");
            }
        } else {
            return config.getEntityGroupId();
        }
    }

    private void validateConfig(TbDuplicateMsgToGroupNodeConfiguration conf) {
        if (!conf.isEntityGroupIsMessageOriginator() && (conf.getEntityGroupId() == null || conf.getEntityGroupId().isNullUid())) {
            log.error("TbDuplicateMsgToGroupNode configuration should have valid Entity Group Id");
            throw new IllegalArgumentException("Wrong configuration for TbDuplicateMsgToGroupNode: Entity Group Id is missing.");
        }
    }

    @Override
    public void destroy() {

    }
}
