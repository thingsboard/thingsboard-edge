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
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.utils.EventDeduplicationExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.thingsboard.server.common.data.DataConstants.UPDATE_FIRMWARE;
import static org.thingsboard.server.common.data.DataConstants.UPDATE_SOFTWARE;

/**
 * Created by ashvayka on 25.06.18.
 */
@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSchedulerService extends TbApplicationEventListener<PartitionChangeEvent> implements SchedulerService {

    private final TenantService tenantService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final SchedulerEventService schedulerEventService;
    private final OtaPackageStateService firmwareStateService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final EntityGroupService entityGroupService;
    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    private final OtaPackageService otaPackageService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentMap<TenantId, List<SchedulerEventId>> tenantEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<SchedulerEventId, SchedulerEventMetaData> eventsMetaData = new ConcurrentHashMap<>();
    final ConcurrentMap<TopicPartitionInfo, Set<TenantId>> partitionedTenants = new ConcurrentHashMap<>();
    ListeningScheduledExecutorService queueExecutor;

    volatile boolean firstRun = true;

    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("scheduler-service")));
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public void onSchedulerEventAdded(SchedulerEventInfo event) {
        sendSchedulerEvent(event.getTenantId(), event.getId(), true, false, false);
    }

    @Override
    public void onSchedulerEventUpdated(SchedulerEventInfo event) {
        sendSchedulerEvent(event.getTenantId(), event.getId(), false, true, false);
    }

    @Override
    public void onSchedulerEventDeleted(SchedulerEventInfo event) {
        sendSchedulerEvent(event.getTenantId(), event.getId(), false, false, true);
    }

    @Override
    public void onQueueMsg(TransportProtos.SchedulerServiceMsgProto proto, TbCallback callback) {
        log.debug("onQueueMsg proto {}", proto);
        TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        SchedulerEventId eventId = new SchedulerEventId(new UUID(proto.getEventIdMSB(), proto.getEventIdLSB()));
        if (proto.getDeleted()) {
            onEventDeleted(eventId);
        } else {
            SchedulerEventInfo event = schedulerEventService.findSchedulerEventInfoById(tenantId, eventId);
            if (event != null) {
                if (proto.getAdded() && !eventsMetaData.containsKey(event.getId())) {
                    scheduleAndAddToMap(event);
                } else {
                    SchedulerEventMetaData oldMd = eventsMetaData.remove(event.getId());
                    if (oldMd != null && oldMd.getNextTaskFuture() != null) {
                        oldMd.getNextTaskFuture().cancel(false);
                    }
                    scheduleAndAddToMap(event);
                }
            }
        }
        callback.onSuccess();
    }

    /**
     * DiscoveryService will call this event from the single thread (one-by-one).
     * Events order is guaranteed by DiscoveryService.
     * The only concurrency is expected from the [main] thread on Application started.
     * Async implementation. Locks is not allowed by design.
     * Any locks or delays in this module will affect DiscoveryService and entire system
     * */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            log.debug("onTbApplicationEvent ServiceType is TB_CORE, processing queue {}", partitionChangeEvent);
            subscribeQueue.add(partitionChangeEvent.getPartitions());
            queueExecutor.submit(this::pollInitStateFromDB);
        }
    }

    void pollInitStateFromDB() {
        final Set<TopicPartitionInfo> partitions = getLatestPartitionsFromQueue();
        if (partitions == null) {
            log.info("Scheduler service. Nothing to do. partitions is null");
            return;
        }
        initStateFromDB(partitions);
    }

    void initStateFromDB(Set<TopicPartitionInfo> partitions) {
        try {
            log.info("Scheduler service {}", firstRun ? "Initializing" : "Updating");

            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedTenants.keySet());
            log.trace("calculated addedPartitions {}", addedPartitions);

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedTenants.keySet());
            removedPartitions.removeAll(partitions);
            log.trace("calculated removedPartitions {}", removedPartitions);

            // We no longer manage current partition of tenants;
            removedPartitions.forEach(partition -> {
                Set<TenantId> tenants = Optional.ofNullable(partitionedTenants.remove(partition)).orElseGet(Collections::emptySet);
                log.trace("removing partition {}, tenants found {}", partition, tenants);
                tenants.forEach(tenantId -> removeEvents(partition, tenantId));
            });

            addedPartitions.forEach(tpi -> partitionedTenants.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            long ts = System.currentTimeMillis();
            List<Tenant> tenants = getAllTenants();
            for (Tenant tenant : tenants) {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), tenant.getId());
                if (addedPartitions.contains(tpi)) {
                    addToPartitionedTenants(tenant, tpi);
                    addEventsForTenant(ts, tenant);
                }
            }

            log.info("Scheduler service {}.", firstRun ? "initialized" : "updated");
            firstRun = false;
        } catch (Throwable t) {
            log.warn("Failed to init scheduler states from DB", t);
        }
    }

    void removeEvents(TopicPartitionInfo partition, TenantId tenantId) {
        log.trace("removing partition {} for tenantId {}", partition, tenantId);
        tenantEvents.getOrDefault(tenantId, emptyList()).forEach(this::onEventDeleted);
        tenantEvents.remove(tenantId);
    }

    boolean addToPartitionedTenants(Tenant tenant, TopicPartitionInfo tpi) {
        return partitionedTenants.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()).add(tenant.getId());
    }

    List<Tenant> getAllTenants() {
        return tenantService.findTenants(new PageLink(Integer.MAX_VALUE)).getData();
    }

    Set<TopicPartitionInfo> getLatestPartitionsFromQueue() {
        log.debug("getLatestPartitionsFromQueue, queue size {}", subscribeQueue.size());
        Set<TopicPartitionInfo> partitions = null;
        while (!subscribeQueue.isEmpty()) {
            partitions = subscribeQueue.poll();
            log.debug("polled from the queue partitions {}", partitions);
        }
        log.debug("getLatestPartitionsFromQueue, partitions {}", partitions);
        return partitions;
    }

    private void addEventsForTenant(long ts, Tenant tenant) {
        log.debug("[{}] Fetching scheduled events for tenant.", tenant.getId());
        List<SchedulerEventId> eventIds = new ArrayList<>();
        List<SchedulerEventInfo> events = schedulerEventService.findSchedulerEventsByTenantId(tenant.getId());
        long scheduled = 0L;
        long passedAway = 0L;
        for (SchedulerEventInfo event : events) {
            SchedulerEventMetaData md = getSchedulerEventMetaData(event);
            if (!md.passedAway(ts)) {
                eventsMetaData.put(event.getId(), md);
                eventIds.add(event.getId());
                scheduled++;
            } else {
                passedAway++;
            }

            scheduleNextEvent(ts, event, md);
        }
        tenantEvents.put(tenant.getId(), eventIds);
        log.debug("[{}] Fetched scheduled events for tenant. Scheduling {} events. Found {} passed events.", tenant.getId(), scheduled, passedAway);
    }

    private void scheduleNextEvent(long ts, SchedulerEventInfo event, SchedulerEventMetaData md) {
        long eventTs = md.getNextEventTime(ts);
        if (eventTs != 0L) {
            log.debug("schedule next event for ts {}, event {}, metadata {}", ts, event, md);
            long eventDelay = eventTs - ts;
            md.setNextTaskFuture(queueExecutor.schedule(() -> processEvent(event.getTenantId(), event.getId()), eventDelay, TimeUnit.MILLISECONDS));
        }
    }

    private SchedulerEventMetaData getSchedulerEventMetaData(SchedulerEventInfo event) {
        JsonNode node = event.getSchedule();
        long startTime = node.get("startTime").asLong();
        String timezone = node.get("timezone").asText();
        JsonNode repeatNode = node.get("repeat");
        SchedulerRepeat repeat = null;
        if (repeatNode != null) {
            try {
                repeat = mapper.treeToValue(repeatNode, SchedulerRepeat.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to read scheduler config", e);
            }
        }
        return new SchedulerEventMetaData(event, startTime, timezone, repeat);
    }

    private void processEvent(TenantId tenantId, SchedulerEventId eventId) {
        log.debug("processEvent tenant {}, event {}", tenantId, eventId);
        SchedulerEventMetaData md = eventsMetaData.get(eventId);
        if (md != null) {
            SchedulerEvent event = schedulerEventService.findSchedulerEventById(tenantId, eventId);
            if (event != null) {
                try {
                    JsonNode configuration = event.getConfiguration();
                    String msgType = getMsgType(event, configuration);
                    EntityId originatorId = getOriginatorId(eventId, configuration);

                    boolean isFirmwareUpdate = UPDATE_FIRMWARE.equals(event.getType());
                    boolean isSoftwareUpdate = UPDATE_SOFTWARE.equals(event.getType());

                    if (isFirmwareUpdate || isSoftwareUpdate) {
                        OtaPackageId firmwareId = JacksonUtil.convertValue(configuration.get("msgBody"), OtaPackageId.class);

                        OtaPackageInfo firmwareInfo = otaPackageService.findOtaPackageInfoById(tenantId, firmwareId);

                        if (firmwareInfo == null) {
                            throw new RuntimeException("Failed to process event: OtaPackage with id [" + firmwareId + "] not found!");
                        }

                        switch (originatorId.getEntityType()) {
                            case DEVICE:
                                Device device = deviceService.findDeviceById(tenantId, (DeviceId) originatorId);
                                if (device == null) {
                                    throw new RuntimeException("Failed to process event: Device with id [" + originatorId + "] not found!");
                                }
                                if (isFirmwareUpdate) {
                                    device.setFirmwareId(firmwareId);
                                } else {
                                    device.setSoftwareId(firmwareId);
                                }
                                firmwareStateService.update(deviceService.saveDevice(device));
                                break;
                            case ENTITY_GROUP:
                                EntityGroup deviceGroup = entityGroupService.findEntityGroupById(tenantId, (EntityGroupId) originatorId);
                                if (deviceGroup == null) {
                                    throw new RuntimeException("Failed to process event: Device group with id [" + originatorId + "] not found!");
                                }
                                DeviceGroupOtaPackage oldDgf = deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(deviceGroup.getId(), firmwareInfo.getType());
                                DeviceGroupOtaPackage dgop = new DeviceGroupOtaPackage();
                                dgop.setOtaPackageType(firmwareInfo.getType());
                                dgop.setGroupId(deviceGroup.getId());
                                dgop.setOtaPackageId(firmwareId);
                                if (oldDgf != null) {
                                    dgop.setId(oldDgf.getId());
                                }
                                firmwareStateService.update(tenantId, deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, dgop), oldDgf);
                                break;
                            case DEVICE_PROFILE:
                                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) originatorId);
                                if (deviceProfile == null) {
                                    throw new RuntimeException("Failed to process event: Device profile with id [" + originatorId + "] not found!");
                                }
                                if (isFirmwareUpdate) {
                                    deviceProfile.setFirmwareId(firmwareId);
                                } else {
                                    deviceProfile.setSoftwareId(firmwareId);
                                }
                                firmwareStateService.update(deviceProfileService.saveDeviceProfile(deviceProfile), isFirmwareUpdate, isSoftwareUpdate);
                                break;
                            default:
                                throw new RuntimeException("Not implemented!");
                        }
                    }
                    TbMsgMetaData tbMsgMD = getTbMsgMetaData(event, configuration);
                    TbMsg tbMsg = TbMsg.newMsg(msgType, originatorId, tbMsgMD, TbMsgDataType.JSON, getMsgBody(event.getConfiguration()));
                    log.debug("pushing message to the rule engine tenant {}, originator {}, msg {}", tenantId, originatorId, tbMsg);
                    clusterService.pushMsgToRuleEngine(tenantId, originatorId, tbMsg, null);
                } catch (Exception e) {
                    log.error("[{}][{}] Failed to trigger event", event.getTenantId(), eventId, e);
                }
                scheduleNextEvent(System.currentTimeMillis(), event, md);
            } else {
                log.debug("[{}] Triggered event is not present in the database.", eventId);
                eventsMetaData.remove(eventId);
            }
        } else {
            log.debug("[{}] Triggered processing of removed event.", eventId);
        }
    }

    private String getMsgBody(JsonNode configuration) throws JsonProcessingException {
        return mapper.writeValueAsString(configuration.get("msgBody"));
    }

    private String getMsgType(SchedulerEvent event, JsonNode configuration) {
        return (configuration.has("msgType") && !configuration.get("msgType").isNull()) ? configuration.get("msgType").asText() : event.getType();
    }

    private EntityId getOriginatorId(SchedulerEventId eventId, JsonNode configuration) {
        EntityId originatorId = eventId;
        if (configuration.has("originatorId") && !configuration.get("originatorId").isNull()) {
            JsonNode entityId = configuration.get("originatorId");
            if (entityId != null) {
                if (entityId.has("entityType") && !entityId.get("entityType").isNull()
                        && entityId.has("id") && !entityId.get("id").isNull())
                    originatorId = EntityIdFactory.getByTypeAndId(entityId.get("entityType").asText(), entityId.get("id").asText());
            }
        }
        return originatorId;
    }

    private TbMsgMetaData getTbMsgMetaData(SchedulerEvent event, JsonNode configuration) throws JsonProcessingException {
        HashMap<String, String> metaData = new HashMap<>();
        if (configuration.has("metadata") && !configuration.get("metadata").isNull()) {
            for (Iterator<Entry<String, JsonNode>> it = configuration.get("metadata").fields(); it.hasNext(); ) {
                Entry<String, JsonNode> kv = it.next();
                metaData.put(kv.getKey(), kv.getValue().asText());
            }
        } else {
            metaData.put("customerId", event.getCustomerId().getId().toString());
            metaData.put("eventName", event.getName());
            if (event.getAdditionalInfo() != null) {
                metaData.put("additionalInfo", mapper.writeValueAsString(event.getAdditionalInfo()));
            }
        }
        return new TbMsgMetaData(metaData);
    }

    void onEventDeleted(SchedulerEventId eventId) {
        log.debug("onEventDeleted event {}", eventId);
        SchedulerEventMetaData oldMd = eventsMetaData.remove(eventId);
        if (oldMd != null && oldMd.getNextTaskFuture() != null) {
            oldMd.getNextTaskFuture().cancel(false);
        }
    }

    private void scheduleAndAddToMap(SchedulerEventInfo event) {
        log.debug("scheduleAndAddToMap event {}", event);
        long ts = System.currentTimeMillis();
        SchedulerEventMetaData eventMd = getSchedulerEventMetaData(event);
        scheduleNextEvent(ts, event, eventMd);
        eventsMetaData.put(event.getId(), eventMd);
    }

    private void sendSchedulerEvent(TenantId tenantId, SchedulerEventId eventId, boolean added, boolean updated, boolean deleted) {
        log.trace("sendSchedulerEvent tenantId {}, eventId {}, added {}, updated {}, deleted {}", tenantId, eventId, added, updated, deleted);
        TransportProtos.SchedulerServiceMsgProto.Builder builder = TransportProtos.SchedulerServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setEventIdMSB(eventId.getId().getMostSignificantBits());
        builder.setEventIdLSB(eventId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        TransportProtos.SchedulerServiceMsgProto msg = builder.build();
        // Routing by tenant id.
        clusterService.pushMsgToCore(tenantId, tenantId, TransportProtos.ToCoreMsg.newBuilder().setSchedulerServiceMsg(msg).build(), null);
    }
}
