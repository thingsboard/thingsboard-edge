/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and (entity type pattern for Asset, Device) and then create a relation to Originator Entity by type and direction." +
                " If Selected entity type: Asset, Device or Customer will create new Entity if it doesn't exist and 'Create new entity if not exists' is set to true.",
        nodeDetails = "If the relation already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle"
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    protected ListenableFuture<Boolean> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entity) {
        return createIfAbsent(ctx, msg, entity);
    }

    private ListenableFuture<Boolean> createIfAbsent(TbContext ctx, TbMsg msg, EntityContainer entityContainer) {
        processSearchDirection(msg, entityContainer);
        return Futures.transformAsync(ctx.getRelationService().checkRelation(ctx.getTenantId(), fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON),
                result -> {
                    if (!result) {
                        return processCreateRelation(ctx, entityContainer);
                    }
                    return Futures.immediateFuture(true);
                });
    }

    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityContainer entityContainer) {
        switch (entityContainer.getEntityType()) {
            case ASSET:
                return processAsset(ctx, entityContainer);
            case DEVICE:
                return processDevice(ctx, entityContainer);
            case CUSTOMER:
                return processCustomer(ctx, entityContainer);
            case DASHBOARD:
                return processDashboard(ctx, entityContainer);
            case ENTITY_VIEW:
                return processView(ctx, entityContainer);
            case TENANT:
                return processTenant(ctx, entityContainer);
        }
        return Futures.immediateFuture(true);
    }

    private ListenableFuture<Boolean> processView(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(entityContainer.getEntityId().getId())), entityView -> {
            if (entityView != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

    private ListenableFuture<Boolean> processDevice(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(entityContainer.getEntityId().getId())), device -> {
            if (device != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

    private ListenableFuture<Boolean> processAsset(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(entityContainer.getEntityId().getId())), asset -> {
            if (asset != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

    private ListenableFuture<Boolean> processCustomer(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(entityContainer.getEntityId().getId())), customer -> {
            if (customer != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

    private ListenableFuture<Boolean> processDashboard(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(entityContainer.getEntityId().getId())), dashboard -> {
            if (dashboard != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

    private ListenableFuture<Boolean> processTenant(TbContext ctx, EntityContainer entityContainer) {
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), new TenantId(entityContainer.getEntityId().getId())), tenant -> {
            if (tenant != null) {
                return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(fromId, toId, config.getRelationType(), RelationTypeGroup.COMMON));
            } else {
                return Futures.immediateFuture(true);
            }
        });
    }

}
