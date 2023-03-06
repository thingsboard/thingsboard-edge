/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
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
        name = "duplicate to specific group",
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
        validateConfig(ctx, config);
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

    private void validateConfig(TbContext ctx, TbDuplicateMsgToGroupNodeConfiguration conf) {
        if (!conf.isEntityGroupIsMessageOriginator()) {
            if (conf.getEntityGroupId() == null || conf.getEntityGroupId().isNullUid()) {
                log.error("TbDuplicateMsgToGroupNode configuration should have valid Entity Group Id");
                throw new IllegalArgumentException("Wrong configuration for TbDuplicateMsgToGroupNode: Entity Group Id is missing.");
            }
            ctx.checkTenantEntity(conf.getEntityGroupId());
        }
    }

}
