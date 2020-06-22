/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.init;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomTranslationProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.SchedulerEventUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WhiteLabelingParamsProtoConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private RuleChainUpdateMsgConstructor ruleChainUpdateMsgConstructor;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private AssetUpdateMsgConstructor assetUpdateMsgConstructor;

    @Autowired
    private EntityViewUpdateMsgConstructor entityViewUpdateMsgConstructor;

    @Autowired
    private DashboardUpdateMsgConstructor dashboardUpdateMsgConstructor;

    @Autowired
    private UserUpdateMsgConstructor userUpdateMsgConstructor;

    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Autowired
    private SchedulerEventUpdateMsgConstructor schedulerEventUpdateMsgConstructor;

    @Autowired
    private WhiteLabelingParamsProtoConstructor whiteLabelingParamsProtoConstructor;

    @Autowired
    private CustomTranslationProtoConstructor customTranslationProtoConstructor;

    @Override
    public void sync(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        syncLoginWhiteLabeling(edge, outputStream);
        syncWhiteLabeling(edge, outputStream);
        syncCustomTranslation(edge, outputStream);

        Set<EntityId> pushedEntityIds = new HashSet<>();
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(syncUsers(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncRuleChains(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncDevices(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncAssets(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncEntityViews(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncDashboards(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncSchedulerEvents(ctx, edge, pushedEntityIds, outputStream));
        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                syncRelations(ctx, edge, pushedEntityIds, outputStream);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Exception during sync entities", t);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> syncRuleChains(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<TimePageData<RuleChain>> future = ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            return Futures.transform(future, pageData -> {
                try {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (RuleChain ruleChain : pageData.getData()) {
                            RuleChainUpdateMsg ruleChainUpdateMsg =
                                    ruleChainUpdateMsgConstructor.constructRuleChainUpdatedMsg(
                                            edge.getRootRuleChainId(),
                                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                            ruleChain);
                            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                    .setRuleChainUpdateMsg(ruleChainUpdateMsg)
                                    .build();
                            outputStream.onNext(ResponseMsg.newBuilder()
                                    .setEntityUpdateMsg(entityUpdateMsg)
                                    .build());
                            pushedEntityIds.add(ruleChain.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during loading edge rule chain(s) on sync!", e);
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Set<EntityId>> findRelatedEdgeEntityIdsByEntityType(EdgeContextComponent ctx, Edge edge, EntityType entityType) {
        ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), entityType);

        ListenableFuture<List<List<EntityId>>> findEntityIdsFuture = Futures.transformAsync(entityGroupsFuture, entityGroups -> {
            List<ListenableFuture<List<EntityId>>> futures = new ArrayList<>();
            if (entityGroups != null && !entityGroups.isEmpty()) {
                for (EntityGroup entityGroup : entityGroups) {
                    futures.add(entityGroupService.findAllEntityIds(edge.getTenantId(), entityGroup.getId(), new TimePageLink(Integer.MAX_VALUE)));
                }
            }
            return Futures.successfulAsList(futures);
        }, ctx.getDbCallbackExecutor());

        return Futures.transform(findEntityIdsFuture, listOfEntityIds -> {
            Set<EntityId> result = new HashSet<>();
            if (listOfEntityIds != null && !listOfEntityIds.isEmpty()) {
                for (List<EntityId> entityIds : listOfEntityIds) {
                    if (entityIds != null && !entityIds.isEmpty()) {
                        result.addAll(entityIds);
                    }
                }
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> syncDevices(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<Set<EntityId>> entityIdsFuture = findRelatedEdgeEntityIdsByEntityType(ctx, edge, EntityType.DEVICE);

            ListenableFuture<List<Device>> devicesFuture = Futures.transformAsync(entityIdsFuture, entityIds -> {
                if (entityIds != null && !entityIds.isEmpty()) {
                    List<DeviceId> deviceIds = entityIds.stream().map(e -> new DeviceId(e.getId())).collect(Collectors.toList());
                    return deviceService.findDevicesByTenantIdAndIdsAsync(edge.getTenantId(), deviceIds);
                } else {
                    return Futures.immediateFuture(Collections.emptyList());
                }
            }, MoreExecutors.directExecutor());

            return Futures.transform(devicesFuture, devices -> {
                if (devices != null && !devices.isEmpty()) {
                    log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), devices.size());
                    for (Device device : devices) {
                        DeviceUpdateMsg deviceUpdateMsg =
                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        device);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDeviceUpdateMsg(deviceUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(device.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncAssets(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<Set<EntityId>> entityIdsFuture = findRelatedEdgeEntityIdsByEntityType(ctx, edge, EntityType.ASSET);

            ListenableFuture<List<Asset>> assetsFuture = Futures.transformAsync(entityIdsFuture, entityIds -> {
                if (entityIds != null && !entityIds.isEmpty()) {
                    List<AssetId> assetIds = entityIds.stream().map(e -> new AssetId(e.getId())).collect(Collectors.toList());
                    return assetService.findAssetsByTenantIdAndIdsAsync(edge.getTenantId(), assetIds);
                } else {
                    return Futures.immediateFuture(Collections.emptyList());
                }
            }, MoreExecutors.directExecutor());

            return Futures.transform(assetsFuture, assets -> {
                if (assets != null && !assets.isEmpty()) {
                    log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), assets.size());
                    for (Asset asset : assets) {
                        AssetUpdateMsg assetUpdateMsg =
                                assetUpdateMsgConstructor.constructAssetUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        asset);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setAssetUpdateMsg(assetUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(asset.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncEntityViews(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<Set<EntityId>> entityIdsFuture = findRelatedEdgeEntityIdsByEntityType(ctx, edge, EntityType.ENTITY_VIEW);

            ListenableFuture<List<EntityView>> entityViewsFuture = Futures.transformAsync(entityIdsFuture, entityIds -> {
                if (entityIds != null && !entityIds.isEmpty()) {
                    List<EntityViewId> entityViewIds = entityIds.stream().map(e -> new EntityViewId(e.getId())).collect(Collectors.toList());
                    return entityViewService.findEntityViewsByTenantIdAndIdsAsync(edge.getTenantId(), entityViewIds);
                } else {
                    return Futures.immediateFuture(Collections.emptyList());
                }
            }, MoreExecutors.directExecutor());

            return Futures.transform(entityViewsFuture, entityViews -> {
                if (entityViews != null && !entityViews.isEmpty()) {
                    log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), entityViews.size());
                    for (EntityView entityView : entityViews) {
                        EntityViewUpdateMsg entityViewUpdateMsg =
                                entityViewUpdateMsgConstructor.constructEntityViewUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        entityView);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setEntityViewUpdateMsg(entityViewUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(entityView.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge entity view(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncDashboards(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<Set<EntityId>> entityIdsFuture = findRelatedEdgeEntityIdsByEntityType(ctx, edge, EntityType.DASHBOARD);

            ListenableFuture<List<DashboardInfo>> dashboardInfosFuture = Futures.transformAsync(entityIdsFuture, entityIds -> {
                if (entityIds != null && !entityIds.isEmpty()) {
                    List<DashboardId> dashboardIds = entityIds.stream().map(e -> new DashboardId(e.getId())).collect(Collectors.toList());
                    return dashboardService.findDashboardInfoByIdsAsync(edge.getTenantId(), dashboardIds);
                } else {
                    return Futures.immediateFuture(Collections.emptyList());
                }
            }, MoreExecutors.directExecutor());

            ListenableFuture<List<Dashboard>> dashboardsFuture = Futures.transformAsync(dashboardInfosFuture, dashboardInfos -> {
                List<ListenableFuture<Dashboard>> futures = new ArrayList<>();
                if (dashboardInfos != null && !dashboardInfos.isEmpty()) {
                    for (DashboardInfo dashboardInfo : dashboardInfos) {
                        futures.add(dashboardService.findDashboardByIdAsync(edge.getTenantId(), dashboardInfo.getId()));
                    }
                }

                return Futures.successfulAsList(futures);
            }, ctx.getDbCallbackExecutor());

            return Futures.transform(dashboardsFuture, dashboards -> {
                if (dashboards != null && !dashboards.isEmpty()) {
                    log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), dashboards.size());
                    for (Dashboard dashboard : dashboards) {
                        DashboardUpdateMsg dashboardUpdateMsg =
                                dashboardUpdateMsgConstructor.constructDashboardUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        dashboard);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setDashboardUpdateMsg(dashboardUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(dashboard.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncUsers(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<Set<EntityId>> entityIdsFuture = findRelatedEdgeEntityIdsByEntityType(ctx, edge, EntityType.ASSET);

            ListenableFuture<List<User>> usersFuture = Futures.transformAsync(entityIdsFuture, entityIds -> {
                if (entityIds != null && !entityIds.isEmpty()) {
                    List<UserId> userIds = entityIds.stream().map(e -> new UserId(e.getId())).collect(Collectors.toList());
                    return userService.findUsersByTenantIdAndIdsAsync(edge.getTenantId(), userIds);
                } else {
                    return Futures.immediateFuture(Collections.emptyList());
                }
            }, MoreExecutors.directExecutor());

            return Futures.transform(usersFuture, users -> {
                if (users != null && !users.isEmpty()) {
                    log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), users.size());
                    for (User user : users) {
                        UserUpdateMsg userUpdateMsg =
                                userUpdateMsgConstructor.constructUserUpdatedMsg(
                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                        user);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setUserUpdateMsg(userUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(user.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncSchedulerEvents(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<SchedulerEvent>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            return Futures.transform(schedulerEventsFuture, schedulerEvents -> {
                if (schedulerEvents != null && !schedulerEvents.isEmpty()) {
                    log.trace("[{}] [{}] scheduler events(s) are going to be pushed to edge.", edge.getId(), schedulerEvents.size());
                    for (SchedulerEvent schedulerEvent : schedulerEvents) {
                        SchedulerEventUpdateMsg schedulerEventUpdateMsg =
                                schedulerEventUpdateMsgConstructor.constructSchedulerEventUpdatedMsg(
                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                        schedulerEvent);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setSchedulerEventUpdateMsg(schedulerEventUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                        pushedEntityIds.add(schedulerEvent.getId());
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge scheduler event(s) on sync!");
            return Futures.immediateFuture(null);
        }
    }

    private void syncLoginWhiteLabeling(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            String domainName = "localhost";
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getTenantLoginWhiteLabelingParams(new TenantId(ownerId.getId())).getDomainName();
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getCustomerLoginWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId())).getDomainName();
            }

            LoginWhiteLabelingParams loginWhiteLabelingParams = whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, domainName == null ? "localhost" : domainName, null, null);
            if (loginWhiteLabelingParams != null) {
                LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                        whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(loginWhiteLabelingParams);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setLoginWhiteLabelingParams(loginWhiteLabelingParamsProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
        }
    }

    private void syncWhiteLabeling(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            WhiteLabelingParams whiteLabelingParams = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(new TenantId(ownerId.getId()), null, null);
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId()), null, null);
            }

            if (whiteLabelingParams != null) {
                WhiteLabelingParamsProto whiteLabelingParamsProto =
                        whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(whiteLabelingParams);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setWhiteLabelingParams(whiteLabelingParamsProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
        }
    }

    private void syncCustomTranslation(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            CustomTranslation customTranslation = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(new TenantId(ownerId.getId()));
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedCustomerCustomTranslation(edge.getTenantId(), new CustomerId(ownerId.getId()));
            }


            if (customTranslation != null) {
                CustomTranslationProto customTranslationProto =
                        customTranslationProtoConstructor.constructCustomTranslationProto(customTranslation);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setCustomTranslation(customTranslationProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
        }
    }

    private ListenableFuture<Void> syncRelations(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        if (!pushedEntityIds.isEmpty()) {
            List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
            for (EntityId entityId : pushedEntityIds) {
                futures.add(syncRelations(edge, entityId, EntitySearchDirection.FROM));
                futures.add(syncRelations(edge, entityId, EntitySearchDirection.TO));
            }
            ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
            return Futures.transform(relationsListFuture, relationsList -> {
                try {
                    Set<EntityRelation> uniqueEntityRelations = new HashSet<>();
                    if (!relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            if (!entityRelations.isEmpty()) {
                                uniqueEntityRelations.addAll(entityRelations);
                            }
                        }
                    }
                    if (!uniqueEntityRelations.isEmpty()) {
                        log.trace("[{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), uniqueEntityRelations.size());
                        for (EntityRelation relation : uniqueEntityRelations) {
                            try {
                                RelationUpdateMsg relationUpdateMsg =
                                        relationUpdateMsgConstructor.constructRelationUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                relation);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setRelationUpdateMsg(relationUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            } catch (Exception e) {
                                log.error("Exception during loading relation [{}] to edge on sync!", relation, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<List<EntityRelation>> syncRelations(Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(edge.getTenantId(), query);
    }

    @Override
    public void syncRuleChainMetadata(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edge.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainUpdateMsgConstructor.constructRuleChainMetadataUpdatedMsg(
                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                            ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        }
    }
}
