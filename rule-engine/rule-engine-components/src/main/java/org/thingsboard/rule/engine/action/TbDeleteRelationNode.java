/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;


@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete relation",
        configClazz = TbDeleteRelationNodeConfiguration.class,
        nodeDescription = "Deletes relation with the incoming message originator based on the configured direction and type.",
        nodeDetails = "Useful when you need to remove relations between entities dynamically depending on incoming message payload, " +
                "message originator type, name, etc.<br><br>" +
                "If <strong>Delete relation with specific entity</strong> enabled, target entity to delete relation with should be specified. " +
                "Otherwise, rule node will delete all relations with the message originator based on the configured direction and type.<br><br>" +
                "Target entity configuration: " +
                "<ul><li><strong>Device</strong> - use a device with the specified name as the target entity to delete relation with.</li>" +
                "<li><strong>Asset</strong> - use an asset with the specified name as the target entity to delete relation with.</li>" +
                "<li><strong>Entity View</strong> - use entity view with the specified name as the target entity to delete relation with.</li>" +
                "<li><strong>Tenant</strong> - use current tenant as target entity to delete relation with.</li>" +
                "<li><strong>Customer</strong> - use customer with the specified title as the target entity to delete relation with.</li>" +
                "<li><strong>Dashboard</strong> - use a dashboard with the specified title as the target entity to delete relation with.</li>" +
                "<li><strong>User</strong> - use a user with the specified email as the target entity to delete relation with.</li>" +
                "<li><strong>Edge</strong> - use an edge with the specified name as the target entity to delete relation with.</li></ul>" +
                "Output connections: <code>Success</code> - If the relation(s) successfully deleted, otherwise <code>Failure</code>.",
        configDirective = "tbActionNodeDeleteRelationConfig",
        icon = "remove_circle",
        version = 1
)
public class TbDeleteRelationNode extends TbAbstractRelationActionNode<TbDeleteRelationNodeConfiguration> {

    @Override
    protected TbDeleteRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        var deleteRelationNodeConfiguration = TbNodeUtils.convert(configuration, TbDeleteRelationNodeConfiguration.class);
        if (!deleteRelationNodeConfiguration.isDeleteForSingleEntity()) {
            return deleteRelationNodeConfiguration;
        }
        checkIfConfigEntityTypeIsSupported(deleteRelationNodeConfiguration.getEntityType());
        return deleteRelationNodeConfiguration;
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return false;
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListenableFuture<Boolean> deleteResultFuture = config.isDeleteForSingleEntity() ?
                Futures.transformAsync(getTargetEntityId(ctx, msg), targetEntityId ->
                        deleteRelationToSpecificEntity(ctx, msg, targetEntityId), MoreExecutors.directExecutor()) :
                deleteRelationsByTypeAndDirection(ctx, msg, ctx.getDbCallbackExecutor());
        withCallback(deleteResultFuture, deleted -> {
                    if (deleted) {
                        ctx.tellSuccess(msg);
                        return;
                    }
                    ctx.tellFailure(msg, new RuntimeException("Failed to delete relation(s) with originator!"));
                },
                t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> deleteRelationToSpecificEntity(TbContext ctx, TbMsg msg, EntityId targetEntityId) {
        EntityId fromId;
        EntityId toId;
        if (EntitySearchDirection.FROM.equals(config.getDirection())) {
            fromId = msg.getOriginator();
            toId = targetEntityId;
        } else {
            toId = msg.getOriginator();
            fromId = targetEntityId;
        }
        var relationType = processPattern(msg, config.getRelationType());
        var tenantId = ctx.getTenantId();
        var relationService = ctx.getRelationService();
        return Futures.transformAsync(relationService.checkRelationAsync(tenantId, fromId, toId, relationType, RelationTypeGroup.COMMON),
                relationExists -> {
                    if (relationExists) {
                        return relationService.deleteRelationAsync(tenantId, fromId, toId, relationType, RelationTypeGroup.COMMON);
                    }
                    return Futures.immediateFuture(true);
                }, MoreExecutors.directExecutor());
    }

}
