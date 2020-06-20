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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        syncUsers(ctx, edge, pushedEntityIds, outputStream);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(syncRuleChains(ctx, edge, pushedEntityIds, outputStream));
//        futures.add(syncDevices(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncAssets(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncEntityViews(ctx, edge, pushedEntityIds, outputStream));
        futures.add(syncDashboards(ctx, edge, pushedEntityIds, outputStream));
        syncSchedulerEvents(ctx, edge, pushedEntityIds, outputStream);
        ListenableFuture<List<Void>> joinFuture = Futures.allAsList(futures);
        Futures.transform(joinFuture, result -> {
            syncRelations(ctx, edge, pushedEntityIds, outputStream);
            return null;
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

//    private ListenableFuture<Void> syncDevices(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
//        try {
//            ListenableFuture<TimePageData<Device>> future = deviceService.findDevicesByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
//            return Futures.transform(future, pageData -> {
//                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
//                    log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
//                    for (Device device : pageData.getData()) {
//                        DeviceUpdateMsg deviceUpdateMsg =
//                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(
//                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
//                                        device);
//                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
//                                .setDeviceUpdateMsg(deviceUpdateMsg)
//                                .build();
//                        outputStream.onNext(ResponseMsg.newBuilder()
//                                .setEntityUpdateMsg(entityUpdateMsg)
//                                .build());
//                        pushedEntityIds.add(device.getId());
//                    }
//                }
//                return null;
//            }, ctx.getDbCallbackExecutor());
//        } catch (Exception e) {
//            log.error("Exception during loading edge device(s) on sync!", e);
//            return Futures.immediateFuture(null);
//        }
//    }

    private void syncDevices(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<EntityGroup>> deviceGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.DEVICE);
            Futures.transform(deviceGroupsFuture, deviceGroups -> {
                if (deviceGroups != null && !deviceGroups.isEmpty()) {
                    try {
                        Set<EntityId> entityIds = new HashSet<>();
                        for (EntityGroup deviceGroup : deviceGroups) {
                            entityIds.addAll(entityGroupService.findAllEntityIds(edge.getTenantId(), deviceGroup.getId(), new TimePageLink(Integer.MAX_VALUE)).get());
                        }
                        if (!entityIds.isEmpty()) {
                            for (EntityId deviceId : entityIds) {
                                ListenableFuture<Device> deviceByIdFuture = deviceService.findDeviceByIdAsync(edge.getTenantId(), new DeviceId(deviceId.getId()));
                                Futures.transform(deviceByIdFuture, deviceById -> {
                                    if (deviceById != null) {
                                        DeviceUpdateMsg deviceUpdateMsg =
                                                deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(
                                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                        deviceById);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setDeviceUpdateMsg(deviceUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                        pushedEntityIds.add(deviceById.getId());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge device(s) on sync!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
        }
    }

    private ListenableFuture<Void> syncAssets(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
//        try {
//            ListenableFuture<TimePageData<Asset>> future = assetService.findAssetsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
//            return Futures.transform(future, pageData -> {
//                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
//                    log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
//                    for (Asset asset : pageData.getData()) {
//                        AssetUpdateMsg assetUpdateMsg =
//                                assetUpdateMsgConstructor.constructAssetUpdatedMsg(
//                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
//                                        asset);
//                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
//                                .setAssetUpdateMsg(assetUpdateMsg)
//                                .build();
//                        outputStream.onNext(ResponseMsg.newBuilder()
//                                .setEntityUpdateMsg(entityUpdateMsg)
//                                .build());
//                        pushedEntityIds.add(asset.getId());
//                    }
//                }
//                return null;
//            }, ctx.getDbCallbackExecutor());
//        } catch (Exception e) {
//            log.error("Exception during loading edge asset(s) on sync!", e);
//            return Futures.immediateFuture(null);
//        }
        return null;
    }

    private ListenableFuture<Void> syncEntityViews(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
//        try {
//            ListenableFuture<TimePageData<EntityView>> future = entityViewService.findEntityViewsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
//            return Futures.transform(future, pageData -> {
//                try {
//                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
//                        log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
//                        for (EntityView entityView : pageData.getData()) {
//                            EntityViewUpdateMsg entityViewUpdateMsg =
//                                    entityViewUpdateMsgConstructor.constructEntityViewUpdatedMsg(
//                                            UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
//                                            entityView);
//                            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
//                                    .setEntityViewUpdateMsg(entityViewUpdateMsg)
//                                    .build();
//                            outputStream.onNext(ResponseMsg.newBuilder()
//                                    .setEntityUpdateMsg(entityUpdateMsg)
//                                    .build());
//                            pushedEntityIds.add(entityView.getId());
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("Exception during loading edge entity view(s) on sync!", e);
//                }
//                return null;
//            }, ctx.getDbCallbackExecutor());
//        } catch (Exception e) {
//            log.error("Exception during loading edge entity view(s) on sync!", e);
//            return Futures.immediateFuture(null);
//        }
        return null;
    }

    private ListenableFuture<Void> syncDashboards(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
//        try {
//            ListenableFuture<TimePageData<DashboardInfo>> future = dashboardService.findDashboardsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
//            return Futures.transform(future, pageData -> {
//                try {
//                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
//                        log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
//                        for (DashboardInfo dashboardInfo : pageData.getData()) {
//                            Dashboard dashboard = dashboardService.findDashboardById(edge.getTenantId(), dashboardInfo.getId());
//                            DashboardUpdateMsg dashboardUpdateMsg =
//                                    dashboardUpdateMsgConstructor.constructDashboardUpdatedMsg(
//                                            UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
//                                            dashboard);
//                            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
//                                    .setDashboardUpdateMsg(dashboardUpdateMsg)
//                                    .build();
//                            outputStream.onNext(ResponseMsg.newBuilder()
//                                    .setEntityUpdateMsg(entityUpdateMsg)
//                                    .build());
//                            pushedEntityIds.add(dashboard.getId());
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("Exception during loading edge dashboard(s) on sync!", e);
//                }
//                return null;
//            }, ctx.getDbCallbackExecutor());
//        } catch (Exception e) {
//            log.error("Exception during loading edge dashboard(s) on sync!", e);
//            return Futures.immediateFuture(null);
//        }
        return null;
    }

//    private void syncUsers(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
//        try {
//            TextPageData<User> pageData = userService.findTenantAdmins(edge.getTenantId(), new TextPageLink(Integer.MAX_VALUE));
//            pushUsersToEdge(pageData, edge, pushedEntityIds, outputStream);
//            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
//                pageData = userService.findCustomerUsers(edge.getTenantId(), edge.getCustomerId(), new TextPageLink(Integer.MAX_VALUE));
//                pushUsersToEdge(pageData, edge, pushedEntityIds, outputStream);
//            }
//        } catch (Exception e) {
//            log.error("Exception during loading edge user(s) on sync!", e);
//        }
//    }



    private void pushUsersToEdge(TextPageData<User> pageData, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
            for (User user : pageData.getData()) {
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
    }

    private void syncUsers(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<EntityGroup>> userGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.USER);
            Futures.transform(userGroupsFuture, userGroups -> {
                if (userGroups != null && !userGroups.isEmpty()) {
                    try {
                        Set<EntityId> entityIds = new HashSet<>();
                        for (EntityGroup userGroup : userGroups) {
                            entityIds.addAll(entityGroupService.findAllEntityIds(edge.getTenantId(), userGroup.getId(), new TimePageLink(Integer.MAX_VALUE)).get());
                        }
                        if (!entityIds.isEmpty()) {
                            for (EntityId userId : entityIds) {
                                ListenableFuture<User> userByIdFuture = userService.findUserByIdAsync(edge.getTenantId(), new UserId(userId.getId()));
                                Futures.transform(userByIdFuture, userById -> {
                                    if (userById != null) {
                                        UserUpdateMsg userUpdateMsg =
                                                userUpdateMsgConstructor.constructUserUpdatedMsg(
                                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                        userById);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setUserUpdateMsg(userUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                        pushedEntityIds.add(userById.getId());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge user(s) on sync!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
        }
    }

    private void syncSchedulerEvents(EdgeContextComponent ctx, Edge edge, Set<EntityId> pushedEntityIds, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<SchedulerEvent>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            Futures.transform(schedulerEventsFuture, schedulerEvents -> {
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
