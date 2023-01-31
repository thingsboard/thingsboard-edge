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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "add to group",
        configClazz = TbAddToGroupConfiguration.class,
        nodeDescription = "Adds Message Originator Entity to Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then adds Originator Entity to this group. " +
                "Will create new Entity Group if it doesn't exists and 'Create new group if not exists' is set to true.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAddToGroupConfig",
        icon = "add_circle",
        ruleChainTypes = RuleChainType.CORE
)
public class TbAddToGroupNode extends TbAbstractGroupActionNode<TbAddToGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return config.isCreateGroupIfNotExists();
    }

    @Override
    protected TbAddToGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAddToGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        if (BooleanUtils.toBooleanDefaultIfNull(config.isRemoveFromCurrentGroups(), false)) {
            removeFromCurrentGroups(ctx, msg, entityGroupId);
        }
        addEntityToGroup(ctx, msg, entityGroupId);
    }

    private void removeFromCurrentGroups(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        DonAsynchron.withCallback(Futures.allAsList(getListenableFutures(ctx, msg)), containerList -> {
            processRemove(ctx, msg, entityGroupId, containerList);
        }, throwable -> {
            throw new RuntimeException(throwable);
        });

    }

    private void processRemove(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId, List<EntityGroupContainer> containerList) {
        if (!containerList.isEmpty()) {
            process(ctx, msg, entityGroupId, containerList, getAllGroupId(containerList));
        }

    }

    private EntityGroupId getAllGroupId(List<EntityGroupContainer> containerList) {
        for (EntityGroupContainer groupContainer : containerList) {
            if (groupContainer != null) {
                if (groupContainer.isGroupAll() && !groupContainer.getEntityGroupIds().isEmpty()) {
                    return groupContainer.getEntityGroupIds().get(0);
                }
            } else {
                throw new RuntimeException("Entity Group 'All' doesn't exist");
            }
        }
        throw new RuntimeException("Entity Group 'All' doesn't exist");
    }

    private void process(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId, List<EntityGroupContainer> containerList, EntityGroupId groupAllId) {
        for (EntityGroupContainer group : containerList) {
            if (!group.isGroupAll() && !group.getEntityGroupIds().isEmpty()) {
                for (EntityGroupId groupId : group.getEntityGroupIds()) {
                    if (!groupId.equals(entityGroupId) && !groupId.equals(groupAllId)) {
                        ctx.getPeContext().getEntityGroupService()
                                .removeEntityFromEntityGroup(ctx.getTenantId(), groupId, msg.getOriginator());
                    }
                }
            }
        }
    }

    private List<ListenableFuture<EntityGroupContainer>> getListenableFutures(TbContext ctx, TbMsg msg) {
        List<ListenableFuture<EntityGroupContainer>> futures = new ArrayList<>();
        futures.add(Futures.transform(ctx.getPeContext().getEntityGroupService()
                .findEntityGroupsForEntity(ctx.getTenantId(), msg.getOriginator()), list -> new EntityGroupContainer(list, false), ctx.getDbCallbackExecutor()));
        futures.add(Futures.transform(getEntityGroupAllListenableFuture(ctx, msg), entityGroupAll -> entityGroupAll
                .map(entityGroup -> new EntityGroupContainer(Collections.singletonList(entityGroup.getId()), true))
                .orElse(null), ctx.getDbCallbackExecutor()));
        return futures;
    }

    private ListenableFuture<Optional<EntityGroup>> getEntityGroupAllListenableFuture(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                ListenableFuture<Device> deviceListenableFuture = ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
                return Futures.transformAsync(deviceListenableFuture, device -> {
                    if (device != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), device.getOwnerId(), EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            case ASSET:
                ListenableFuture<Asset> assetListenableFuture = ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
                return Futures.transformAsync(assetListenableFuture, asset -> {
                    if (asset != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), asset.getOwnerId(), EntityType.ASSET, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            case CUSTOMER:
                ListenableFuture<Customer> customerListenableFuture = ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(msg.getOriginator().getId()));
                return Futures.transformAsync(customerListenableFuture, customer -> {
                    if (customer != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), customer.getOwnerId(), EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            case ENTITY_VIEW:
                ListenableFuture<EntityView> entityViewListenableFuture = ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
                return Futures.transformAsync(entityViewListenableFuture, entityView -> {
                    if (entityView != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), entityView.getOwnerId(), EntityType.ENTITY_VIEW, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            case EDGE:
                ListenableFuture<Edge> edgeListenableFuture = ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId()));
                return Futures.transformAsync(edgeListenableFuture, edge -> {
                    if (edge != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), edge.getOwnerId(), EntityType.EDGE, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            case DASHBOARD:
                ListenableFuture<Dashboard> dashboardListenableFuture = ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()));
                return Futures.transformAsync(dashboardListenableFuture, dashboard -> {
                    if (dashboard != null) {
                        return ctx.getPeContext().getEntityGroupService().findEntityGroupByTypeAndNameAsync(ctx.getTenantId(), dashboard.getOwnerId(), EntityType.DASHBOARD, EntityGroup.GROUP_ALL_NAME);
                    } else {
                        return Futures.immediateFuture(Optional.empty());
                    }
                }, ctx.getDbCallbackExecutor());
            default:
                throw new RuntimeException("Entity with EntityType: '" + msg.getOriginator().getEntityType() + "'  doesn't support grouping");
        }
    }

    private void addEntityToGroup(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        ctx.getPeContext().getEntityGroupService().addEntityToEntityGroup(ctx.getTenantId(), entityGroupId, msg.getOriginator());
    }

    @Data
    @AllArgsConstructor
    private static class EntityGroupContainer {

        private List<EntityGroupId> entityGroupIds;
        private boolean groupAll;

    }

}
