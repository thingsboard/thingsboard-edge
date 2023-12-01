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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group by name",
        configClazz = TbDuplicateMsgToGroupByNameNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to resolved Entity Group",
        nodeDetails = "Entities are fetched from Entity Group that is detected according to the configuration. " +
                "Entity Group is dynamically resolved based on it's name and type.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDuplicateToGroupByNameConfig",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupByNameNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupByNameNodeConfiguration> {

    @Override
    protected TbDuplicateMsgToGroupByNameNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupByNameNodeConfiguration.class);
        if (Resource.groupResourceFromGroupType(config.getGroupType()) == null) {
            throw new IllegalArgumentException("Entity Type :" + config.getGroupType() + " is not a group entity. " +
                    "Only " + EntityType.GROUP_ENTITY_TYPES + " types are allowed!");
        }
        if (StringUtils.isEmpty(config.getGroupName())) {
            throw new IllegalArgumentException("Group name should be specified!");
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, EntityId original) {
        var entityGroupId = detectTargetEntityGroupId(ctx, original);
        return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), entityGroupId, new PageLink(Integer.MAX_VALUE));
    }

    private EntityGroupId detectTargetEntityGroupId(TbContext ctx, EntityId originator) {
        EntityId ownerId = config.isSearchEntityGroupForTenantOnly() ? ctx.getTenantId() : ctx.getPeContext().getOwner(ctx.getTenantId(), originator);
        return tryFindGroupByOwnerId(ctx, ownerId);
    }

    private EntityGroupId tryFindGroupByOwnerId(TbContext ctx, EntityId ownerId) {
        EntityGroupId entityGroupId = ctx.getPeContext().getEntityGroupService()
                .findEntityGroupByTypeAndName(ctx.getTenantId(), ownerId, config.getGroupType(), config.getGroupName())
                .map(IdBased::getId).orElse(null);
        if (entityGroupId != null) {
            return entityGroupId;
        } else {
            if (!EntityType.TENANT.equals(ownerId.getEntityType())) {
                return tryFindGroupByOwnerId(ctx, ctx.getPeContext().getOwner(ctx.getTenantId(), ownerId));
            } else {
                throw new RuntimeException("Can't find group with type: " + config.getGroupType() + " name: " + config.getGroupName() + "!");
            }
        }
    }

}
