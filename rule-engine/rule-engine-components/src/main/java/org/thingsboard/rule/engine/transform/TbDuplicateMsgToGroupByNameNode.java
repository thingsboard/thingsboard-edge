/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group by name",
        version = 1,
        configClazz = TbDuplicateMsgToGroupByNameNodeConfiguration.class,
        nodeDescription = "Duplicates message to all entities belonging to resolved Entity group",
        nodeDetails = "Entities are fetched from entity group that is detected according to the configuration. " +
                "When <b>\"search entity group on Tenant level only\"</b> is enabled, the search is restricted to the Tenant level only. " +
                "If <b>\"consider originator as a group owner\"</b> is enabled and the originator is a Tenant or Customer, the search starts from the originator's level and goes up the hierarchy to the tenant level if the group isn't found. " +
                "Otherwise, the search starts at the same level as the message originator's owner. " +
                "Entity group is dynamically resolved based on it's name and type. " +
                "For each entity from group new message is created with entity as originator " +
                "and message parameters copied from original message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeDuplicateToGroupByNameConfig",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupByNameNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupByNameNodeConfiguration> {

    private static final String CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER = "considerMessageOriginatorAsAGroupOwner";

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
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        String groupName = TbNodeUtils.processPattern(config.getGroupName(), msg);
        var entityGroupId = detectTargetEntityGroupId(ctx, msg.getOriginator(), groupName);
        return ctx.getPeContext().getEntityGroupService().findAllEntityIdsAsync(ctx.getTenantId(), entityGroupId, new PageLink(Integer.MAX_VALUE));
    }

    private EntityGroupId detectTargetEntityGroupId(TbContext ctx, EntityId originator, String groupName) {
        if (config.isSearchEntityGroupForTenantOnly()) {
            return tryFindGroupByOwnerId(ctx, ctx.getTenantId(), groupName);
        }
        if (config.isConsiderMessageOriginatorAsAGroupOwner() &&
                (originator.getEntityType() == EntityType.TENANT ||
                        originator.getEntityType() == EntityType.CUSTOMER)) {
            return tryFindGroupByOwnerId(ctx, originator, groupName);
        }
        return tryFindGroupByOwnerId(ctx, ctx.getPeContext().getOwner(ctx.getTenantId(), originator), groupName);
    }

    private EntityGroupId tryFindGroupByOwnerId(TbContext ctx, EntityId ownerId, String groupName) {
        EntityGroupId entityGroupId = ctx.getPeContext().getEntityGroupService()
                .findEntityGroupByTypeAndName(ctx.getTenantId(), ownerId, config.getGroupType(), groupName)
                .map(IdBased::getId).orElse(null);
        if (entityGroupId != null) {
            return entityGroupId;
        } else {
            if (!EntityType.TENANT.equals(ownerId.getEntityType())) {
                return tryFindGroupByOwnerId(ctx, ctx.getPeContext().getOwner(ctx.getTenantId(), ownerId), groupName);
            } else {
                throw new RuntimeException("Can't find group with type: " + config.getGroupType() + " name: " + groupName + "!");
            }
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        if (fromVersion == 0) {
            if (!oldConfiguration.has(CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER)) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration).put(CONSIDER_MESSAGE_ORIGINATOR_AS_A_GROUP_OWNER, false);
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
