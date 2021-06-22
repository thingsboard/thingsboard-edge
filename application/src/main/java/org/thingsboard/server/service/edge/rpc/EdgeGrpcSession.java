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
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EntityGroupEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SchedulerEventsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SysAdminRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.WhiteLabelingEdgeEventFetcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Data
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock downlinkMsgLock = new ReentrantLock();
    private static final ReentrantLock downlinkMsgsPackLock = new ReentrantLock();

    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";

    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    private final Consumer<EdgeId> sessionCloseListener;
    private final ObjectMapper mapper;

    private final Map<Integer, DownlinkMsg> pendingMsgsMap = new LinkedHashMap<>();

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
        syncCompleted = false;
        syncExecutorService.submit(() -> {
            try {
                syncEdgeOwner(tenantId, edge);

                startProcessingEdgeEvents(new SysAdminRolesEdgeEventFetcher(ctx.getRoleService()));
                startProcessingEdgeEvents(new TenantRolesEdgeEventFetcher(ctx.getRoleService()));
                startProcessingEdgeEvents(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
                startProcessingEdgeEvents(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
                startProcessingEdgeEvents(new WhiteLabelingEdgeEventFetcher(ctx.getWhiteLabelingService(), ctx.getCustomTranslationService()));
                startProcessingEdgeEvents(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), ctx.getAttributesService()));
                startProcessingEdgeEvents(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
                startProcessingEdgeEvents(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DEVICE));
                startProcessingEdgeEvents(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ASSET));
                startProcessingEdgeEvents(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ENTITY_VIEW));
                startProcessingEdgeEvents(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DASHBOARD));
                startProcessingEdgeEvents(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.USER));
                startProcessingEdgeEvents(new SchedulerEventsEdgeEventFetcher(ctx.getSchedulerEventService()));

                DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                        .build();
                sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg));
            } catch (Exception e) {
                log.error("[{}][{}] Exception during sync process", edge.getTenantId(), edge.getId(), e);
            }
        });
    }

    private void syncEdgeOwner(TenantId tenantId, Edge edge) throws Exception {
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            EdgeEvent customerEdgeEvent = EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                    EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getOwnerId(), null);
            DownlinkMsg customerDownlinkMsg = convertToDownlinkMsg(customerEdgeEvent);
            sendDownlinkMsgsPack(Collections.singletonList(customerDownlinkMsg));

            startProcessingEdgeEvents(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), new CustomerId(edge.getOwnerId().getId())));
        }
    }


    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder()
                        .setUplinkMsgId(uplinkMsg.getUplinkMsgId())
                        .setSuccess(true).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }

            @Override
            public void onFailure(Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "";
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder()
                        .setUplinkMsgId(uplinkMsg.getUplinkMsgId())
                        .setSuccess(false).setErrorMsg(errorMsg).build();
                sendDownlinkMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }
        }, MoreExecutors.directExecutor());
    }

    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                pendingMsgsMap.remove(msg.getDownlinkMsgId());
                log.debug("[{}] Msg has been processed successfully! {}", edge.getRoutingKey(), msg);
            } else {
                log.error("[{}] Msg processing failed! Error msg: {}", edge.getRoutingKey(), msg.getErrorMsg());
            }
            latch.countDown();
        } catch (Exception e) {
            log.error("[{}] Can't process downlink response message [{}] {}", this.sessionId, msg, e);
        }
    }

    private void sendDownlinkMsg(ResponseMsg downlinkMsg) {
        log.trace("[{}] Sending downlink msg [{}]", this.sessionId, downlinkMsg);
        if (isConnected()) {
            try {
                downlinkMsgLock.lock();
                outputStream.onNext(downlinkMsg);
            } catch (Exception e) {
                log.error("[{}] Failed to send downlink message [{}] {}", this.sessionId, downlinkMsg, e);
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

    void processEdgeEvents() throws Exception {
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

    private UUID startProcessingEdgeEvents(EdgeEventFetcher fetcher) throws Exception {
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        PageData<EdgeEvent> pageData;
        UUID ifOffset = null;
        boolean success;
        do {
            pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge, pageLink);
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
        try {
            downlinkMsgsPackLock.lock();
            boolean success;
            pendingMsgsMap.clear();
            downlinkMsgsPack.forEach(msg -> pendingMsgsMap.put(msg.getDownlinkMsgId(), msg));
            do {
                log.trace("[{}] [{}] downlink msg(s) are going to be send.", this.sessionId, pendingMsgsMap.values().size());
                latch = new CountDownLatch(pendingMsgsMap.values().size());
                List<DownlinkMsg> copy = new ArrayList<>(pendingMsgsMap.values());
                for (DownlinkMsg downlinkMsg : copy) {
                    sendDownlinkMsg(ResponseMsg.newBuilder()
                            .setDownlinkMsg(downlinkMsg)
                            .build());
                }
                success = latch.await(10, TimeUnit.SECONDS);
                if (!success || pendingMsgsMap.values().size() > 0) {
                    log.warn("[{}] Failed to deliver the batch: {}", this.sessionId, pendingMsgsMap.values());
                }
                if (isConnected() && (!success || pendingMsgsMap.values().size() > 0)) {
                    try {
                        Thread.sleep(ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches());
                    } catch (InterruptedException e) {
                        log.error("[{}] Error during sleep between batches", this.sessionId, e);
                    }
                }
            } while (isConnected() && (!success || pendingMsgsMap.values().size() > 0));
            return success;
        } finally {
            downlinkMsgsPackLock.unlock();
        }
    }

    private DownlinkMsg convertToDownlinkMsg(EdgeEvent edgeEvent) {
        log.trace("[{}][{}] converting edge event to downlink msg [{}]", edge.getTenantId(), this.sessionId, edgeEvent);
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
                case CHANGE_OWNER:
                    downlinkMsg = processEntityMessage(edgeEvent, edgeEvent.getAction());
                    log.trace("[{}][{}] entity message processed [{}]", edgeEvent.getTenantId(), this.sessionId, downlinkMsg);
                    break;
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                case ATTRIBUTES_DELETED:
                case TIMESERIES_UPDATED:
                    downlinkMsg = ctx.getTelemetryProcessor().processTelemetryMessageToEdge(edgeEvent);
                    break;
                case CREDENTIALS_REQUEST:
                    downlinkMsg = ctx.getEntityProcessor().processCredentialsRequestMessageToEdge(edgeEvent);
                    break;
                case ENTITY_MERGE_REQUEST:
                    downlinkMsg = ctx.getEntityProcessor().processEntityMergeRequestMessageToEdge(edge, edgeEvent);
                    break;
                case RPC_CALL:
                    downlinkMsg = ctx.getDeviceProcessor().processRpcCallMsgToEdge(edgeEvent);
                    break;
                default:
                    log.warn("[{}][{}] Unsupported action type [{}]", edge.getTenantId(), this.sessionId, edgeEvent.getAction());
            }
        } catch (Exception e) {
            log.error("[{}][{}] Exception during converting edge event to downlink msg {}", edge.getTenantId(), this.sessionId, e);
        }
        return downlinkMsg;
    }

    private List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        return edgeEvents
                .stream()
                .map(this::convertToDownlinkMsg)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    private DownlinkMsg processEntityMessage(EdgeEvent edgeEvent, EdgeEventActionType action) {
        UpdateMsgType msgType = getResponseMsgType(edgeEvent.getAction());
        log.trace("Executing processEntityMessage, edgeEvent [{}], action [{}], msgType [{}]", edgeEvent, action, msgType);
        switch (edgeEvent.getType()) {
            case DEVICE:
                return ctx.getDeviceProcessor().processDeviceToEdge(edge, edgeEvent, msgType, action);
            case DEVICE_PROFILE:
                return ctx.getDeviceProfileProcessor().processDeviceProfileToEdge(edgeEvent, msgType, action);
            case ASSET:
                return ctx.getAssetProcessor().processAssetToEdge(edge, edgeEvent, msgType, action);
            case ENTITY_VIEW:
                return ctx.getEntityViewProcessor().processEntityViewToEdge(edge, edgeEvent, msgType, action);
            case DASHBOARD:
                return ctx.getDashboardProcessor().processDashboardToEdge(edge, edgeEvent, msgType, action);
            case CUSTOMER:
                return ctx.getCustomerProcessor().processCustomerToEdge(edgeEvent, msgType, action);
            case RULE_CHAIN:
                return ctx.getRuleChainProcessor().processRuleChainToEdge(edge, edgeEvent, msgType, action);
            case RULE_CHAIN_METADATA:
                return ctx.getRuleChainProcessor().processRuleChainMetadataToEdge(edgeEvent, msgType);
            case ALARM:
                return ctx.getAlarmProcessor().processAlarmToEdge(edge, edgeEvent, msgType);
            case USER:
                return ctx.getUserProcessor().processUserToEdge(edge, edgeEvent, msgType, action);
            case RELATION:
                return ctx.getRelationProcessor().processRelationToEdge(edgeEvent, msgType);
            case WIDGETS_BUNDLE:
                return ctx.getWidgetBundleProcessor().processWidgetsBundleToEdge(edgeEvent, msgType, action);
            case WIDGET_TYPE:
                return ctx.getWidgetTypeProcessor().processWidgetTypeToEdge(edgeEvent, msgType, action);
            case ADMIN_SETTINGS:
                return ctx.getAdminSettingsProcessor().processAdminSettingsToEdge(edgeEvent);
            case SCHEDULER_EVENT:
                return ctx.getSchedulerEventProcessor().processSchedulerEventToEdge(edgeEvent, msgType);
            case ENTITY_GROUP:
                return ctx.getEntityGroupProcessor().processEntityGroupToEdge(edgeEvent, msgType);
            case WHITE_LABELING:
                return ctx.getWhiteLabelingProcessor().processWhiteLabelingToEdge(edgeEvent);
            case LOGIN_WHITE_LABELING:
                return ctx.getWhiteLabelingProcessor().processLoginWhiteLabelingToEdge(edgeEvent);
            case CUSTOM_TRANSLATION:
                return ctx.getWhiteLabelingProcessor().processCustomTranslationToEdge(edgeEvent);
            case ROLE:
                return ctx.getRoleProcessor().processRoleToEdge(edgeEvent, msgType);
            case GROUP_PERMISSION:
                return ctx.getGroupPermissionsProcessor().processGroupPermissionToEdge(edgeEvent, msgType);
            default:
                log.warn("Unsupported edge event type [{}]", edgeEvent);
                return null;
        }
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
            case CHANGE_OWNER:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        }
    }

    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryFromEdge(edge.getTenantId(), edge.getCustomerId(), entityData));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceCredentialsFromEdge(edge.getTenantId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(ctx.getAlarmProcessor().processAlarmFromEdge(edge.getTenantId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(ctx.getRelationProcessor().processRelationFromEdge(edge.getTenantId(), relationUpdateMsg));
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
                    result.add(ctx.getDeviceProcessor().processDeviceRpcCallResponseFromEdge(edge.getTenantId(), deviceRpcCallMsg));
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
            log.error("[{}] Can't process uplink msg [{}] {}", this.sessionId, uplinkMsg, e);
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
                log.error("[{}] Failed to process edge connection! {}", request.getEdgeRoutingKey(), e);
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
