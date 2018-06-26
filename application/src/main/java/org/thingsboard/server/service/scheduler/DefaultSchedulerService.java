/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ClusterRoutingService routingService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private ActorService actorService;

    @Autowired
    private ClusterRpcService clusterRpcService;

    private final ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<TenantId, List<SchedulerEventId>> tenantEvents;
    private ConcurrentMap<SchedulerEventId, SchedulerEventMetaData> eventsMetaData;
    private ListeningScheduledExecutorService queueExecutor;

    @PostConstruct
    public void init() {
        tenantEvents = new ConcurrentHashMap<>();
        eventsMetaData = new ConcurrentHashMap<>();
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        queueExecutor.submit(this::initStateFromDB);
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public void onSchedulerEventAdded(SchedulerEventInfo event) {
        queueExecutor.submit(() -> onSchedulerEventAddedSync(event));
    }

    @Override
    public void onSchedulerEventUpdated(SchedulerEventInfo event) {
        queueExecutor.submit(() -> onSchedulerEventUpdatedSync(event));
    }

    @Override
    public void onSchedulerEventDeleted(SchedulerEventInfo event) {
        queueExecutor.submit(() -> onSchedulerEventDeletedSync(event.getTenantId(), event.getId()));
    }

    @Override
    public void onRemoteMsg(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.SchedulerServiceMsgProto proto;
        try {
            proto = ClusterAPIProtos.SchedulerServiceMsgProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        SchedulerEventId eventId = new SchedulerEventId(new UUID(proto.getEventIdMSB(), proto.getEventIdLSB()));
        if (proto.getDeleted()) {
            queueExecutor.submit(() -> onSchedulerEventDeletedSync(tenantId, eventId));
        } else {
            SchedulerEventInfo eventInfo = schedulerEventService.findSchedulerEventInfoById(eventId);
            if (eventInfo != null) {
                if (proto.getAdded()) {
                    onSchedulerEventAdded(eventInfo);
                } else if (proto.getUpdated()) {
                    onSchedulerEventUpdated(eventInfo);
                }
            }
        }
    }

    @Override
    public void onClusterUpdate() {
        queueExecutor.submit(this::onClusterUpdateSync);
    }

    private void initStateFromDB() {
        log.info("Initializing scheduler service...");
        long ts = System.currentTimeMillis();
        List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
        for (Tenant tenant : tenants) {
            if (routingService.resolveById(tenant.getId()).isPresent()) {
                break;
            }
            addEventsForTenant(ts, tenant);
        }
        log.info("Scheduler service initialized.");
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
            md.setNextTaskFuture(queueExecutor.schedule(() -> processEvent(event.getId()), eventDelay, TimeUnit.MILLISECONDS));
        }
    }

    private SchedulerEventMetaData getSchedulerEventMetaData(SchedulerEventInfo event) {
        JsonNode node = event.getSchedule();
        long startTime = node.get("startTime").asLong();
        JsonNode repeatNode = node.get("repeat");
        SchedulerRepeat repeat = null;
        if (repeatNode != null) {
            try {
                repeat = mapper.treeToValue(repeatNode, SchedulerRepeat.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to read scheduler config", e);
            }
        }
        return new SchedulerEventMetaData(event, startTime, repeat);
    }

    private void processEvent(SchedulerEventId eventId) {
        SchedulerEventMetaData md = eventsMetaData.get(eventId);
        if (md != null) {
            SchedulerEvent event = schedulerEventService.findSchedulerEventById(eventId);
            if (event != null) {
                try {
                    HashMap<String, String> metaData = new HashMap<>();
                    metaData.put("customerId", event.getCustomerId().getId().toString());
                    metaData.put("eventName", event.getName());
                    if (event.getAdditionalInfo() != null) {
                        metaData.put("additionalInfo", mapper.writeValueAsString(event.getAdditionalInfo()));
                    }
                    TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), event.getType(), eventId, new TbMsgMetaData(metaData),
                            TbMsgDataType.JSON, mapper.writeValueAsString(event.getConfiguration()), null, null, 0L);
                    actorService.onMsg(new ServiceToRuleEngineMsg(event.getTenantId(), tbMsg));
                } catch (JsonProcessingException e) {
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

    private void onSchedulerEventAddedSync(SchedulerEventInfo event) {
        Optional<ServerAddress> address = routingService.resolveById(event.getTenantId());
        if (!address.isPresent()) {
            if (!eventsMetaData.containsKey(event.getId())) {
                scheduleAndAddToMap(event);
            } else {
                onSchedulerEventUpdated(event);
            }
        } else {
            sendSchedulerEvent(event.getTenantId(), event.getId(), address.get(), true, false, false);
        }
    }

    private void onSchedulerEventUpdatedSync(SchedulerEventInfo event) {
        Optional<ServerAddress> address = routingService.resolveById(event.getTenantId());
        if (!address.isPresent()) {
            SchedulerEventMetaData oldMd = eventsMetaData.remove(event.getId());
            if (oldMd != null && oldMd.getNextTaskFuture() != null) {
                oldMd.getNextTaskFuture().cancel(false);
            }
            scheduleAndAddToMap(event);
        } else {
            sendSchedulerEvent(event.getTenantId(), event.getId(), address.get(), false, true, false);
        }
    }

    private void onSchedulerEventDeletedSync(TenantId tenantId, SchedulerEventId eventId) {
        Optional<ServerAddress> address = routingService.resolveById(tenantId);
        if (!address.isPresent()) {
            SchedulerEventMetaData oldMd = eventsMetaData.remove(eventId);
            if (oldMd != null && oldMd.getNextTaskFuture() != null) {
                oldMd.getNextTaskFuture().cancel(false);
            }
        } else {
            sendSchedulerEvent(tenantId, eventId, address.get(), false, false, true);
        }
    }

    private void scheduleAndAddToMap(SchedulerEventInfo event) {
        long ts = System.currentTimeMillis();
        SchedulerEventMetaData eventMd = getSchedulerEventMetaData(event);
        scheduleNextEvent(ts, event, eventMd);
        eventsMetaData.put(event.getId(), eventMd);
    }

    private void onClusterUpdateSync() {
        long ts = System.currentTimeMillis();
        List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
        for (Tenant tenant : tenants) {
            if (routingService.resolveById(tenant.getId()).isPresent()) {
                List<SchedulerEventId> eventsIds = tenantEvents.remove(tenant.getId());
                if (eventsIds != null) {
                    for (SchedulerEventId eventId : eventsIds) {
                        SchedulerEventMetaData md = eventsMetaData.remove(eventId);
                        if (md != null && md.getNextTaskFuture() != null) {
                            md.getNextTaskFuture().cancel(false);
                        }
                    }
                }
                break;
            } else {
                List<SchedulerEventId> eventIds = tenantEvents.get(tenant.getId());
                if (eventIds == null) {
                    addEventsForTenant(ts, tenant);
                }
            }
        }
    }

    private void sendSchedulerEvent(TenantId tenantId, SchedulerEventId eventId, ServerAddress address, boolean added, boolean updated, boolean deleted) {
        log.trace("[{}][{}] Device is monitored on other server: {}", tenantId, eventId, address);
        ClusterAPIProtos.SchedulerServiceMsgProto.Builder builder = ClusterAPIProtos.SchedulerServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setEventIdMSB(eventId.getId().getMostSignificantBits());
        builder.setEventIdLSB(eventId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        clusterRpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_SCHEDULER_SERVICE_MESSAGE, builder.build().toByteArray());
    }
}
