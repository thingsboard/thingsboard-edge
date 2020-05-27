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
import io.grpc.stub.StreamObserver;
import jnr.ffi.annotations.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserUpdateMsgConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DefaultInitEdgeService implements InitEdgeService {

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private SchedulerEventService schedulerEventService;

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

    @Override
    public void init(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        initRuleChains(ctx, edge, outputStream);
        initDevices(ctx, edge, outputStream);
        initAssets(ctx, edge, outputStream);
        initEntityViews(ctx, edge, outputStream);
        initDashboards(ctx, edge, outputStream);
        initUsers(ctx, edge, outputStream);
        initSchedulerEvents(ctx, edge, outputStream);
    }

    private void initDevices(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
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
                                                        deviceById,
                                                        null);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setDeviceUpdateMsg(deviceUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge device(s) on init!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on init!", e);
        }
    }

    private void initAssets(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<EntityGroup>> assetGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.ASSET);
            Futures.transform(assetGroupsFuture, assetGroups -> {
                if (assetGroups != null && !assetGroups.isEmpty()) {
                    try {
                        Set<EntityId> entityIds = new HashSet<>();
                        for (EntityGroup assetGroup : assetGroups) {
                            entityIds.addAll(entityGroupService.findAllEntityIds(edge.getTenantId(), assetGroup.getId(), new TimePageLink(Integer.MAX_VALUE)).get());
                        }
                        if (!entityIds.isEmpty()) {
                            for (EntityId assetId : entityIds) {
                                ListenableFuture<Asset> assetByIdFuture = assetService.findAssetByIdAsync(edge.getTenantId(), new AssetId(assetId.getId()));
                                Futures.transform(assetByIdFuture, assetById -> {
                                    if (assetById != null) {
                                        AssetUpdateMsg assetUpdateMsg =
                                                assetUpdateMsgConstructor.constructAssetUpdatedMsg(
                                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                        assetById, null);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setAssetUpdateMsg(assetUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge asset(s) on init!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on init!", e);
        }
    }

    private void initEntityViews(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<EntityGroup>> entityViewGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.ENTITY_VIEW);
            Futures.transform(entityViewGroupsFuture, entityViewGroups -> {
                if (entityViewGroups != null && !entityViewGroups.isEmpty()) {
                    try {
                        Set<EntityId> entityIds = new HashSet<>();
                        for (EntityGroup entityViewGroup : entityViewGroups) {
                            entityIds.addAll(entityGroupService.findAllEntityIds(edge.getTenantId(), entityViewGroup.getId(), new TimePageLink(Integer.MAX_VALUE)).get());
                        }
                        if (!entityIds.isEmpty()) {
                            for (EntityId entityViewId : entityIds) {
                                ListenableFuture<EntityView> entityViewByIdFuture = entityViewService.findEntityViewByIdAsync(edge.getTenantId(), new EntityViewId(entityViewId.getId()));
                                Futures.transform(entityViewByIdFuture, entityViewById -> {
                                    if (entityViewById != null) {
                                        EntityViewUpdateMsg entityViewUpdateMsg =
                                                entityViewUpdateMsgConstructor.constructEntityViewUpdatedMsg(
                                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                        entityViewById, null);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setEntityViewUpdateMsg(entityViewUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge entity view(s) on init!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge entity view(s) on init!", e);
        }
    }

    private void initDashboards(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<EntityGroup>> dashboardGroupsFuture = entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.DASHBOARD);
            Futures.transform(dashboardGroupsFuture, dashboardGroups -> {
                if (dashboardGroups != null && !dashboardGroups.isEmpty()) {
                    try {
                        Set<EntityId> entityIds = new HashSet<>();
                        for (EntityGroup dashboardGroup : dashboardGroups) {
                            entityIds.addAll(entityGroupService.findAllEntityIds(edge.getTenantId(), dashboardGroup.getId(), new TimePageLink(Integer.MAX_VALUE)).get());
                        }
                        if (!entityIds.isEmpty()) {
                            for (EntityId dashboardId : entityIds) {
                                ListenableFuture<Dashboard> dashboardByIdFuture = dashboardService.findDashboardByIdAsync(edge.getTenantId(), new DashboardId(dashboardId.getId()));
                                Futures.transform(dashboardByIdFuture, dashboardById -> {
                                    if (dashboardById != null) {
                                        DashboardUpdateMsg dashboardUpdateMsg =
                                                dashboardUpdateMsgConstructor.constructDashboardUpdatedMsg(
                                                        UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                        dashboardById, null);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setDashboardUpdateMsg(dashboardUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge dashboard(s) on init!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on init!", e);
        }
    }

    private void initUsers(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
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
                                                        userById, null);
                                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                                .setUserUpdateMsg(userUpdateMsg)
                                                .build();
                                        outputStream.onNext(ResponseMsg.newBuilder()
                                                .setEntityUpdateMsg(entityUpdateMsg)
                                                .build());
                                    }
                                    return null;
                                }, ctx.getDbCallbackExecutor());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception during loading edge user(s) on init!", e);
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on init!", e);
        }
    }

    private void initRuleChains(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<TimePageData<RuleChain>> pageDataFuture =
                    ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.transform(pageDataFuture, pageData -> {
                if (pageData != null && !pageData.getData().isEmpty()) {
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
                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on init!");
        }
    }

    private void initSchedulerEvents(EdgeContextComponent ctx, Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<SchedulerEventInfo>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            Futures.transform(schedulerEventsFuture, schedulerEvents -> {
                if (schedulerEvents != null && !schedulerEvents.isEmpty()) {
                    log.trace("[{}] [{}] scheduler events(s) are going to be pushed to edge.", edge.getId(), schedulerEvents.size());
//                    for (SchedulerEventInfo schedulerEventInfo : schedulerEvents) {
//                        RuleChainUpdateMsg ruleChainUpdateMsg =
//                                ruleChainUpdateMsgConstructor.constructRuleChainUpdatedMsg(
//                                        edge.getRootRuleChainId(),
//                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
//                                        ruleChain);
//                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
//                                .setRuleChainUpdateMsg(ruleChainUpdateMsg)
//                                .build();
//                        outputStream.onNext(ResponseMsg.newBuilder()
//                                .setEntityUpdateMsg(entityUpdateMsg)
//                                .build());
//                    }
                }
                return null;
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Exception during loading edge scheduler event(s) on init!");
        }
    }

    @Override
    public void initRuleChainMetadata(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg, StreamObserver<ResponseMsg> outputStream) {
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
