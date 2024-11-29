/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class BaseUplinkMessageService {
    private static final String CANT_PROCESS_UPLINK_RESPONSE_MESSAGE = "Can't process uplink response message";
    private static final String MSG_HAS_BEEN_PROCESSED_SUCCESSFULLY = "Msg has been processed successfully!";
    private static final String MSG_PROCESSING_FAILED = "Msg processing failed!";
    private static final String EVENTS_ARE_GOING_TO_BE_CONVERTED = "event(s) are going to be converted.";
    private static final String INTERRUPTED_WHILE_WAITING_FOR_LATCH = "Interrupted while waiting for latch. ";
    private static final String UPLINK_MSGS_ARE_GOING_TO_BE_SEND = "uplink msg(s) are going to be send.";
    private static final String INTERRUPTED_EXCEPTION = "sendUplinkMsgPack throw InterruptedException";
    private static final String UPLINK_MSG_SIZE_ERROR_MESSAGE =
            "Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                    "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it. Message {}";
    private static final String FAILED_TO_DELIVER_THE_BATCH = "Failed to deliver the batch:";
    private static final String ERROR_DURING_SLEEP = "Error during sleep between batches or on rate limit violation";
    private static final String NEXT_MESSAGES_ARE_GOING_TO_BE_DISCARDED = "Next messages are going to be discarded";
    private static final String CONVERTING_CLOUD_EVENT = "Converting cloud event";
    private static final String EXCEPTION_DURING_CONVERTING_EVENTS = "Exception during converting events from queue, skipping event";
    private static final String UNSUPPORTED_ACTION_TYPE = "Unsupported action type";
    private static final String EXECUTING_CONVERT_ENTITY_EVENT = "Executing convertEntityEventToUplink";
    private static final String UNSUPPORTED_CLOUD_EVENT_TYPE = "Unsupported cloud event type";
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";

    private static final ReentrantLock uplinkMsgPackLock = new ReentrantLock();
    private static final int MAX_SEND_UPLINK_ATTEMPTS = 10;

    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgMap = new ConcurrentHashMap<>();
    private volatile boolean isRateLimitViolated = false;
    private volatile boolean sendingInProgress = false;

    private CountDownLatch latch;

    @Value("${cloud.uplink_pack_timeout_sec:60}")
    private long uplinkPackTimeoutSec;

    @Autowired
    protected CloudEventService cloudEventService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private RelationCloudProcessor relationProcessor;

    @Autowired
    private DeviceCloudProcessor deviceProcessor;

    @Autowired
    private DeviceProfileCloudProcessor deviceProfileProcessor;

    @Autowired
    private AlarmCloudProcessor alarmProcessor;

    @Autowired
    private TelemetryCloudProcessor telemetryProcessor;

    @Autowired
    private WidgetBundleCloudProcessor widgetBundleProcessor;

    @Autowired
    private EntityViewCloudProcessor entityViewProcessor;

    @Autowired
    private DashboardCloudProcessor dashboardProcessor;

    @Autowired
    private AssetCloudProcessor assetProcessor;

    @Autowired
    private AssetProfileCloudProcessor assetProfileProcessor;

    @Autowired
    private ResourceCloudProcessor resourceCloudProcessor;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    public void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            confirmSuccessMessage(msg);
        } catch (Exception e) {
            log.error(CANT_PROCESS_UPLINK_RESPONSE_MESSAGE + " [{}]", msg, e);
        }
    }

    private void confirmSuccessMessage(UplinkResponseMsg msg) {
        if (sendingInProgress) {
            if (msg.getSuccess()) {
                pendingMsgMap.remove(msg.getUplinkMsgId());
                log.debug(MSG_HAS_BEEN_PROCESSED_SUCCESSFULLY + " {}", msg);
            } else {
                logError(msg);
            }
            latch.countDown();
        }
    }

    private void logError(UplinkResponseMsg msg) {
        if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
            log.warn(MSG_PROCESSING_FAILED + " {}", RATE_LIMIT_REACHED);
            isRateLimitViolated = true;
        } else {
            log.error(MSG_PROCESSING_FAILED + " Error msg: {}", msg.getErrorMsg());
        }
    }

    protected void sendCloudEvents(PageData<CloudEvent> cloudEvents) {
        log.trace("[{}] " + EVENTS_ARE_GOING_TO_BE_CONVERTED, cloudEvents.getData().size());
        List<UplinkMsg> uplinkMsgPack = convertToUplinkMsgPack(cloudEvents.getData());

        if (!uplinkMsgPack.isEmpty()) {
            lockMsgPack(uplinkMsgPack);
        }
    }

    private void lockMsgPack(List<UplinkMsg> uplinkMsgPack) {
        try {
            uplinkMsgPackLock.lock();
            processMsgPack(uplinkMsgPack);
        } finally {
            uplinkMsgPackLock.unlock();
        }
    }

    private void processMsgPack(List<UplinkMsg> uplinkMsgPack) {
        pendingMsgMap.clear();
        uplinkMsgPack.forEach(msg -> pendingMsgMap.put(msg.getUplinkMsgId(), msg));
        LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue = new LinkedBlockingQueue<>(pendingMsgMap.values());
        int attempt = 1;

        while (!uplinkMsgPackSent(orderedPendingMsgQueue, attempt)) {
            attempt++;
        }
    }

    private boolean uplinkMsgPackSent(LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue, int attempt) {
        log.trace("[{}] " + UPLINK_MSGS_ARE_GOING_TO_BE_SEND, pendingMsgMap.values().size());

        boolean success = sendUplinkMsgPack(orderedPendingMsgQueue) && pendingMsgMap.isEmpty();

        if (!success) {
            sleepThread(attempt);
        }

        attempt++;

        return checkAttempt(attempt) && success;
    }

    private boolean sendUplinkMsgPack(LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue) {
        try {
            boolean success;

            sendingInProgress = true;
            latch = new CountDownLatch(pendingMsgMap.values().size());
            orderedPendingMsgQueue.forEach(this::sendUplinkMsg);

            success = latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
            sendingInProgress = false;

            return success;
        } catch (InterruptedException e) {
            log.error(INTERRUPTED_EXCEPTION, e);
            throw new RuntimeException(INTERRUPTED_WHILE_WAITING_FOR_LATCH + e);
        }
    }

    private void sendUplinkMsg(UplinkMsg uplinkMsg) {
        if (isCorrectMessageSize(uplinkMsg)) {
            edgeRpcClient.sendUplinkMsg(uplinkMsg);
        } else {
            log.error(UPLINK_MSG_SIZE_ERROR_MESSAGE, uplinkMsg.getSerializedSize(), edgeRpcClient.getServerMaxInboundMessageSize(), uplinkMsg);
            pendingMsgMap.remove(uplinkMsg.getUplinkMsgId());
            latch.countDown();
        }
    }

    private boolean isCorrectMessageSize(UplinkMsg uplinkMsg) {
        return edgeRpcClient.getServerMaxInboundMessageSize() == 0 ||
                uplinkMsg.getSerializedSize() <= edgeRpcClient.getServerMaxInboundMessageSize();
    }

    private void sleepThread(int attempt) {
        log.warn(FAILED_TO_DELIVER_THE_BATCH + " {}, attempt: {}", pendingMsgMap.values(), attempt);
        try {
            Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());

            if (isRateLimitViolated) {
                isRateLimitViolated = false;
                TimeUnit.SECONDS.sleep(60);
            }
        } catch (InterruptedException e) {
            log.error(ERROR_DURING_SLEEP, e);
        }
    }

    private boolean checkAttempt(int attempt) {
        if (attempt > MAX_SEND_UPLINK_ATTEMPTS) {
            log.warn(FAILED_TO_DELIVER_THE_BATCH + " after {} attempts. " + NEXT_MESSAGES_ARE_GOING_TO_BE_DISCARDED + " {}",
                    MAX_SEND_UPLINK_ATTEMPTS, pendingMsgMap.values());
            return false;
        }
        return true;
    }

    private List<UplinkMsg> convertToUplinkMsgPack(List<CloudEvent> cloudEvents) {
        return cloudEvents.stream()
                .map(this::convertEventToUplink)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private UplinkMsg convertEventToUplink(CloudEvent cloudEvent) {
        log.trace(CONVERTING_CLOUD_EVENT + " [{}]", cloudEvent);
        try {
            return switch (cloudEvent.getAction()) {
                case UPDATED, ADDED, DELETED, ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, CREDENTIALS_UPDATED,
                     RELATION_ADD_OR_UPDATE, RELATION_DELETED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER,
                     ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> convertEntityEventToUplink(cloudEvent);
                case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                        telemetryProcessor.convertTelemetryEventToUplink(cloudEvent.getTenantId(), cloudEvent);
                case ATTRIBUTES_REQUEST -> telemetryProcessor.convertAttributesRequestEventToUplink(cloudEvent);
                case RELATION_REQUEST -> relationProcessor.convertRelationRequestEventToUplink(cloudEvent);
                case RPC_CALL -> deviceProcessor.convertRpcCallEventToUplink(cloudEvent);
                case WIDGET_BUNDLE_TYPES_REQUEST -> widgetBundleProcessor.convertWidgetBundleTypesRequestEventToUplink(cloudEvent);
                case ENTITY_VIEW_REQUEST -> entityViewProcessor.convertEntityViewRequestEventToUplink(cloudEvent);
                default -> {
                    log.warn(UNSUPPORTED_ACTION_TYPE + " [{}]", cloudEvent);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error(EXCEPTION_DURING_CONVERTING_EVENTS + " [{}]", cloudEvent, e);
            return null;
        }
    }

    @Nullable
    private UplinkMsg convertEntityEventToUplink(CloudEvent cloudEvent) {
        log.trace(EXECUTING_CONVERT_ENTITY_EVENT + ", cloudEvent [{}], edgeEventAction [{}]", cloudEvent, cloudEvent.getAction());
        EdgeVersion edgeVersion = EdgeVersion.V_LATEST;

        return switch (cloudEvent.getType()) {
            case DEVICE -> deviceProcessor.convertDeviceEventToUplink(cloudEvent.getTenantId(), cloudEvent, edgeVersion);
            case DEVICE_PROFILE -> deviceProfileProcessor.convertDeviceProfileEventToUplink(cloudEvent, edgeVersion);
            case ALARM -> alarmProcessor.convertAlarmEventToUplink(cloudEvent, edgeVersion);
            case ALARM_COMMENT -> alarmProcessor.convertAlarmCommentEventToUplink(cloudEvent, edgeVersion);
            case ASSET -> assetProcessor.convertAssetEventToUplink(cloudEvent, edgeVersion);
            case ASSET_PROFILE -> assetProfileProcessor.convertAssetProfileEventToUplink(cloudEvent, edgeVersion);
            case DASHBOARD -> dashboardProcessor.convertDashboardEventToUplink(cloudEvent, edgeVersion);
            case ENTITY_VIEW -> entityViewProcessor.convertEntityViewEventToUplink(cloudEvent, edgeVersion);
            case RELATION -> relationProcessor.convertRelationEventToUplink(cloudEvent, edgeVersion);
            case TB_RESOURCE -> resourceCloudProcessor.convertResourceEventToUplink(cloudEvent, edgeVersion);
            default -> {
                log.warn(UNSUPPORTED_CLOUD_EVENT_TYPE + " [{}]", cloudEvent);
                yield null;
            }
        };
    }

    protected boolean isProcessContinue(TenantId tenantId) {
        return !newMessagesAvailableInGeneralQueue(tenantId);
    }

    protected abstract boolean newMessagesAvailableInGeneralQueue(TenantId tenantId);

}
