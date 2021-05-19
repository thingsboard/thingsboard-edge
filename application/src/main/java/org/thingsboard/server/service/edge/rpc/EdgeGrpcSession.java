/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SchedulerEventsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SysAdminRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public final class  EdgeGrpcSession implements Closeable {

    private static final ReentrantLock downlinkMsgLock = new ReentrantLock();

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;
    private final ObjectMapper mapper;

    private EdgeContextComponent ctx;
    private Edge edge;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;
    private boolean syncCompleted;

    private ExecutorService syncExecutorService;

    private CountDownLatch latch;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ObjectMapper mapper, ExecutorService syncExecutorService) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.mapper = mapper;
        this.syncExecutorService = syncExecutorService;
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    } else {
                        connected = true;
                    }
                }
                if (connected && requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                    if (requestMsg.getSyncRequestMsg().getSyncRequired()) {
                        startSyncProcess(edge.getTenantId(), edge.getId());
                    }
                    syncCompleted = true;
                }
                if (connected) {
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE) && requestMsg.hasUplinkMsg()) {
                        onUplinkMsg(requestMsg.getUplinkMsg());
                    }
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE) && requestMsg.hasDownlinkResponseMsg()) {
                        onDownlinkResponse(requestMsg.getDownlinkResponseMsg());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
                closeSession();
            }

            @Override
            public void onCompleted() {
                closeSession();
            }

            private void closeSession() {
                connected = false;
                if (edge != null) {
                    try {
                        sessionCloseListener.accept(edge.getId());
                    } catch (Exception ignored) {
                    }
                }
                try {
                    outputStream.onCompleted();
                } catch (Exception ignored) {
                }
            }
        };
    }

    public void startSyncProcess(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}][{}] Staring edge sync process", tenantId, edgeId);
        syncExecutorService.submit(() -> {
            try {
                syncEdgeOwner(tenantId, edge);

                startProcessingEdgeEvents(new SysAdminRolesEdgeEventFetcher(ctx.getRoleService()));
                startProcessingEdgeEvents(new TenantRolesEdgeEventFetcher(ctx.getRoleService()));
                startProcessingEdgeEvents(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));

                syncWhiteLabelingAndCustomTranslation(tenantId, edge);

                // TODO: voba - implement this
                // syncAdminSettings(tenantId, edge);
                startProcessingEdgeEvents(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));

                syncEntityGroups(tenantId, edge);
                startProcessingEdgeEvents(new SchedulerEventsEdgeEventFetcher(ctx.getSchedulerEventService()));

                DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                        .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                        .build();
                sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg));
            } catch (Exception e) {
                log.error("[{}][{}] Exception during sync process", edge.getTenantId(), edge.getId(), e);
            }
        });
    }

    private void syncEdgeOwner(TenantId tenantId, Edge edge) throws InterruptedException {
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            EdgeEvent customerEdgeEvent = EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                    EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getOwnerId(), null);
            DownlinkMsg customerDownlinkMsg = convertToDownlinkMsg(customerEdgeEvent);
            sendDownlinkMsgsPack(Collections.singletonList(customerDownlinkMsg));

            startProcessingEdgeEvents(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), new CustomerId(edge.getOwnerId().getId())));
        }
    }

    private void syncWhiteLabelingAndCustomTranslation(TenantId tenantId, Edge edge) throws InterruptedException {
        List<EdgeEvent> edgeEvents = new ArrayList<>();
        EdgeEvent loginWhiteLabelingEdgeEvent = getLoginWhiteLabelingEdgeEvent(tenantId, edge);
        if (loginWhiteLabelingEdgeEvent != null) {
            edgeEvents.add(loginWhiteLabelingEdgeEvent);
        }
        EdgeEvent whiteLabelingEdgeEvent = getWhiteLabelingEdgeEvent(tenantId, edge);
        if (whiteLabelingEdgeEvent != null) {
            edgeEvents.add(whiteLabelingEdgeEvent);
        }
        EdgeEvent customTranslationEdgeEvent = getCustomTranslationEdgeEvent(tenantId, edge);
        if (customTranslationEdgeEvent != null) {
            edgeEvents.add(customTranslationEdgeEvent);
        }
        List<DownlinkMsg> downlinkMsgs = convertToDownlinkMsgsPack(edgeEvents);
        sendDownlinkMsgsPack(downlinkMsgs);
    }

    private EdgeEvent getLoginWhiteLabelingEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            String domainName = "localhost";
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                domainName = ctx.getWhiteLabelingService().getTenantLoginWhiteLabelingParams(tenantId).getDomainName();
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                domainName = ctx.getWhiteLabelingService().getCustomerLoginWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId())).getDomainName();
            }
            LoginWhiteLabelingParams loginWhiteLabelingParams = ctx.getWhiteLabelingService()
                    .getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, domainName == null ? "localhost" : domainName, null, null);
            if (loginWhiteLabelingParams != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.LOGIN_WHITE_LABELING, EdgeEventActionType.UPDATED, null, mapper.valueToTree(loginWhiteLabelingParams));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
            throw new RuntimeException(e);
        }
    }

    private EdgeEvent getWhiteLabelingEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            WhiteLabelingParams whiteLabelingParams = null;
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                whiteLabelingParams = ctx.getWhiteLabelingService().getMergedTenantWhiteLabelingParams(tenantId, null, null);
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                whiteLabelingParams = ctx.getWhiteLabelingService().getMergedCustomerWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId()), null, null);
            }
            if (whiteLabelingParams != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.WHITE_LABELING, EdgeEventActionType.UPDATED, null, mapper.valueToTree(whiteLabelingParams));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
            throw new RuntimeException(e);
        }
    }

    private EdgeEvent getCustomTranslationEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            CustomTranslation customTranslation = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                customTranslation = ctx.getCustomTranslationService().getMergedTenantCustomTranslation(new TenantId(ownerId.getId()));
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                customTranslation = ctx.getCustomTranslationService().getMergedCustomerCustomTranslation(edge.getTenantId(), new CustomerId(ownerId.getId()));
            }

            if (customTranslation != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.CUSTOM_TRANSLATION, EdgeEventActionType.UPDATED, null, mapper.valueToTree(customTranslation));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
            throw new RuntimeException(e);
        }
    }

    private void syncEntityGroups(TenantId tenantId, Edge edge) {
        try {
            List<EdgeEvent> edgeEvents = new ArrayList<>();
            edgeEvents.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.DEVICE));
            edgeEvents.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.ASSET));
            // TODO: entity view must be in sync with assets/devices
            edgeEvents.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.ENTITY_VIEW));
            edgeEvents.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.DASHBOARD));
            edgeEvents.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.USER));
            List<DownlinkMsg> downlinkMsgs = convertToDownlinkMsgsPack(edgeEvents);
            sendDownlinkMsgsPack(downlinkMsgs);
        } catch (Exception e) {
            log.error("Exception during loading edge entity groups(s) on sync!", e);
        }
    }

    private List<EdgeEvent> getEntityGroupsEdgeEvents(TenantId tenantId, EdgeId edgeId, EntityType entityGroupType) {
        try {
            List<EntityGroup> list = ctx.getEntityGroupService().findEdgeEntityGroupsByType(tenantId, edgeId, entityGroupType).get();
            List<EdgeEvent> result = new ArrayList<>();
            if (list != null && !list.isEmpty()) {
                for (EntityGroup entityGroup : list) {
                    if (!entityGroup.isEdgeGroupAll()) {
                        result.add(EdgeEventUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP,
                                EdgeEventActionType.ADDED, entityGroup.getId(), null, null));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Exception during loading edge entity groups(s) on sync!", e);
            throw new RuntimeException(e);
        }
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(true).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }

            @Override
            public void onFailure(Throwable t) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }
        }, MoreExecutors.directExecutor());
    }

    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                log.debug("[{}] Msg has been processed successfully! {}", edge.getRoutingKey(), msg);
            } else {
                log.error("[{}] Msg processing failed! Error msg: {}", edge.getRoutingKey(), msg.getErrorMsg());
            }
            latch.countDown();
        } catch (Exception e) {
            log.error("[{}] Can't process downlink response message [{}]", this.sessionId, msg, e);
        }
    }

    private void sendDownlinkMsg(ResponseMsg downlinkMsg) {
        log.trace("[{}] Sending downlink msg [{}]", this.sessionId, downlinkMsg);
        if (isConnected()) {
            try {
                downlinkMsgLock.lock();
                outputStream.onNext(downlinkMsg);
            } catch (Exception e) {
                log.error("[{}] Failed to send downlink message [{}]", this.sessionId, downlinkMsg, e);
                connected = false;
                sessionCloseListener.accept(edge.getId());
            } finally {
                downlinkMsgLock.unlock();
            }
            log.trace("[{}] Response msg successfully sent [{}]", this.sessionId, downlinkMsg);
        }
    }

    void onConfigurationUpdate(Edge edge) {
        log.debug("[{}] onConfigurationUpdate [{}]", this.sessionId, edge);
        this.edge = edge;
        EdgeUpdateMsg edgeConfig = EdgeUpdateMsg.newBuilder()
                .setConfiguration(constructEdgeConfigProto(edge)).build();
        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();
        sendDownlinkMsg(edgeConfigMsg);
    }

    void processEdgeEvents() throws ExecutionException, InterruptedException {
        log.trace("[{}] processHandleMessages started", this.sessionId);
        if (isConnected() && isSyncCompleted()) {
            Long queueStartTs = getQueueStartTs().get();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    queueStartTs,
                    ctx.getEdgeEventService());
            UUID ifOffset = startProcessingEdgeEvents(fetcher);
            if (ifOffset != null) {
                Long newStartTs = Uuids.unixTimestamp(ifOffset);
                updateQueueStartTs(newStartTs);
            }
        }
        log.trace("[{}] processHandleMessages finished", this.sessionId);
    }

    private UUID startProcessingEdgeEvents(EdgeEventFetcher fetcher) throws InterruptedException {
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        PageData<EdgeEvent> pageData;
        UUID ifOffset = null;
        boolean success;
        do {
            pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                success = sendDownlinkMsgsPack(downlinkMsgsPack);
                ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();
                if (success) {
                    pageLink = pageLink.nextPageLink();
                }
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events", this.sessionId);
            }
        } while (isConnected() && pageData.hasNext());
        return ifOffset;
    }

    private boolean sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) throws InterruptedException {
        boolean success;
        do {
            log.trace("[{}] [{}] downlink msg(s) are going to be send.", this.sessionId, downlinkMsgsPack.size());
            latch = new CountDownLatch(downlinkMsgsPack.size());
            for (DownlinkMsg downlinkMsg : downlinkMsgsPack) {
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setDownlinkMsg(downlinkMsg)
                        .build());
            }
            success = latch.await(10, TimeUnit.SECONDS);
            if (!success) {
                log.warn("[{}] Failed to deliver the batch: {}", this.sessionId, downlinkMsgsPack);
            }
            if (isConnected() && !success) {
                try {
                    Thread.sleep(ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches());
                } catch (InterruptedException e) {
                    log.error("[{}] Error during sleep between next send of single downlink msg", this.sessionId, e);
                }
            }
        } while (isConnected() && !success);
        return success;
    }

    private DownlinkMsg convertToDownlinkMsg(EdgeEvent edgeEvent) {
        log.trace("[{}] converting edge event to downlink msg [{}]", this.sessionId, edgeEvent);
        DownlinkMsg downlinkMsg = null;
        try {
            switch (edgeEvent.getAction()) {
                case UPDATED:
                case ADDED:
                case DELETED:
                case ADDED_TO_ENTITY_GROUP:
                case REMOVED_FROM_ENTITY_GROUP:
                case ASSIGNED_TO_EDGE:
                case UNASSIGNED_FROM_EDGE:
                case ALARM_ACK:
                case ALARM_CLEAR:
                case CREDENTIALS_UPDATED:
                case RELATION_ADD_OR_UPDATE:
                case RELATION_DELETED:
                    downlinkMsg = processEntityMessage(edgeEvent, edgeEvent.getAction());
                    break;
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                case ATTRIBUTES_DELETED:
                case TIMESERIES_UPDATED:
                    downlinkMsg = processTelemetryMessage(edgeEvent);
                    break;
                case CREDENTIALS_REQUEST:
                    downlinkMsg = processCredentialsRequestMessage(edgeEvent);
                    break;
                case ENTITY_MERGE_REQUEST:
                    downlinkMsg = processEntityMergeRequestMessage(edgeEvent);
                    break;
                case RPC_CALL:
                    downlinkMsg = processRpcCallMsg(edgeEvent);
                    break;
            }
        } catch (Exception e) {
            log.error("Exception during converting edge event to downlink msg", e);
        }
        return downlinkMsg;
    }

    private List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        return edgeEvents
                .stream()
                .map(this::convertToDownlinkMsg)
                .collect(Collectors.toList());
    }

    private DownlinkMsg processEntityMergeRequestMessage(EdgeEvent edgeEvent) {
        DownlinkMsg downlinkMsg = null;
        if (EdgeEventType.DEVICE.equals(edgeEvent.getType())) {
            DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
            Device device = ctx.getDeviceService().findDeviceById(edge.getTenantId(), deviceId);
            // TODO: voba - fix this
            // CustomerId customerId = getCustomerIdIfEdgeAssignedToCustomer(device);
            String conflictName = null;
            if(edgeEvent.getBody() != null) {
                conflictName = edgeEvent.getBody().get("conflictName").asText();
            }
            DeviceUpdateMsg d = ctx.getDeviceMsgConstructor()
                    .constructDeviceUpdatedMsg(UpdateMsgType.ENTITY_MERGE_RPC_MESSAGE, device, null, conflictName, null);
            downlinkMsg = DownlinkMsg.newBuilder()
                    .addAllDeviceUpdateMsg(Collections.singletonList(d))
                    .build();
        }
        return downlinkMsg;
    }

    private DownlinkMsg processRpcCallMsg(EdgeEvent edgeEvent) {
        log.trace("Executing processRpcCall, edgeEvent [{}]", edgeEvent);
        DeviceRpcCallMsg deviceRpcCallMsg =
                ctx.getDeviceMsgConstructor().constructDeviceRpcCallMsg(edgeEvent.getEntityId(), edgeEvent.getBody());
        return DownlinkMsg.newBuilder()
                .addAllDeviceRpcCallMsg(Collections.singletonList(deviceRpcCallMsg))
                .build();
    }

    private DownlinkMsg processCredentialsRequestMessage(EdgeEvent edgeEvent) {
        DownlinkMsg downlinkMsg = null;
        if (EdgeEventType.DEVICE.equals(edgeEvent.getType())) {
            DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
            DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                    .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                    .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                    .build();
            DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                    .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg));
            downlinkMsg = builder.build();
        }
        return downlinkMsg;
    }

    private ListenableFuture<Long> getQueueStartTs() {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                ctx.getAttributesService().find(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, QUEUE_START_TS_ATTR_KEY);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void updateQueueStartTs(Long newStartTs) {
        log.trace("[{}] updating QueueStartTs [{}][{}]", this.sessionId, edge.getId(), newStartTs);
        newStartTs = ++newStartTs; // increments ts by 1 - next edge event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private DownlinkMsg processTelemetryMessage(EdgeEvent edgeEvent) {
        log.trace("[{}] Executing processTelemetryMessage, edgeEvent [{}]", this.sessionId, edgeEvent);
        EntityId entityId = null;
        switch (edgeEvent.getType()) {
            case DEVICE:
                entityId = new DeviceId(edgeEvent.getEntityId());
                break;
            case ASSET:
                entityId = new AssetId(edgeEvent.getEntityId());
                break;
            case ENTITY_VIEW:
                entityId = new EntityViewId(edgeEvent.getEntityId());
                break;
            case DASHBOARD:
                entityId = new DashboardId(edgeEvent.getEntityId());
                break;
            case TENANT:
                entityId = new TenantId(edgeEvent.getEntityId());
                break;
            case CUSTOMER:
                entityId = new CustomerId(edgeEvent.getEntityId());
                break;
            case EDGE:
                entityId = new EdgeId(edgeEvent.getEntityId());
                break;
            case ENTITY_GROUP:
                entityId = new EntityGroupId(edgeEvent.getEntityId());
                break;
        }
        DownlinkMsg downlinkMsg = null;
        if (entityId != null) {
            log.debug("[{}] Sending telemetry data msg, entityId [{}], body [{}]", this.sessionId, edgeEvent.getEntityId(), edgeEvent.getBody());
            try {
                downlinkMsg = constructEntityDataProtoMsg(entityId, edgeEvent.getAction(), JsonUtils.parse(mapper.writeValueAsString(edgeEvent.getBody())));
            } catch (Exception e) {
                log.warn("[{}] Can't send telemetry data msg, entityId [{}], body [{}]", this.sessionId, edgeEvent.getEntityId(), edgeEvent.getBody(), e);
            }
        }
        return downlinkMsg;
    }

    private DownlinkMsg processEntityMessage(EdgeEvent edgeEvent, EdgeEventActionType action) {
        UpdateMsgType msgType = getResponseMsgType(edgeEvent.getAction());
        log.trace("Executing processEntityMessage, edgeEvent [{}], action [{}], msgType [{}]", edgeEvent, action, msgType);
        switch (edgeEvent.getType()) {
            case DEVICE:
                return processDevice(edgeEvent, msgType, action);
            case DEVICE_PROFILE:
                return processDeviceProfile(edgeEvent, msgType, action);
            case ASSET:
                return processAsset(edgeEvent, msgType, action);
            case ENTITY_VIEW:
                return processEntityView(edgeEvent, msgType, action);
            case DASHBOARD:
                return processDashboard(edgeEvent, msgType, action);
            case CUSTOMER:
                return processCustomer(edgeEvent, msgType, action);
            case RULE_CHAIN:
                return processRuleChain(edgeEvent, msgType, action);
            case RULE_CHAIN_METADATA:
                return processRuleChainMetadata(edgeEvent, msgType);
            case ALARM:
                return processAlarm(edgeEvent, msgType);
            case USER:
                return processUser(edgeEvent, msgType, action);
            case RELATION:
                return processRelation(edgeEvent, msgType);
            case WIDGETS_BUNDLE:
                return processWidgetsBundle(edgeEvent, msgType, action);
            case WIDGET_TYPE:
                return processWidgetType(edgeEvent, msgType, action);
            case ADMIN_SETTINGS:
                return processAdminSettings(edgeEvent);
            case SCHEDULER_EVENT:
                return processSchedulerEvent(edgeEvent, msgType);
            case ENTITY_GROUP:
                return processEntityGroup(edgeEvent, msgType);
            case WHITE_LABELING:
                return processWhiteLabeling(edgeEvent);
            case LOGIN_WHITE_LABELING:
                return processLoginWhiteLabeling(edgeEvent);
            case CUSTOM_TRANSLATION:
                return processCustomTranslation(edgeEvent);
            case ROLE:
                return processRole(edgeEvent, msgType);
            case GROUP_PERMISSION:
                return processGroupPermission(edgeEvent, msgType);
            default:
                log.warn("Unsupported edge event type [{}]", edgeEvent);
                return null;
        }
    }

    private DownlinkMsg processDevice(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType edgeEdgeEventActionType) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEdgeEventActionType) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Device device = ctx.getDeviceService().findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    DeviceUpdateMsg deviceUpdateMsg =
                            ctx.getDeviceMsgConstructor().constructDeviceUpdatedMsg(msgType, device, null, null, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg =
                        ctx.getDeviceMsgConstructor().constructDeviceDeleteMsg(deviceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            ctx.getDeviceMsgConstructor().constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDeviceCredentialsUpdateMsg(Collections.singletonList(deviceCredentialsUpdateMsg))
                            .build();
                }
                break;
        }
        log.trace("[{}] device processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processDeviceProfile(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case UPDATED:
                DeviceProfile deviceProfile = ctx.getDeviceProfileService().findDeviceProfileById(edgeEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                            ctx.getDeviceProfileMsgConstructor().constructDeviceProfileUpdatedMsg(msgType, deviceProfile);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDeviceProfileUpdateMsg(Collections.singletonList(deviceProfileUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                        ctx.getDeviceProfileMsgConstructor().constructDeviceProfileDeleteMsg(deviceProfileId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllDeviceProfileUpdateMsg(Collections.singletonList(deviceProfileUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] device profile processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processAsset(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Asset asset = ctx.getAssetService().findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    AssetUpdateMsg assetUpdateMsg =
                            ctx.getAssetMsgConstructor().constructAssetUpdatedMsg(msgType, asset, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllAssetUpdateMsg(Collections.singletonList(assetUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg =
                        ctx.getAssetMsgConstructor().constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllAssetUpdateMsg(Collections.singletonList(assetUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] asset processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processEntityView(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    EntityViewUpdateMsg entityViewUpdateMsg =
                            ctx.getEntityViewMsgConstructor().constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllEntityViewUpdateMsg(Collections.singletonList(entityViewUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                EntityViewUpdateMsg entityViewUpdateMsg =
                        ctx.getEntityViewMsgConstructor().constructEntityViewDeleteMsg(entityViewId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllEntityViewUpdateMsg(Collections.singletonList(entityViewUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] entity view processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processDashboard(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Dashboard dashboard = ctx.getDashboardService().findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    DashboardUpdateMsg dashboardUpdateMsg =
                            ctx.getDashboardMsgConstructor().constructDashboardUpdatedMsg(msgType, dashboard, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDashboardUpdateMsg(Collections.singletonList(dashboardUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                DashboardUpdateMsg dashboardUpdateMsg =
                        ctx.getDashboardMsgConstructor().constructDashboardDeleteMsg(dashboardId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllDashboardUpdateMsg(Collections.singletonList(dashboardUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] dashboard processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processCustomer(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        CustomerId customerId = new CustomerId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case UPDATED:
                Customer customer = ctx.getCustomerService().findCustomerById(edgeEvent.getTenantId(), customerId);
                if (customer != null) {
                    CustomerUpdateMsg customerUpdateMsg =
                            ctx.getCustomerMsgConstructor().constructCustomerUpdatedMsg(msgType, customer);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllCustomerUpdateMsg(Collections.singletonList(customerUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                CustomerUpdateMsg customerUpdateMsg =
                        ctx.getCustomerMsgConstructor().constructCustomerDeleteMsg(customerId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllCustomerUpdateMsg(Collections.singletonList(customerUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] customer processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processRuleChain(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType action) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (action) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ctx.getRuleChainMsgConstructor().constructRuleChainUpdatedMsg(edge.getRootRuleChainId(), msgType, ruleChain);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllRuleChainUpdateMsg(Collections.singletonList(ruleChainUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllRuleChainUpdateMsg(Collections.singletonList(ctx.getRuleChainMsgConstructor().constructRuleChainDeleteMsg(ruleChainId)))
                        .build();
                break;
        }
        log.trace("[{}] rule chain processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processRuleChainMetadata(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        DownlinkMsg downlinkMsg = null;
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ctx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ctx.getRuleChainMsgConstructor().constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllRuleChainMetadataUpdateMsg(Collections.singletonList(ruleChainMetadataUpdateMsg))
                        .build();
            }
        }
        log.trace("[{}] rule chain metadata processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processUser(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType edgeEdgeEventActionType) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEdgeEventActionType) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                User user = ctx.getUserService().findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllUserUpdateMsg(Collections.singletonList(ctx.getUserMsgConstructor().constructUserUpdatedMsg(msgType, user, entityGroupId)))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllUserUpdateMsg(Collections.singletonList(ctx.getUserMsgConstructor().constructUserDeleteMsg(userId)))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                UserCredentials userCredentialsByUserId = ctx.getUserService().findUserCredentialsByUserId(edge.getTenantId(), userId);
                if (userCredentialsByUserId != null && userCredentialsByUserId.isEnabled()) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            ctx.getUserMsgConstructor().constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllUserCredentialsUpdateMsg(Collections.singletonList(userCredentialsUpdateMsg))
                            .build();
                }
        }
        log.trace("[{}] user processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processRelation(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        EntityRelation entityRelation = mapper.convertValue(edgeEvent.getBody(), EntityRelation.class);
        RelationUpdateMsg r = ctx.getRelationMsgConstructor().constructRelationUpdatedMsg(msgType, entityRelation);
        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                .addAllRelationUpdateMsg(Collections.singletonList(r))
                .build();
        log.trace("[{}] relation processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processAlarm(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        DownlinkMsg downlinkMsg = null;
        try {
            AlarmId alarmId = new AlarmId(edgeEvent.getEntityId());
            Alarm alarm = ctx.getAlarmService().findAlarmByIdAsync(edgeEvent.getTenantId(), alarmId).get();
            if (alarm != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllAlarmUpdateMsg(Collections.singletonList(ctx.getAlarmMsgConstructor().constructAlarmUpdatedMsg(edge.getTenantId(), msgType, alarm)))
                        .build();
            }
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", edgeEvent, msgType, e);
        }
        log.trace("[{}] alarm processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processWidgetsBundle(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType edgeEdgeEventActionType) {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEdgeEventActionType) {
            case ADDED:
            case UPDATED:
                WidgetsBundle widgetsBundle = ctx.getWidgetsBundleService().findWidgetsBundleById(edgeEvent.getTenantId(), widgetsBundleId);
                if (widgetsBundle != null) {
                    WidgetsBundleUpdateMsg widgetsBundleUpdateMsg =
                            ctx.getWidgetsBundleMsgConstructor().constructWidgetsBundleUpdateMsg(msgType, widgetsBundle);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllWidgetsBundleUpdateMsg(Collections.singletonList(widgetsBundleUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                WidgetsBundleUpdateMsg widgetsBundleUpdateMsg =
                        ctx.getWidgetsBundleMsgConstructor().constructWidgetsBundleDeleteMsg(widgetsBundleId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllWidgetsBundleUpdateMsg(Collections.singletonList(widgetsBundleUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] widget bundle processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processWidgetType(EdgeEvent edgeEvent, UpdateMsgType msgType, EdgeEventActionType edgeEdgeEventActionType) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEdgeEventActionType) {
            case ADDED:
            case UPDATED:
                WidgetType widgetType = ctx.getWidgetTypeService().findWidgetTypeById(edgeEvent.getTenantId(), widgetTypeId);
                if (widgetType != null) {
                    WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                            ctx.getWidgetTypeMsgConstructor().constructWidgetTypeUpdateMsg(msgType, widgetType);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllWidgetTypeUpdateMsg(Collections.singletonList(widgetTypeUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                        ctx.getWidgetTypeMsgConstructor().constructWidgetTypeDeleteMsg(widgetTypeId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllWidgetTypeUpdateMsg(Collections.singletonList(widgetTypeUpdateMsg))
                        .build();
                break;
        }
        log.trace("[{}] widget type processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private DownlinkMsg processSchedulerEvent(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        SchedulerEventId schedulerEventId = new SchedulerEventId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                SchedulerEvent schedulerEvent = ctx.getSchedulerEventService().findSchedulerEventById(edgeEvent.getTenantId(), schedulerEventId);
                if (schedulerEvent != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllSchedulerEventUpdateMsg(Collections.singletonList(ctx.getSchedulerEventMsgConstructor().constructSchedulerEventUpdatedMsg(msgType, schedulerEvent)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllSchedulerEventUpdateMsg(Collections.singletonList(ctx.getSchedulerEventMsgConstructor().constructEventDeleteMsg(schedulerEventId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processEntityGroup(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        EntityGroupId entityGroupId = new EntityGroupId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                EntityGroup entityGroup = ctx.getEntityGroupService().findEntityGroupById(edgeEvent.getTenantId(), entityGroupId);
                if (entityGroup != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllEntityGroupUpdateMsg(Collections.singletonList(ctx.getEntityGroupMsgConstructor().constructEntityGroupUpdatedMsg(msgType, entityGroup)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllEntityGroupUpdateMsg(Collections.singletonList(ctx.getEntityGroupMsgConstructor().constructEntityGroupDeleteMsg(entityGroupId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processWhiteLabeling(EdgeEvent edgeEvent) {
        WhiteLabelingParams whiteLabelingParams = mapper.convertValue(edgeEvent.getBody(), WhiteLabelingParams.class);
        WhiteLabelingParamsProto whiteLabelingParamsProto =
                ctx.getWhiteLabelingParamsProtoConstructor().constructWhiteLabelingParamsProto(whiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllWhiteLabelingParams(Collections.singletonList(whiteLabelingParamsProto))
                .build();
    }

    private DownlinkMsg processLoginWhiteLabeling(EdgeEvent edgeEvent) {
        LoginWhiteLabelingParams loginWhiteLabelingParams = mapper.convertValue(edgeEvent.getBody(), LoginWhiteLabelingParams.class);
        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                ctx.getWhiteLabelingParamsProtoConstructor().constructLoginWhiteLabelingParamsProto(loginWhiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllLoginWhiteLabelingParams(Collections.singletonList(loginWhiteLabelingParamsProto))
                .build();
    }

    private DownlinkMsg processCustomTranslation(EdgeEvent edgeEvent) {
        CustomTranslation customTranslation = mapper.convertValue(edgeEvent.getBody(), CustomTranslation.class);
        CustomTranslationProto customTranslationProto =
                ctx.getCustomTranslationProtoConstructor().constructCustomTranslationProto(customTranslation);
        return DownlinkMsg.newBuilder()
                .addAllCustomTranslationMsg(Collections.singletonList(customTranslationProto))
                .build();
    }

    private DownlinkMsg processRole(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        RoleId roleId = new RoleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                Role role = ctx.getRoleService().findRoleById(edgeEvent.getTenantId(), roleId);
                if (role != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllRoleMsg(Collections.singletonList(ctx.getRoleProtoConstructor().constructRoleProto(msgType, role)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllRoleMsg(Collections.singletonList(ctx.getRoleProtoConstructor().constructRoleDeleteMsg(roleId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processGroupPermission(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        GroupPermissionId groupPermissionId = new GroupPermissionId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                GroupPermission groupPermission = ctx.getGroupPermissionService().findGroupPermissionById(edgeEvent.getTenantId(), groupPermissionId);
                if (groupPermission != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllGroupPermissionMsg(
                                    Collections.singletonList(
                                            ctx.getGroupPermissionProtoConstructor().constructGroupPermissionProto(msgType, groupPermission)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllGroupPermissionMsg(
                                Collections.singletonList(
                                        ctx.getGroupPermissionProtoConstructor().constructGroupPermissionDeleteMsg(groupPermissionId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processAdminSettings(EdgeEvent edgeEvent) {
        AdminSettings adminSettings = mapper.convertValue(edgeEvent.getBody(), AdminSettings.class);
        AdminSettingsUpdateMsg t = ctx.getAdminSettingsMsgConstructor().constructAdminSettingsUpdateMsg(adminSettings);
        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                .addAllAdminSettingsUpdateMsg(Collections.singletonList(t))
                .build();
        log.trace("[{}] admin settings processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private UpdateMsgType getResponseMsgType(EdgeEventActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case ASSIGNED_TO_EDGE:
            case RELATION_ADD_OR_UPDATE:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
            case RELATION_DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    private DownlinkMsg constructEntityDataProtoMsg(EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = ctx.getEntityDataMsgConstructor().constructEntityDataMsg(entityId, actionType, entityData);
        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataProto))
                .build();
        log.trace("[{}] entity data proto processed [{}]", this.sessionId, downlinkMsg);
        return downlinkMsg;
    }

    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().onTelemetryUpdate(edge.getTenantId(), edge.getCustomerId(), entityData));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().onDeviceUpdate(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().onDeviceCredentialsUpdate(edge.getTenantId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(ctx.getAlarmProcessor().onAlarmUpdate(edge.getTenantId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(ctx.getRelationProcessor().onRelationUpdate(edge.getTenantId(), relationUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgCount() > 0) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRuleChainMetadataRequestMsg(edge.getTenantId(), edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgCount() > 0) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processAttributesRequestMsg(edge.getTenantId(), edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgCount() > 0) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRelationRequestMsg(edge.getTenantId(), edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgCount() > 0) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processUserCredentialsRequestMsg(edge.getTenantId(), edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceCredentialsRequestMsg(edge.getTenantId(), edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcCallMsg : uplinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceRpcCallResponseMsg(edge.getTenantId(), deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getDeviceProfileDevicesRequestMsgCount() > 0) {
                for (DeviceProfileDevicesRequestMsg deviceProfileDevicesRequestMsg : uplinkMsg.getDeviceProfileDevicesRequestMsgList()) {
                    // do nothing. used in CE. in PE devices are synced by entity group entities request
                    result.add(Futures.immediateFuture(null));
                }
            }
            if (uplinkMsg.getWidgetBundleTypesRequestMsgCount() > 0) {
                for (WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg : uplinkMsg.getWidgetBundleTypesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processWidgetBundleTypesRequestMsg(edge.getTenantId(), edge, widgetBundleTypesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityViewsRequestMsgCount() > 0) {
                for (EntityViewsRequestMsg entityViewRequestMsg : uplinkMsg.getEntityViewsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityViewsRequestMsg(edge.getTenantId(), edge, entityViewRequestMsg));
                }
            }
            if (uplinkMsg.getEntityGroupEntitiesRequestMsgList() != null && !uplinkMsg.getEntityGroupEntitiesRequestMsgList().isEmpty()) {
                for (EntityGroupRequestMsg entityGroupEntitiesRequestMsg : uplinkMsg.getEntityGroupEntitiesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityGroupEntitiesRequest(edge.getTenantId(), edge, entityGroupEntitiesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityGroupPermissionsRequestMsgList() != null && !uplinkMsg.getEntityGroupPermissionsRequestMsgList().isEmpty()) {
                for (EntityGroupRequestMsg userGroupPermissionsRequestMsg : uplinkMsg.getEntityGroupPermissionsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityGroupPermissionsRequest(edge.getTenantId(), edge, userGroupPermissionsRequestMsg));
                }
            }
        } catch (Exception e) {
            log.error("[{}] Can't process uplink msg [{}]", this.sessionId, uplinkMsg, e);
        }
        return Futures.allAsList(result);
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        log.trace("[{}] processConnect [{}]", this.sessionId, request);
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            edge = optional.get();
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    sessionOpenListener.accept(edge.getId(), this);
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(constructEdgeConfigProto(edge)).build();
                }
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg("Failed to validate the edge!")
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            } catch (Exception e) {
                log.error("[{}] Failed to process edge connection!", request.getEdgeRoutingKey(), e);
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                        .setErrorMsg("Failed to process edge connection!")
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            }
        }
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                .setErrorMsg("Failed to find the edge! Routing key: " + request.getEdgeRoutingKey())
                .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
    }

    private EdgeConfiguration constructEdgeConfigProto(Edge edge) {
        EdgeConfiguration.Builder builder = EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setType(edge.getType())
                .setRoutingKey(edge.getRoutingKey())
                .setSecret(edge.getSecret())
                .setEdgeLicenseKey(edge.getEdgeLicenseKey())
                .setCloudEndpoint(edge.getCloudEndpoint())
                .setAdditionalInfo(JacksonUtil.toString(edge.getAdditionalInfo()))
                .setCloudType("PE");
        if (edge.getCustomerId() != null) {
            builder.setCustomerIdMSB(edge.getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(edge.getCustomerId().getId().getLeastSignificantBits());
        }
        return builder
                .build();
    }

    @Override
    public void close() {
        log.debug("[{}] Closing session", sessionId);
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
