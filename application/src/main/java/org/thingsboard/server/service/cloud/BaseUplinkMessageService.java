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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
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
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class BaseUplinkMessageService {

    protected static final String QUEUE_SEQ_ID_OFFSET_ATTR_KEY = "queueSeqIdOffset";
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";
    private static final int MAX_UPLINK_ATTEMPTS = 10; // max number of attemps to send downlink message if edge connected

    private static final ReentrantLock uplinkMsgsPackLock = new ReentrantLock();
    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgsMap = new ConcurrentHashMap<>();
    private CountDownLatch latch;

    private volatile boolean isRateLimitViolated = false;
    private volatile boolean sendingInProgress = false;

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
    private AttributesService attributesService;

    @Autowired
    private EdgeRpcClient edgeRpcClient;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    public void processCloudEvents(TenantId tenantId, Long queueSeqIdStart, TimePageLink pageLink) throws Exception {
        PageData<CloudEvent> cloudEvents;
        boolean success;
        do {
            cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);
            if (cloudEvents.getData().isEmpty()) {
                log.info("seqId column of {} table started new cycle", getTableName());
                cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink);
            }
            log.trace("[{}] event(s) are going to be converted.", cloudEvents.getData().size());
            List<UplinkMsg> uplinkMsgsPack = convertToUplinkMsgsPack(tenantId, cloudEvents.getData());
            if (!uplinkMsgsPack.isEmpty()) {
                success = sendUplinkMsgsPack(uplinkMsgsPack);
            } else {
                success = true;
            }
            if (success && cloudEvents.getTotalElements() > 0) {
                CloudEvent latestCloudEvent = cloudEvents.getData().get(cloudEvents.getData().size() - 1);
                try {
                    Long newStartTs = Uuids.unixTimestamp(latestCloudEvent.getUuidId());
                    updateQueueStartTsSeqIdOffset(tenantId, newStartTs, latestCloudEvent.getSeqId());
                    log.debug("Queue offset was updated [{}][{}][{}]", latestCloudEvent.getUuidId(), newStartTs, latestCloudEvent.getSeqId());
                } catch (Exception e) {
                    log.error("Failed to update queue offset [{}]", latestCloudEvent);
                }
            }
            if (success) {
                pageLink = pageLink.nextPageLink();
            }
            if (newMessagesAvailableInGeneralQueue(tenantId)) {
                return;
            }
        } while (!success || cloudEvents.hasNext());
    }

    protected abstract String getTableName();
    protected abstract boolean newMessagesAvailableInGeneralQueue(TenantId tenantId);
    protected abstract void updateQueueStartTsSeqIdOffset(TenantId tenantId, Long newStartTs, Long newSeqId);

    public void processHandleMessages(TenantId tenantId) throws Exception {
        Long cloudEventsQueueSeqIdStart = getQueueSeqIdStart(tenantId).get();
        TimePageLink cloudEventsPageLink = newCloudEventsAvailable(tenantId, cloudEventsQueueSeqIdStart);
        if (cloudEventsPageLink != null) {
            processCloudEvents(tenantId, cloudEventsQueueSeqIdStart, cloudEventsPageLink);
        }
    }

    protected abstract ListenableFuture<Long> getQueueStartTs(TenantId tenantId);
    protected abstract ListenableFuture<Long> getQueueSeqIdStart(TenantId tenantId);

    protected ListenableFuture<Long> getLongAttrByKey(TenantId tenantId, String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, dbCallbackExecutorService);
    }

    public TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart) {
        try {
            long queueStartTs = getQueueStartTs(tenantId).get();
            long queueEndTs = queueStartTs + TimeUnit.DAYS.toMillis(1);
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);
            PageData<CloudEvent> cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);
            if (cloudEvents.getData().isEmpty()) {
                // check if new cycle started (seq_id starts from '1')
                cloudEvents = findCloudEventsFromBeginning(tenantId, pageLink);
                if (cloudEvents.getData().stream().anyMatch(ce -> ce.getSeqId() == 1)) {
                    log.info("newCloudEventsAvailable: new cycle started (seq_id starts from '1')!");
                    return pageLink;
                } else {
                    while (queueEndTs < System.currentTimeMillis()) {
                        log.info("newCloudEventsAvailable: queueEndTs < System.currentTimeMillis() [{}] [{}]", queueEndTs, System.currentTimeMillis());
                        queueStartTs = queueEndTs;
                        queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
                        pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                                0, null, null, queueStartTs, queueEndTs);
                        cloudEvents = findCloudEvents(tenantId, queueSeqIdStart, null, pageLink);
                        if (!cloudEvents.getData().isEmpty()) {
                            return pageLink;
                        }
                    }
                    return null;
                }
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn("Failed to check newCloudEventsAvailable!", e);
            return null;
        }
    }

    protected PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, TimePageLink pageLink) {
        long seqIdEnd = Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount());
        seqIdEnd = Math.max(seqIdEnd, 50L);
        return findCloudEvents(tenantId, 0L, seqIdEnd, pageLink);
    }

    protected abstract PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    protected void updateQueueStartTsSeqIdOffset(TenantId tenantId, String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}][{}][{}]", attrStartTsKey, attrSeqIdKey, startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis()));
        attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

    public void onUplinkResponse(UplinkResponseMsg msg) {
        try {
            if (sendingInProgress) {
                if (msg.getSuccess()) {
                    pendingMsgsMap.remove(msg.getUplinkMsgId());
                    log.debug("Msg has been processed successfully! {}", msg);
                } else if (msg.getErrorMsg().contains(RATE_LIMIT_REACHED)) {
                    log.warn("Msg processing failed! {}", RATE_LIMIT_REACHED);
                    isRateLimitViolated = true;
                } else {
                    log.error("Msg processing failed! Error msg: {}", msg.getErrorMsg());
                }
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("Can't process uplink response message [{}]", msg, e);
        }
    }

    protected boolean sendUplinkMsgsPack(List<UplinkMsg> uplinkMsgsPack) throws InterruptedException {
        uplinkMsgsPackLock.lock();
        try {
            int attempt = 1;
            boolean success;
            LinkedBlockingQueue<UplinkMsg> orderedPendingMsgsQueue = new LinkedBlockingQueue<>();
            pendingMsgsMap.clear();
            uplinkMsgsPack.forEach(msg -> {
                pendingMsgsMap.put(msg.getUplinkMsgId(), msg);
                orderedPendingMsgsQueue.add(msg);
            });
            do {
                log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgsMap.values().size());
                sendingInProgress = true;
                latch = new CountDownLatch(pendingMsgsMap.values().size());
                for (UplinkMsg uplinkMsg : orderedPendingMsgsQueue) {
                    if (edgeRpcClient.getServerMaxInboundMessageSize() != 0 && uplinkMsg.getSerializedSize() > edgeRpcClient.getServerMaxInboundMessageSize()) {
                        log.error("Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                                        "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it." +
                                        "Message {}",
                                uplinkMsg.getSerializedSize(), edgeRpcClient.getServerMaxInboundMessageSize(), uplinkMsg);
                        pendingMsgsMap.remove(uplinkMsg.getUplinkMsgId());
                        latch.countDown();
                    } else {
                        edgeRpcClient.sendUplinkMsg(uplinkMsg);
                    }
                }
                success = latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
                sendingInProgress = false;
                success = success && pendingMsgsMap.isEmpty();
                if (!success) {
                    log.warn("Failed to deliver the batch: {}, attempt: {}", pendingMsgsMap.values(), attempt);
                }
                if (!success) {
                    try {
                        Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());
                    } catch (InterruptedException e) {
                        log.error("Error during sleep between batches", e);
                    }
                }
                if (!success && isRateLimitViolated) {
                    isRateLimitViolated = false;
                    try {
                        TimeUnit.SECONDS.sleep(60);
                    } catch (InterruptedException e) {
                        log.error("Error during sleep on rate limit violation", e);
                    }
                }
                attempt++;
                if (attempt > MAX_UPLINK_ATTEMPTS) {
                    log.warn("Failed to deliver the batch after {} attempts. Next messages are going to be discarded {}",
                            MAX_UPLINK_ATTEMPTS, pendingMsgsMap.values());
                    return true;
                }
            } while (!success);
            return true;
        } finally {
            uplinkMsgsPackLock.unlock();
        }
    }

    private List<UplinkMsg> convertToUplinkMsgsPack(TenantId tenantId, List<CloudEvent> cloudEvents) {
        List<UplinkMsg> result = new ArrayList<>();
        for (CloudEvent cloudEvent : cloudEvents) {
            log.trace("Converting cloud event [{}]", cloudEvent);
            UplinkMsg uplinkMsg = null;
            try {
                switch (cloudEvent.getAction()) {
                    case UPDATED, ADDED, DELETED, ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, CREDENTIALS_UPDATED, RELATION_ADD_OR_UPDATE, RELATION_DELETED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT ->
                            uplinkMsg = convertEntityEventToUplink(tenantId, cloudEvent);
                    case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                            uplinkMsg = telemetryProcessor.convertTelemetryEventToUplink(tenantId, cloudEvent);
                    case ATTRIBUTES_REQUEST -> uplinkMsg = telemetryProcessor.convertAttributesRequestEventToUplink(cloudEvent);
                    case RELATION_REQUEST -> uplinkMsg = relationProcessor.convertRelationRequestEventToUplink(cloudEvent);
                    case RPC_CALL -> uplinkMsg = deviceProcessor.convertRpcCallEventToUplink(cloudEvent);
                    case WIDGET_BUNDLE_TYPES_REQUEST -> uplinkMsg = widgetBundleProcessor.convertWidgetBundleTypesRequestEventToUplink(cloudEvent);
                    case ENTITY_VIEW_REQUEST -> uplinkMsg = entityViewProcessor.convertEntityViewRequestEventToUplink(cloudEvent);
                }
            } catch (Exception e) {
                log.error("Exception during converting events from queue, skipping event [{}]", cloudEvent, e);
            }
            if (uplinkMsg != null) {
                result.add(uplinkMsg);
            }
        }
        return result;
    }

    private UplinkMsg convertEntityEventToUplink(TenantId tenantId, CloudEvent cloudEvent) {
        log.trace("Executing convertEntityEventToUplink, cloudEvent [{}], edgeEventAction [{}]", cloudEvent, cloudEvent.getAction());
        EdgeVersion edgeVersion = EdgeVersion.V_LATEST;
        switch (cloudEvent.getType()) {
            case DEVICE:
                return deviceProcessor.convertDeviceEventToUplink(tenantId, cloudEvent, edgeVersion);
            case DEVICE_PROFILE:
                return deviceProfileProcessor.convertDeviceProfileEventToUplink(cloudEvent, edgeVersion);
            case ALARM:
                return alarmProcessor.convertAlarmEventToUplink(cloudEvent, edgeVersion);
            case ALARM_COMMENT:
                return alarmProcessor.convertAlarmCommentEventToUplink(cloudEvent, edgeVersion);
            case ASSET:
                return assetProcessor.convertAssetEventToUplink(cloudEvent, edgeVersion);
            case ASSET_PROFILE:
                return assetProfileProcessor.convertAssetProfileEventToUplink(cloudEvent, edgeVersion);
            case DASHBOARD:
                return dashboardProcessor.convertDashboardEventToUplink(cloudEvent, edgeVersion);
            case ENTITY_VIEW:
                return entityViewProcessor.convertEntityViewEventToUplink(cloudEvent, edgeVersion);
            case RELATION:
                return relationProcessor.convertRelationEventToUplink(cloudEvent, edgeVersion);
            case TB_RESOURCE:
                return resourceCloudProcessor.convertResourceEventToUplink(cloudEvent, edgeVersion);
            default:
                log.warn("Unsupported cloud event type [{}]", cloudEvent);
                return null;
        }
    }
}
