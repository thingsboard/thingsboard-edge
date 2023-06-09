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
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group by group name",
        configClazz = TbDuplicateMsgToGroupByNameNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to resolved Entity Group",
        nodeDetails = "Entities are fetched from Entity Group detected according to the configuration. Entity Group is dynamically resolved based on it's name and type." +
                "By default, rule node attempts to find the group by name that belongs to the same customer which owns the device (or other message originator). " +
                "If no such group on the customer level, rule node will search for parent customer level and finally for the tenant level. " +
                "You may configure rule node to search only tenant level entity groups.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDuplicateToGroupByNameConfig",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupByNameNode extends TbAbstractDuplicateMsgToOriginatorsNode {

    private TbDuplicateMsgToGroupByNameNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupByNameNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, EntityId original) {
        try {
            return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), detectTargetEntityGroupId(ctx, original), new PageLink(Integer.MAX_VALUE));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private EntityGroupId detectTargetEntityGroupId(TbContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        EntityId ownerId;
        if (config.isSearchEntityGroupForTenantOnly()) {
            ownerId = ctx.getTenantId();
        } else {
            ownerId = ctx.getPeContext().getOwner(ctx.getTenantId(), originator);
        }
        return tryFindGroupByOwnerId(ctx, ownerId);
    }

    private EntityGroupId tryFindGroupByOwnerId(TbContext ctx, EntityId ownerId) throws ExecutionException, InterruptedException {
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

    private void validateConfig(TbDuplicateMsgToGroupByNameNodeConfiguration conf) {
        if (Resource.groupResourceFromGroupType(conf.getGroupType()) == null) {
            throw new IllegalArgumentException("Wrong configuration. Specified Entity Type is not a group entity.");
        }
    }

}
