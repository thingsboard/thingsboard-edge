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
package org.thingsboard.server.service.edge.rpc;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.ConnectResponseCode;
import org.thingsboard.server.gen.edge.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RequestMsg;
import org.thingsboard.server.gen.edge.RequestMsgType;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Data
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();
    private static final ReentrantLock entityGroupCreationLock = new ReentrantLock();

    private static final ReentrantLock responseMsgLock = new ReentrantLock();

    private final Gson gson = new Gson();

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

    private CountDownLatch latch;

    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    Consumer<EdgeId> sessionCloseListener, ObjectMapper mapper) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.mapper = mapper;
        this.ruleEngineMsgProducer = ctx.getProducerProvider().getRuleEngineMsgProducer();
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<RequestMsg>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    }
                    if (ConnectResponseCode.ACCEPTED == responseMsg.getResponseCode()) {
                        connected = true;
                        ctx.getSyncEdgeService().sync(edge);
                    }
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
            }

            @Override
            public void onCompleted() {
                connected = false;
                sessionCloseListener.accept(edge.getId());
                outputStream.onCompleted();
            }
        };
    }

    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(true).build();
                sendResponseMsg(ResponseMsg.newBuilder()
                        .setUplinkResponseMsg(uplinkResponseMsg)
                        .build());
            }

            @Override
            public void onFailure(Throwable t) {
                UplinkResponseMsg uplinkResponseMsg = UplinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                sendResponseMsg(ResponseMsg.newBuilder()
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
            log.error("Can't process downlink response message [{}]", msg, e);
        }
    }

    private void sendResponseMsg(ResponseMsg responseMsg) {
        if (isConnected()) {
            try {
                responseMsgLock.lock();
                outputStream.onNext(responseMsg);
            } catch (Exception e) {
                log.error("Failed to send response message [{}]", responseMsg, e);
                connected = false;
                sessionCloseListener.accept(edge.getId());
            } finally {
                responseMsgLock.unlock();
            }
        }
    }

    void onConfigurationUpdate(Edge edge) {
        try {
            this.edge = edge;
            // TODO: voba - push edge configuration update to edge
//            sendResponseMsg(org.thingsboard.server.gen.integration.ResponseMsg.newBuilder()
//                    .setIntegrationUpdateMsg(IntegrationUpdateMsg.newBuilder()
//                            .setConfiguration(constructIntegrationConfigProto(configuration, defaultConverterProto, downLinkConverterProto))
//                            .build())
//                    .build());
        } catch (Exception e) {
            log.error("Failed to construct proto objects!", e);
        }
    }

    void processHandleMessages() throws ExecutionException, InterruptedException {
        Long queueStartTs = getQueueStartTs().get();
        TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), queueStartTs, null, true);
        TimePageData<EdgeEvent> pageData;
        UUID ifOffset = null;
        boolean success = true;
        do {
            pageData = ctx.getEdgeNotificationService().findEdgeEvents(edge.getTenantId(), edge.getId(), pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}] [{}] event(s) are going to be processed.", this.sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                log.trace("[{}] downlink msg(s) are going to be send.", downlinkMsgsPack.size());

                latch = new CountDownLatch(downlinkMsgsPack.size());
                for (DownlinkMsg downlinkMsg : downlinkMsgsPack) {
                    sendResponseMsg(ResponseMsg.newBuilder()
                            .setDownlinkMsg(downlinkMsg)
                            .build());
                }

                ifOffset = pageData.getData().get(pageData.getData().size() - 1).getUuidId();

                success = latch.await(10, TimeUnit.SECONDS);
                if (!success) {
                    log.warn("Failed to deliver the batch: {}", downlinkMsgsPack);
                }
            }
            if (isConnected() && (!success || pageData.hasNext())) {
                try {
                    Thread.sleep(ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches());
                } catch (InterruptedException e) {
                    log.error("Error during sleep between batches", e);
                }
                if (success) {
                    pageLink = pageData.getNextPageLink();
                }
            }
        } while (isConnected() && (!success || pageData.hasNext()));

        if (ifOffset != null) {
            Long newStartTs = UUIDs.unixTimestamp(ifOffset);
            updateQueueStartTs(newStartTs);
        }
        try {
            Thread.sleep(ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval());
        } catch (InterruptedException e) {
            log.error("Error during sleep", e);
        }
    }

    private List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        List<DownlinkMsg> result = new ArrayList<>();
        for (EdgeEvent edgeEvent : edgeEvents) {
            log.trace("Processing edge event [{}]", edgeEvent);
            try {
                DownlinkMsg downlinkMsg = null;
                ActionType edgeEventAction = ActionType.valueOf(edgeEvent.getEdgeEventAction());
                switch (edgeEventAction) {
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
                        downlinkMsg = processEntityMessage(edgeEvent, edgeEventAction);
                        break;
                    case ATTRIBUTES_UPDATED:
                    case ATTRIBUTES_DELETED:
                    case TIMESERIES_UPDATED:
                        downlinkMsg = processTelemetryMessage(edgeEvent);
                        break;
                }
                if (downlinkMsg != null) {
                    result.add(downlinkMsg);
                }
            } catch (Exception e) {
                log.error("Exception during processing records from queue", e);
            }
        }
        return result;
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
        newStartTs = ++newStartTs; // increments ts by 1 - next edge event search starts from current offset + 1
        List<AttributeKvEntry> attributes = Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, newStartTs), System.currentTimeMillis()));
        ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    private DownlinkMsg processTelemetryMessage(EdgeEvent edgeEvent) {
        log.trace("Executing processTelemetryMessage, edgeEvent [{}]", edgeEvent);
        EntityId entityId = null;
        switch (edgeEvent.getEdgeEventType()) {
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
            case ENTITY_GROUP:
                entityId = new EntityGroupId(edgeEvent.getEntityId());
                break;
        }
        DownlinkMsg downlinkMsg = null;
        if (entityId != null) {
            log.debug("Sending telemetry data msg, entityId [{}], body [{}]", edgeEvent.getEntityId(), edgeEvent.getEntityBody());
            try {
                ActionType actionType = ActionType.valueOf(edgeEvent.getEdgeEventAction());
                downlinkMsg = constructEntityDataProtoMsg(entityId, actionType, JsonUtils.parse(mapper.writeValueAsString(edgeEvent.getEntityBody())));
            } catch (Exception e) {
                log.warn("Can't send telemetry data msg, entityId [{}], body [{}]", edgeEvent.getEntityId(), edgeEvent.getEntityBody(), e);
            }
        }
        return downlinkMsg;
    }

    private DownlinkMsg processEntityMessage(EdgeEvent edgeEvent, ActionType edgeEventAction) {
        UpdateMsgType msgType = getResponseMsgType(ActionType.valueOf(edgeEvent.getEdgeEventAction()));
        log.trace("Executing processEntityMessage, edgeEvent [{}], edgeEventAction [{}], msgType [{}]", edgeEvent, edgeEventAction, msgType);
        switch (edgeEvent.getEdgeEventType()) {
            case EDGE:
                // TODO: voba - add edge update logic
                return null;
            case DEVICE:
                return processDevice(edgeEvent, msgType, edgeEventAction);
            case ASSET:
                return processAsset(edgeEvent, msgType, edgeEventAction);
            case ENTITY_VIEW:
                return processEntityView(edgeEvent, msgType, edgeEventAction);
            case DASHBOARD:
                return processDashboard(edgeEvent, msgType, edgeEventAction);
            case CUSTOMER:
                return processCustomer(edgeEvent, msgType, edgeEventAction);
            case RULE_CHAIN:
                return processRuleChain(edgeEvent, msgType, edgeEventAction);
            case RULE_CHAIN_METADATA:
                return processRuleChainMetadata(edgeEvent, msgType);
            case ALARM:
                return processAlarm(edgeEvent, msgType);
            case USER:
                return processUser(edgeEvent, msgType, edgeEventAction);
            case RELATION:
                return processRelation(edgeEvent, msgType);
            case WIDGETS_BUNDLE:
                return processWidgetsBundle(edgeEvent, msgType, edgeEventAction);
            case WIDGET_TYPE:
                return processWidgetType(edgeEvent, msgType, edgeEventAction);
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

    private DownlinkMsg processDevice(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Device device = ctx.getDeviceService().findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    DeviceUpdateMsg deviceUpdateMsg =
                            ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(msgType, device, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg =
                        ctx.getDeviceUpdateMsgConstructor().constructDeviceDeleteMsg(deviceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            ctx.getDeviceUpdateMsgConstructor().constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDeviceCredentialsUpdateMsg(Collections.singletonList(deviceCredentialsUpdateMsg))
                            .build();
                }
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processAsset(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Asset asset = ctx.getAssetService().findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    AssetUpdateMsg assetUpdateMsg =
                            ctx.getAssetUpdateMsgConstructor().constructAssetUpdatedMsg(msgType, asset, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllAssetUpdateMsg(Collections.singletonList(assetUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg =
                        ctx.getAssetUpdateMsgConstructor().constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllAssetUpdateMsg(Collections.singletonList(assetUpdateMsg))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processEntityView(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        EntityViewId entityViewId = new EntityViewId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(edgeEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    EntityViewUpdateMsg entityViewUpdateMsg =
                            ctx.getEntityViewUpdateMsgConstructor().constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllEntityViewUpdateMsg(Collections.singletonList(entityViewUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                EntityViewUpdateMsg entityViewUpdateMsg =
                        ctx.getEntityViewUpdateMsgConstructor().constructEntityViewDeleteMsg(entityViewId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllEntityViewUpdateMsg(Collections.singletonList(entityViewUpdateMsg))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processDashboard(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        DashboardId dashboardId = new DashboardId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Dashboard dashboard = ctx.getDashboardService().findDashboardById(edgeEvent.getTenantId(), dashboardId);
                if (dashboard != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    DashboardUpdateMsg dashboardUpdateMsg =
                            ctx.getDashboardUpdateMsgConstructor().constructDashboardUpdatedMsg(msgType, dashboard, entityGroupId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllDashboardUpdateMsg(Collections.singletonList(dashboardUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                DashboardUpdateMsg dashboardUpdateMsg =
                        ctx.getDashboardUpdateMsgConstructor().constructDashboardDeleteMsg(dashboardId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllDashboardUpdateMsg(Collections.singletonList(dashboardUpdateMsg))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processCustomer(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        CustomerId customerId = new CustomerId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
                Customer customer = ctx.getCustomerService().findCustomerById(edgeEvent.getTenantId(), customerId);
                if (customer != null) {
                    CustomerUpdateMsg customerUpdateMsg =
                            ctx.getCustomerUpdateMsgConstructor().constructCustomerUpdatedMsg(msgType, customer);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllCustomerUpdateMsg(Collections.singletonList(customerUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                CustomerUpdateMsg customerUpdateMsg =
                        ctx.getCustomerUpdateMsgConstructor().constructCustomerDeleteMsg(customerId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllCustomerUpdateMsg(Collections.singletonList(customerUpdateMsg))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processRuleChain(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeEventAction) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEventAction) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ctx.getRuleChainUpdateMsgConstructor().constructRuleChainUpdatedMsg(edge.getRootRuleChainId(), msgType, ruleChain);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllRuleChainUpdateMsg(Collections.singletonList(ruleChainUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllRuleChainUpdateMsg(Collections.singletonList(ctx.getRuleChainUpdateMsgConstructor().constructRuleChainDeleteMsg(ruleChainId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processRuleChainMetadata(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ctx.getRuleChainService().findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        DownlinkMsg downlinkMsg = null;
        if (ruleChain != null) {
            RuleChainMetaData ruleChainMetaData = ctx.getRuleChainService().loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ctx.getRuleChainUpdateMsgConstructor().constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllRuleChainMetadataUpdateMsg(Collections.singletonList(ruleChainMetadataUpdateMsg))
                        .build();
            }
        }
        return downlinkMsg;
    }

    private DownlinkMsg processUser(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                User user = ctx.getUserService().findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllUserUpdateMsg(Collections.singletonList(ctx.getUserUpdateMsgConstructor().constructUserUpdatedMsg(msgType, user, entityGroupId)))
                            .build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllUserUpdateMsg(Collections.singletonList(ctx.getUserUpdateMsgConstructor().constructUserDeleteMsg(userId)))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                UserCredentials userCredentialsByUserId = ctx.getUserService().findUserCredentialsByUserId(edge.getTenantId(), userId);
                if (userCredentialsByUserId != null && userCredentialsByUserId.isEnabled()) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            ctx.getUserUpdateMsgConstructor().constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllUserCredentialsUpdateMsg(Collections.singletonList(userCredentialsUpdateMsg))
                            .build();
                }
        }
        return downlinkMsg;
    }

    private DownlinkMsg processRelation(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        EntityRelation entityRelation = mapper.convertValue(edgeEvent.getEntityBody(), EntityRelation.class);
        RelationUpdateMsg r = ctx.getRelationUpdateMsgConstructor().constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .addAllRelationUpdateMsg(Collections.singletonList(r))
                .build();
    }

    private DownlinkMsg processAlarm(EdgeEvent edgeEvent, UpdateMsgType msgType) {
        DownlinkMsg downlinkMsg = null;
        try {
            AlarmId alarmId = new AlarmId(edgeEvent.getEntityId());
            Alarm alarm = ctx.getAlarmService().findAlarmByIdAsync(edgeEvent.getTenantId(), alarmId).get();
            if (alarm != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllAlarmUpdateMsg(Collections.singletonList(ctx.getAlarmUpdateMsgConstructor().constructAlarmUpdatedMsg(edge.getTenantId(), msgType, alarm)))
                        .build();
            }
        } catch (Exception e) {
            log.error("Can't process alarm msg [{}] [{}]", edgeEvent, msgType, e);
        }
        return downlinkMsg;
    }

    private DownlinkMsg processWidgetsBundle(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case UPDATED:
                WidgetsBundle widgetsBundle = ctx.getWidgetsBundleService().findWidgetsBundleById(edgeEvent.getTenantId(), widgetsBundleId);
                if (widgetsBundle != null) {
                    WidgetsBundleUpdateMsg widgetsBundleUpdateMsg =
                            ctx.getWidgetsBundleUpdateMsgConstructor().constructWidgetsBundleUpdateMsg(msgType, widgetsBundle);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllWidgetsBundleUpdateMsg(Collections.singletonList(widgetsBundleUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                WidgetsBundleUpdateMsg widgetsBundleUpdateMsg =
                        ctx.getWidgetsBundleUpdateMsgConstructor().constructWidgetsBundleDeleteMsg(widgetsBundleId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllWidgetsBundleUpdateMsg(Collections.singletonList(widgetsBundleUpdateMsg))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processWidgetType(EdgeEvent edgeEvent, UpdateMsgType msgType, ActionType edgeActionType) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeActionType) {
            case ADDED:
            case UPDATED:
                WidgetType widgetType = ctx.getWidgetTypeService().findWidgetTypeById(edgeEvent.getTenantId(), widgetTypeId);
                if (widgetType != null) {
                    WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                            ctx.getWidgetTypeUpdateMsgConstructor().constructWidgetTypeUpdateMsg(msgType, widgetType);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .addAllWidgetTypeUpdateMsg(Collections.singletonList(widgetTypeUpdateMsg))
                            .build();
                }
                break;
            case DELETED:
                WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                        ctx.getWidgetTypeUpdateMsgConstructor().constructWidgetTypeDeleteMsg(widgetTypeId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllWidgetTypeUpdateMsg(Collections.singletonList(widgetTypeUpdateMsg))
                        .build();
                break;
        }
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
                            .addAllSchedulerEventUpdateMsg(Collections.singletonList(ctx.getSchedulerEventUpdateMsgConstructor().constructSchedulerEventUpdatedMsg(msgType, schedulerEvent)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllSchedulerEventUpdateMsg(Collections.singletonList(ctx.getSchedulerEventUpdateMsgConstructor().constructEventDeleteMsg(schedulerEventId)))
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
                            .addAllEntityGroupUpdateMsg(Collections.singletonList(ctx.getEntityGroupUpdateMsgConstructor().constructEntityGroupUpdatedMsg(msgType, entityGroup)))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .addAllEntityGroupUpdateMsg(Collections.singletonList(ctx.getEntityGroupUpdateMsgConstructor().constructEntityGroupDeleteMsg(entityGroupId)))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    private DownlinkMsg processWhiteLabeling(EdgeEvent edgeEvent) {
        WhiteLabelingParams whiteLabelingParams = mapper.convertValue(edgeEvent.getEntityBody(), WhiteLabelingParams.class);
        WhiteLabelingParamsProto whiteLabelingParamsProto =
                ctx.getWhiteLabelingParamsProtoConstructor().constructWhiteLabelingParamsProto(whiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllWhiteLabelingParams(Collections.singletonList(whiteLabelingParamsProto))
                .build();
    }

    private DownlinkMsg processLoginWhiteLabeling(EdgeEvent edgeEvent) {
        LoginWhiteLabelingParams loginWhiteLabelingParams = mapper.convertValue(edgeEvent.getEntityBody(), LoginWhiteLabelingParams.class);
        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                ctx.getWhiteLabelingParamsProtoConstructor().constructLoginWhiteLabelingParamsProto(loginWhiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllLoginWhiteLabelingParams(Collections.singletonList(loginWhiteLabelingParamsProto))
                .build();
    }

    private DownlinkMsg processCustomTranslation(EdgeEvent edgeEvent) {
        CustomTranslation customTranslation = mapper.convertValue(edgeEvent.getEntityBody(), CustomTranslation.class);
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
        AdminSettings adminSettings = mapper.convertValue(edgeEvent.getEntityBody(), AdminSettings.class);
        AdminSettingsUpdateMsg t = ctx.getAdminSettingsUpdateMsgConstructor().constructAdminSettingsUpdateMsg(adminSettings);
        return DownlinkMsg.newBuilder()
                .addAllAdminSettingsUpdateMsg(Collections.singletonList(t))
                .build();
    }

    private UpdateMsgType getResponseMsgType(ActionType actionType) {
        switch (actionType) {
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
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

    private DownlinkMsg constructEntityDataProtoMsg(EntityId entityId, ActionType actionType, JsonElement entityData) {
        EntityDataProto entityDataProto = ctx.getEntityDataMsgConstructor().constructEntityDataMsg(entityId, actionType, entityData);
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataProto));
        return builder.build();
    }

    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getEntityDataList() != null && !uplinkMsg.getEntityDataList().isEmpty()) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    EntityId entityId = constructEntityId(entityData);
                    if ((entityData.hasPostAttributesMsg() || entityData.hasPostTelemetryMsg()) && entityId != null) {
                        TbMsgMetaData metaData = constructBaseMsgMetadata(entityId);
                        metaData.putValue(DataConstants.MSG_SOURCE_KEY, DataConstants.EDGE_MSG_SOURCE);
                        if (entityData.hasPostAttributesMsg()) {
                            metaData.putValue("scope", entityData.getPostAttributeScope());
                            result.add(processPostAttributes(entityId, entityData.getPostAttributesMsg(), metaData));
                        }
                        if (entityData.hasPostTelemetryMsg()) {
                            result.add(processPostTelemetry(entityId, entityData.getPostTelemetryMsg(), metaData));
                        }
                    }
                    if (entityData.hasAttributeDeleteMsg()) {
                        result.add(processAttributeDeleteMsg(entityId, entityData.getAttributeDeleteMsg(), entityData.getEntityType()));
                    }
                }
            }

            if (uplinkMsg.getDeviceUpdateMsgList() != null && !uplinkMsg.getDeviceUpdateMsgList().isEmpty()) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(onDeviceUpdate(deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgList() != null && !uplinkMsg.getDeviceCredentialsUpdateMsgList().isEmpty()) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(onDeviceCredentialsUpdate(deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgList() != null && !uplinkMsg.getAlarmUpdateMsgList().isEmpty()) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(onAlarmUpdate(alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgList() != null && !uplinkMsg.getRelationUpdateMsgList().isEmpty()) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    onRelationUpdate(relationUpdateMsg);
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgList() != null && !uplinkMsg.getRuleChainMetadataRequestMsgList().isEmpty()) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processRuleChainMetadataRequestMsg(edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgList() != null && !uplinkMsg.getAttributesRequestMsgList().isEmpty()) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processAttributesRequestMsg(edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgList() != null && !uplinkMsg.getRelationRequestMsgList().isEmpty()) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processRelationRequestMsg(edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgList() != null && !uplinkMsg.getUserCredentialsRequestMsgList().isEmpty()) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processUserCredentialsRequestMsg(edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgList() != null && !uplinkMsg.getDeviceCredentialsRequestMsgList().isEmpty()) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processDeviceCredentialsRequestMsg(edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getEntityGroupEntitiesRequestMsgList() != null && !uplinkMsg.getEntityGroupEntitiesRequestMsgList().isEmpty()) {
                for (EntityGroupRequestMsg entityGroupEntitiesRequestMsg : uplinkMsg.getEntityGroupEntitiesRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processEntityGroupEntitiesRequest(edge, entityGroupEntitiesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityGroupPermissionsRequestMsgList() != null && !uplinkMsg.getEntityGroupPermissionsRequestMsgList().isEmpty()) {
                for (EntityGroupRequestMsg userGroupPermissionsRequestMsg : uplinkMsg.getEntityGroupPermissionsRequestMsgList()) {
                    result.add(ctx.getSyncEdgeService().processEntityGroupPermissionsRequest(edge, userGroupPermissionsRequestMsg));
                }
            }
        } catch (Exception e) {
            log.error("Can't process uplink msg [{}]", uplinkMsg, e);
        }
        return Futures.allAsList(result);
    }

    private TbMsgMetaData constructBaseMsgMetadata(EntityId entityId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        switch (entityId.getEntityType()) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceById(edge.getTenantId(), new DeviceId(entityId.getId()));
                if (device != null) {
                    metaData.putValue("deviceName", device.getName());
                    metaData.putValue("deviceType", device.getType());
                }
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(edge.getTenantId(), new AssetId(entityId.getId()));
                if (asset != null) {
                    metaData.putValue("assetName", asset.getName());
                    metaData.putValue("assetType", asset.getType());
                }
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(edge.getTenantId(), new EntityViewId(entityId.getId()));
                if (entityView != null) {
                    metaData.putValue("entityViewName", entityView.getName());
                    metaData.putValue("entityViewType", entityView.getType());
                }
                break;
            case ENTITY_GROUP:
                EntityGroup entityGroup = ctx.getEntityGroupService().findEntityGroupById(edge.getTenantId(), new EntityGroupId(entityId.getId()));
                if (entityGroup != null) {
                    metaData.putValue("entityGroupName", entityGroup.getName());
                    metaData.putValue("entityGroupType", entityGroup.getType().name());
                }
                break;
            default:
                log.debug("Using empty metadata for entityId [{}]", entityId);
                break;
        }
        return metaData;
    }

    private EntityId constructEntityId(EntityDataProto entityData) {
        EntityType entityType = EntityType.valueOf(entityData.getEntityType());
        switch (entityType) {
            case DEVICE:
                return new DeviceId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ASSET:
                return new AssetId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_VIEW:
                return new EntityViewId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case DASHBOARD:
                return new DashboardId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case TENANT:
                return new TenantId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case CUSTOMER:
                return new CustomerId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case USER:
                return new UserId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            case ENTITY_GROUP:
                return new EntityGroupId(new UUID(entityData.getEntityIdMSB(), entityData.getEntityIdLSB()));
            default:
                log.warn("Unsupported entity type [{}] during construct of entity id. EntityDataProto [{}]", entityData.getEntityType(), entityData);
                return null;
        }
    }

    private ListenableFuture<Void> processPostTelemetry(EntityId entityId, TransportProtos.PostTelemetryMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, gson.toJson(json));
            ctx.getTbClusterService().pushMsgToRuleEngine(edge.getTenantId(), tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process post telemetry [{}]", msg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private ListenableFuture<Void> processPostAttributes(EntityId entityId, TransportProtos.PostAttributeMsg msg, TbMsgMetaData metaData) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), entityId, metaData, gson.toJson(json));
        ctx.getTbClusterService().pushMsgToRuleEngine(edge.getTenantId(), tbMsg.getOriginator(), tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't process post attributes [{}]", msg, t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private ListenableFuture<Void> processAttributeDeleteMsg(EntityId entityId, AttributeDeleteMsg attributeDeleteMsg, String entityType) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        String scope = attributeDeleteMsg.getScope();
        List<String> attributeNames = attributeDeleteMsg.getAttributeNamesList();
        ctx.getAttributesService().removeAll(edge.getTenantId(), entityId, scope, attributeNames);
        if (EntityType.DEVICE.name().equals(entityType)) {
            Set<AttributeKey> attributeKeys = new HashSet<>();
            for (String attributeName : attributeNames) {
                attributeKeys.add(new AttributeKey(scope, attributeName));
            }
            ctx.getTbClusterService().pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                    edge.getTenantId(), (DeviceId) entityId, attributeKeys), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't process attribute delete msg [{}]", attributeDeleteMsg, t);
                    futureToSet.setException(t);
                }
            });
        }
        return futureToSet;
    }

    private ListenableFuture<Void> onDeviceUpdate(DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId edgeDeviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
                String deviceName = deviceUpdateMsg.getName();
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(edge.getTenantId(), deviceName);
                if (device != null) {
                    // device with this name already exists on the cloud - update ID on the edge
                    if (!device.getId().equals(edgeDeviceId)) {
                        DeviceUpdateMsg d = ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE, device, null);
                        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                                .addAllDeviceUpdateMsg(Collections.singletonList(d))
                                .build();
                        sendResponseMsg(ResponseMsg.newBuilder()
                                .setDownlinkMsg(downlinkMsg)
                                .build());
                    }
                } else {
                    Device deviceById = ctx.getDeviceService().findDeviceById(edge.getTenantId(), edgeDeviceId);
                    if (deviceById != null) {
                        // this ID already used by other device - create new device and update ID on the edge
                        device = createDevice(deviceUpdateMsg);
                        DeviceUpdateMsg d = ctx.getDeviceUpdateMsgConstructor().constructDeviceUpdatedMsg(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE, device, null);
                        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                                .addAllDeviceUpdateMsg(Collections.singletonList(d))
                                .build();
                        sendResponseMsg(ResponseMsg.newBuilder()
                                .setDownlinkMsg(downlinkMsg)
                                .build());
                    } else {
                        device = createDevice(deviceUpdateMsg);
                    }
                }
                // TODO: voba - assign device only in case device is not assigned yet. Missing functionality to check this relation prior assignment
                ctx.getEntityGroupService().addEntityToEntityGroupAll(device.getTenantId(), device.getOwnerId(), device.getId());
                addDeviceToDeviceGroup(device.getId());
                break;
            case ENTITY_UPDATED_RPC_MESSAGE:
                updateDevice(deviceUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                removeDeviceFromDeviceGroup(edgeDeviceId);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type {}", deviceUpdateMsg.getMsgType());
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + deviceUpdateMsg.getMsgType()));
        }
        return Futures.immediateFuture(null);
    }

    private void removeDeviceFromDeviceGroup(DeviceId deviceId) {
        Device deviceToDelete = ctx.getDeviceService().findDeviceById(edge.getTenantId(), deviceId);
        if (deviceToDelete != null) {
            ListenableFuture<EntityGroup> edgeDeviceGroup = findEdgeAllGroup(EntityType.DEVICE);
            Futures.addCallback(edgeDeviceGroup, new FutureCallback<EntityGroup>() {
                @Override
                public void onSuccess(@Nullable EntityGroup entityGroup) {
                    if (entityGroup != null) {
                        ctx.getEntityGroupService().removeEntityFromEntityGroup(edge.getTenantId(), entityGroup.getId(), deviceToDelete.getId());
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Can't remove from edge device group, device id [{}]", deviceId, t);
                }
            }, ctx.getDbCallbackExecutor());
        }
    }

    private void addDeviceToDeviceGroup(DeviceId deviceId) {
        ListenableFuture<EntityGroup> edgeDeviceGroup = findEdgeAllGroup(EntityType.DEVICE);
        Futures.addCallback(edgeDeviceGroup, new FutureCallback<EntityGroup>() {
            @Override
            public void onSuccess(@Nullable EntityGroup entityGroup) {
                if (entityGroup != null) {
                    ctx.getEntityGroupService().addEntityToEntityGroup(edge.getTenantId(), entityGroup.getId(), deviceId);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Can't add device to edge device group, device id [{}]", deviceId, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void updateDevice(DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        Device device = ctx.getDeviceService().findDeviceById(edge.getTenantId(), deviceId);
        device.setName(deviceUpdateMsg.getName());
        device.setType(deviceUpdateMsg.getType());
        device.setLabel(deviceUpdateMsg.getLabel());
        device = ctx.getDeviceService().saveDevice(device);

        requestDeviceCredentialsFromEdge(device);
    }

    private ListenableFuture<Void> onDeviceCredentialsUpdate(DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("Executing onDeviceCredentialsUpdate, deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = ctx.getDeviceService().findDeviceByIdAsync(edge.getTenantId(), deviceId);
        return Futures.transform(deviceFuture, device -> {
            if (device != null) {
                log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                        device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                try {
                    DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edge.getTenantId(), device.getId());
                    deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                    deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                    deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                    ctx.getDeviceCredentialsService().updateDeviceCredentials(edge.getTenantId(), deviceCredentials);
                } catch (Exception e) {
                    log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, ctx.getDbCallbackExecutor());
    }

    private void requestDeviceCredentialsFromEdge(Device device) {
        log.debug("Executing requestDeviceCredentialsFromEdge device [{}]", device);

        DownlinkMsg downlinkMsg = constructDeviceCredentialsRequestMsg(device.getId());
        sendResponseMsg(ResponseMsg.newBuilder()
                .setDownlinkMsg(downlinkMsg)
                .build());
    }

    private DownlinkMsg constructDeviceCredentialsRequestMsg(DeviceId deviceId) {
        DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg));
        return builder.build();
    }

    private Device createDevice(DeviceUpdateMsg deviceUpdateMsg) {
        Device device;
        try {
            deviceCreationLock.lock();
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = new Device();
            device.setTenantId(edge.getTenantId());
            device.setCustomerId(edge.getCustomerId());
            device.setId(deviceId);
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.getLabel());
            device = ctx.getDeviceService().saveDevice(device);
            createDeviceCredentials(device);
            createRelationFromEdge(device.getId());
            ctx.getDeviceStateService().onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(device);
            requestDeviceCredentialsFromEdge(device);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private void createDeviceCredentials(Device device) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
        ctx.getDeviceCredentialsService().createDeviceCredentials(device.getTenantId(), deviceCredentials);
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, device.getId(), getActionTbMsgMetaData(device.getCustomerId()), mapper.writeValueAsString(entityNode));
            sendToRuleEngine(edge.getTenantId(), tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    // TODO: voba - handle success
                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO: voba - handle failure
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    protected void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = ctx.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    private TbMsgMetaData getActionTbMsgMetaData(CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(edge);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData getTbMsgMetaData(Edge edge) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        return metaData;
    }

    private ListenableFuture<EntityGroup> findEdgeAllGroup(EntityType groupType) {
        TenantId tenantId = edge.getTenantId();
        String deviceGroupName = String.format(EntityGroup.GROUP_EDGE_ALL_NAME_PATTERN, edge.getName());
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = ctx.getEntityGroupService()
                .findEntityGroupByTypeAndName(tenantId, edge.getOwnerId(), groupType, deviceGroupName);

        return Futures.transform(futureEntityGroup, optionalEntityGroup -> {
            if (optionalEntityGroup != null && optionalEntityGroup.isPresent()) {
                return optionalEntityGroup.get();
            } else {
                try {
                    entityGroupCreationLock.lock();
                    Optional<EntityGroup> currentEntityGroup = ctx.getEntityGroupService()
                            .findEntityGroupByTypeAndName(tenantId, edge.getOwnerId(), groupType, deviceGroupName).get();
                    if (!currentEntityGroup.isPresent()) {
                        EntityGroup entityGroup = createEntityGroup(deviceGroupName, edge.getOwnerId(), tenantId);
                        ctx.getEntityGroupService().assignEntityGroupToEdge(edge.getTenantId(), entityGroup.getId(), edge.getId(), EntityType.DEVICE);
                        return entityGroup;
                    } else {
                        return currentEntityGroup.get();
                    }
                } catch (Exception e) {
                    log.error("[{}] Can't get entity group by name edge owner id [{}], groupType [{}], deviceGroupName [{}]",
                            tenantId, edge.getOwnerId(), groupType, deviceGroupName, e);
                    throw new RuntimeException(e);
                } finally {
                    entityGroupCreationLock.unlock();
                }
            }
        }, ctx.getDbCallbackExecutor());
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, TenantId tenantId) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(EntityType.DEVICE);
        return ctx.getEntityGroupService().saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    private EntityId getAlarmOriginator(String entityName, org.thingsboard.server.common.data.EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return ctx.getDeviceService().findDeviceByTenantIdAndName(edge.getTenantId(), entityName).getId();
            case ASSET:
                return ctx.getAssetService().findAssetByTenantIdAndName(edge.getTenantId(), entityName).getId();
            case ENTITY_VIEW:
                return ctx.getEntityViewService().findEntityViewByTenantIdAndName(edge.getTenantId(), entityName).getId();
            default:
                return null;
        }
    }

    private ListenableFuture<Void> onAlarmUpdate(AlarmUpdateMsg alarmUpdateMsg) {
        EntityId originatorId = getAlarmOriginator(alarmUpdateMsg.getOriginatorName(), org.thingsboard.server.common.data.EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
        if (originatorId == null) {
            return Futures.immediateFuture(null);
        }
        try {
            Alarm existentAlarm = ctx.getAlarmService().findLatestByOriginatorAndType(edge.getTenantId(), originatorId, alarmUpdateMsg.getType()).get();
            switch (alarmUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (existentAlarm == null || existentAlarm.getStatus().isCleared()) {
                        existentAlarm = new Alarm();
                        existentAlarm.setTenantId(edge.getTenantId());
                        existentAlarm.setType(alarmUpdateMsg.getName());
                        existentAlarm.setOriginator(originatorId);
                        existentAlarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
                        existentAlarm.setStartTs(alarmUpdateMsg.getStartTs());
                        existentAlarm.setClearTs(alarmUpdateMsg.getClearTs());
                        existentAlarm.setPropagate(alarmUpdateMsg.getPropagate());
                    }
                    existentAlarm.setStatus(AlarmStatus.valueOf(alarmUpdateMsg.getStatus()));
                    existentAlarm.setAckTs(alarmUpdateMsg.getAckTs());
                    existentAlarm.setEndTs(alarmUpdateMsg.getEndTs());
                    existentAlarm.setDetails(mapper.readTree(alarmUpdateMsg.getDetails()));
                    ctx.getAlarmService().createOrUpdateAlarm(existentAlarm);
                    break;
                case ALARM_ACK_RPC_MESSAGE:
                    if (existentAlarm != null) {
                        ctx.getAlarmService().ackAlarm(edge.getTenantId(), existentAlarm.getId(), alarmUpdateMsg.getAckTs());
                    }
                    break;
                case ALARM_CLEAR_RPC_MESSAGE:
                    if (existentAlarm != null) {
                        ctx.getAlarmService().clearAlarm(edge.getTenantId(), existentAlarm.getId(), mapper.readTree(alarmUpdateMsg.getDetails()), alarmUpdateMsg.getAckTs());
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    if (existentAlarm != null) {
                        ctx.getAlarmService().deleteAlarm(edge.getTenantId(), existentAlarm.getId());
                    }
                    break;
            }
            return Futures.immediateFuture(null);
        } catch (Exception e) {
            log.error("Error during finding existent alarm", e);
            return Futures.immediateFailedFuture(new RuntimeException("Error during finding existent alarm", e));
        }
    }

    private void onRelationUpdate(RelationUpdateMsg relationUpdateMsg) {
        log.info("onRelationUpdate {}", relationUpdateMsg);
        try {
            EntityRelation entityRelation = new EntityRelation();

            UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
            EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
            entityRelation.setFrom(fromId);

            UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
            EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
            entityRelation.setTo(toId);

            entityRelation.setType(relationUpdateMsg.getType());
            entityRelation.setTypeGroup(RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()));
            entityRelation.setAdditionalInfo(mapper.readTree(relationUpdateMsg.getAdditionalInfo()));
            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (isEntityExists(edge.getTenantId(), entityRelation.getTo())
                            && isEntityExists(edge.getTenantId(), entityRelation.getFrom())) {
                        ctx.getRelationService().saveRelationAsync(edge.getTenantId(), entityRelation);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    ctx.getRelationService().deleteRelation(edge.getTenantId(), entityRelation);
                    break;
                case UNRECOGNIZED:
                    log.error("Unsupported msg type");
            }
        } catch (Exception e) {
            log.error("Error during relation update msg", e);
        }
    }

    private boolean isEntityExists(TenantId tenantId, EntityId entityId) throws ThingsboardException {
        switch (entityId.getEntityType()) {
            case DEVICE:
                return ctx.getDeviceService().findDeviceById(tenantId, new DeviceId(entityId.getId())) != null;
            case ASSET:
                return ctx.getAssetService().findAssetById(tenantId, new AssetId(entityId.getId())) != null;
            case ENTITY_VIEW:
                return ctx.getEntityViewService().findEntityViewById(tenantId, new EntityViewId(entityId.getId())) != null;
            case CUSTOMER:
                return ctx.getCustomerService().findCustomerById(tenantId, new CustomerId(entityId.getId())) != null;
            case USER:
                return ctx.getUserService().findUserById(tenantId, new UserId(entityId.getId())) != null;
            case DASHBOARD:
                return ctx.getDashboardService().findDashboardById(tenantId, new DashboardId(entityId.getId())) != null;
            default:
                throw new ThingsboardException("Unsupported entity type " + entityId.getEntityType(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
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

    private void createRelationFromEdge(EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edge.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        ctx.getRelationService().saveRelation(edge.getTenantId(), relation);
    }

    private EdgeConfiguration constructEdgeConfigProto(Edge edge) throws JsonProcessingException {
        return EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setRoutingKey(edge.getRoutingKey())
                .setType(edge.getType())
                .setCloudType("PE")
                .build();
    }

    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
