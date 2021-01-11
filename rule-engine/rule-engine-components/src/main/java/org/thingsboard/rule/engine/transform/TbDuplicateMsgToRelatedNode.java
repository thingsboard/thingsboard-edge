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
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to related",
        configClazz = TbDuplicateMsgToRelatedNodeConfiguration.class,
        nodeDescription = "Duplicates message to related entities fetched by relation query",
        nodeDetails = "Related Entities found using configured relation direction and Relation Type. " +
                "For each found related entity new message is created with related entity as originator and message parameters copied from original message.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDuplicateToRelatedConfig",
        icon = "call_split"
)
public class TbDuplicateMsgToRelatedNode extends TbAbstractDuplicateMsgToOriginatorsNode {

    private TbDuplicateMsgToRelatedNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDuplicateMsgToRelatedNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, EntityId original) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, original, config.getRelationsQuery());
    }

    private void validateConfig(TbDuplicateMsgToRelatedNodeConfiguration conf) {
        if (conf.getRelationsQuery() == null) {
            log.error("TbDuplicateMsgToRelatedNode configuration should have relations query");
            throw new IllegalArgumentException("Wrong configuration for TbDuplicateMsgToRelatedNode: relation query is missing.");
        }
    }

    @Override
    public void destroy() {

    }
}
