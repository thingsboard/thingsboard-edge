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
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.TbClusterService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 25.06.18.
 */
@Service
@Slf4j
public class DefaultSchedulerService implements SchedulerService {

    private final TenantService tenantService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final SchedulerEventService schedulerEventService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentMap<TenantId, List<SchedulerEventId>> tenantEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<SchedulerEventId, SchedulerEventMetaData> eventsMetaData = new ConcurrentHashMap<>();
    private final ConcurrentMap<TopicPartitionInfo, Set<TenantId>> partitionedTenants = new ConcurrentHashMap<>();
    private ListeningScheduledExecutorService queueExecutor;

    private volatile boolean clusterUpdatePending = false;
    private volatile boolean firstRun = true;

    public DefaultSchedulerService(TenantService tenantService, TbClusterService clusterService, PartitionService partitionService, SchedulerEventService schedulerEventService) {
        this.tenantService = tenantService;
        this.clusterService = clusterService;
        this.partitionService = partitionService;
        this.schedulerEventService = schedulerEventService;
    }

    @PostConstruct
    public void init() {
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
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

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            synchronized (this) {
                if (!clusterUpdatePending) {
                    clusterUpdatePending = true;
                    queueExecutor.submit(() -> {
                        clusterUpdatePending = false;
                        initStateFromDB(partitionChangeEvent.getPartitions());
                    });
                }
            }
        }
    }

    private void initStateFromDB(Set<TopicPartitionInfo> partitions) {
        try {
            log.info("{}} scheduler service.", firstRun ? "Initializing" : "Updating");
            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(partitionedTenants.keySet());

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(partitionedTenants.keySet());
            removedPartitions.removeAll(partitions);

            // We no longer manage current partition of tenants;
            removedPartitions.forEach(partition -> {
                Set<TenantId> tenants = partitionedTenants.remove(partition);
                tenants.forEach(tenantId -> {
                    tenantEvents.getOrDefault(tenantId, Collections.emptyList()).forEach(this::onEventDeleted);
                    tenantEvents.remove(tenantId);
                });
            });

            addedPartitions.forEach(tpi -> partitionedTenants.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()));

            long ts = System.currentTimeMillis();
            List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
            for (Tenant tenant : tenants) {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), tenant.getId());
                if (addedPartitions.contains(tpi)) {
                    addEventsForTenant(ts, tenant);
                }
            }

            log.info("Scheduler service {}.", firstRun ? "initialized" : "updated");
            firstRun = false;
        } catch (Throwable t) {
            log.warn("Failed to init device states from DB", t);
        }
    }

    private void addEventsForTenant(long ts, Tenant tenant) {
        List<SchedulerEventId> eventIds = new ArrayList<>();
        log.debug("[{}] Fetching scheduled events for tenant.", tenant.getId());
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
        SchedulerEventMetaData md = eventsMetaData.get(eventId);
        if (md != null) {
            SchedulerEvent event = schedulerEventService.findSchedulerEventById(tenantId, eventId);
            if (event != null) {
                try {
                    JsonNode configuration = event.getConfiguration();
                    String msgType = getMsgType(event, configuration);
                    EntityId originatorId = getOriginatorId(eventId, configuration);
                    TbMsgMetaData tbMsgMD = getTbMsgMetaData(event, configuration);
                    TbMsg tbMsg = TbMsg.newMsg(msgType, originatorId, tbMsgMD, TbMsgDataType.JSON, getMsgBody(event.getConfiguration()));
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

    private void onEventDeleted(SchedulerEventId eventId) {
        SchedulerEventMetaData oldMd = eventsMetaData.remove(eventId);
        if (oldMd != null && oldMd.getNextTaskFuture() != null) {
            oldMd.getNextTaskFuture().cancel(false);
        }
    }

    private void scheduleAndAddToMap(SchedulerEventInfo event) {
        long ts = System.currentTimeMillis();
        SchedulerEventMetaData eventMd = getSchedulerEventMetaData(event);
        scheduleNextEvent(ts, event, eventMd);
        eventsMetaData.put(event.getId(), eventMd);
    }

    private void sendSchedulerEvent(TenantId tenantId, SchedulerEventId eventId, boolean added, boolean updated, boolean deleted) {
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
