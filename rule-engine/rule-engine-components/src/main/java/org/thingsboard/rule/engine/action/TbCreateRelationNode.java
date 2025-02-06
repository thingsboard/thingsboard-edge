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
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target entity specified in the configuration and creates a relation with the " +
                "incoming message originator based on the configured direction and type.",
        nodeDetails = "Useful when you need to create relations between entities dynamically depending on " +
                "incoming message payload, message originator type, name, etc.<br><br>" +
                "Target entity configuration: " +
                "<ul><li><strong>Device</strong> - use a device with the specified name as the target entity to create a relation with. " +
                "When selected, rule node allows us to use advanced mode to enable device creation if it doesn't exist. " +
                "In advanced mode, device profile name should be specified.</li>" +
                "<li><strong>Asset</strong> - use an asset with the specified name as the target entity to create a relation with. " +
                "When selected, rule node allows us to use advanced mode to enable device creation if it doesn't exist. " +
                "In advanced mode, asset profile name should be specified.</li>" +
                "<li><strong>Entity View</strong> - use entity view with the specified name as the target entity to create a relation with.</li>" +
                "<li><strong>Tenant</strong> - use current tenant as target entity to create a relation with.</li>" +
                "<li><strong>Customer</strong> - use customer with the specified title as the target entity to create a relation with. " +
                "When selected, rule node allows us to use advanced mode to enable customer creation if it doesn't exist.</li>" +
                "<li><strong>Dashboard</strong> - use a dashboard with the specified title as the target entity to create a relation with.</li>" +
                "<li><strong>User</strong> - use a user with the specified email as the target entity to create a relation with.</li>" +
                "<li><strong>Edge</strong> - use an edge with the specified name as the target entity to create a relation with.</li>" +
                "<li><strong>Data Converter</strong> - use data converter with the specified name as the target entity to create a relation with.</li>" +
                "<li><strong>Role</strong> - use role with the specified name as the target entity to create a relation with.</li></ul>" +
                "Advanced settings: " +
                "<ul><li><strong>Remove current relations</strong> - removes current relations with originator of the incoming message based on direction and type. " +
                "Useful in GPS tracking use cases where relation acts as a temporary indicator of a tracker presence in specific geofence.</li>" +
                "<li><strong>Change originator to target entity</strong> - useful when you need to process submitted message as a message from target entity.</li></ul>" +
                "Output connections: <code>Success</code> - if the relation already exists or successfully created, otherwise <code>Failure</code>.",
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle",
        version = 1
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        var createRelationNodeConfiguration = TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
        checkIfConfigEntityTypeIsSupported(createRelationNodeConfiguration.getEntityType());
        return createRelationNodeConfiguration;
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var targetEntityIdFuture = getTargetEntityId(ctx, msg);
        var createRelationResultFuture = Futures.transformAsync(targetEntityIdFuture, targetEntityId -> {
            var originator = msg.getOriginator();
            var relationType = processPattern(msg, config.getRelationType());
            if (config.isRemoveCurrentRelations()) {
                var removalOfCurrentRelationsFuture = deleteRelationsByTypeAndDirection(ctx, msg, relationType, MoreExecutors.directExecutor());
                return Futures.transformAsync(removalOfCurrentRelationsFuture, __ ->
                        checkRelationAndCreateIfAbsent(ctx, originator, targetEntityId, relationType), MoreExecutors.directExecutor());
            }
            return checkRelationAndCreateIfAbsent(ctx, originator, targetEntityId, relationType);
        }, MoreExecutors.directExecutor());
        if (!config.isChangeOriginatorToRelatedEntity()) {
            withCallback(createRelationResultFuture,
                    relationCreated -> {
                        if (relationCreated) {
                            ctx.tellSuccess(msg);
                            return;
                        }
                        ctx.tellFailure(msg, new RuntimeException("Failed to create originator relation with target entity!"));
                    },
                    t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
            return;
        }
        withCallback(Futures.allAsList(targetEntityIdFuture, createRelationResultFuture), result -> {
            var targetEntityId = (EntityId) result.get(0);
            var relationCreated = (Boolean) result.get(1);
            if (relationCreated) {
                var transformedMsg = ctx.transformMsgOriginator(msg, targetEntityId);
                ctx.tellSuccess(transformedMsg);
                return;
            }
            ctx.tellFailure(msg, new RuntimeException("Failed to create originator relation with target entity!"));
        }, t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> checkRelationAndCreateIfAbsent(TbContext ctx, EntityId originator, EntityId targetEntityId, String relationType) {
        EntityId fromId;
        EntityId toId;
        if (EntitySearchDirection.FROM.equals(config.getDirection())) {
            fromId = originator;
            toId = targetEntityId;
        } else {
            toId = originator;
            fromId = targetEntityId;
        }
        var checkRelationFuture = ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), fromId, toId, relationType, RelationTypeGroup.COMMON);
        return Futures.transformAsync(checkRelationFuture, relationExists ->
                        relationExists ?
                                Futures.immediateFuture(true) :
                                ctx.getRelationService()
                                        .saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, relationType, RelationTypeGroup.COMMON)),
                MoreExecutors.directExecutor());
    }

}
