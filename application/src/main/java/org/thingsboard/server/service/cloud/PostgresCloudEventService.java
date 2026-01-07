/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.cloud.TsKvCloudEventDao;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_START_TS_ATTR_KEY;

@Slf4j
@Service
@AllArgsConstructor
public class PostgresCloudEventService implements CloudEventService {

    private static final List<EdgeEventActionType> CLOUD_EVENT_ACTION_WITHOUT_DUPLICATES = List.of(
            EdgeEventActionType.ATTRIBUTES_REQUEST,
            EdgeEventActionType.RELATION_REQUEST,
            EdgeEventActionType.CALCULATED_FIELD_REQUEST
    );

    private final CloudStatsCounterService statsCounterService;
    private final AttributesService attributesService;
    private final CloudEventDao cloudEventDao;
    private final TsKvCloudEventDao tsKvCloudEventDao;
    private final DataValidator<CloudEvent> cloudEventValidator;

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody) throws ExecutionException, InterruptedException {
        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody) {
        if (shouldAddEventToQueue(tenantId, cloudEventType, cloudEventAction, entityId)) {
            CloudEvent cloudEvent = new CloudEvent(
                    tenantId,
                    cloudEventAction,
                    entityId != null ? entityId.getId() : null,
                    cloudEventType,
                    entityBody
            );
            return saveAsync(cloudEvent);
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private boolean shouldAddEventToQueue(TenantId tenantId, CloudEventType cloudEventType,
                                          EdgeEventActionType cloudEventAction, EntityId entityId) {
        Long queueStartTs = null;
        try {
            Optional<AttributeKvEntry> attributeKvEntry = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, QUEUE_START_TS_ATTR_KEY).get();
            if (attributeKvEntry.isPresent() && attributeKvEntry.get().getLongValue().isPresent()) {
                queueStartTs = attributeKvEntry.get().getLongValue().get();
            }
        } catch (Exception ignored) {}

        if (queueStartTs == null || queueStartTs <= 0 || !CLOUD_EVENT_ACTION_WITHOUT_DUPLICATES.contains(cloudEventAction)) {
            return true;
        }

        long countMsgsInQueue = cloudEventDao.countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(
                tenantId.getId(), entityId.getId(), cloudEventType, cloudEventAction, queueStartTs, System.currentTimeMillis());

        if (countMsgsInQueue > 0) {
            log.info("{} Skipping adding of {} event because it's already present in db {} {}", tenantId, cloudEventAction, entityId, cloudEventType);
            return false;
        }

        return true;
    }

    @Override
    public ListenableFuture<Void> saveAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);
        log.trace("Save cloud event {}", cloudEvent);
        return handleSaveResult(cloudEvent, cloudEventDao.saveAsync(cloudEvent));
    }

    @Override
    public ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);
        return handleSaveResult(cloudEvent, tsKvCloudEventDao.saveAsync(cloudEvent));
    }

    private ListenableFuture<Void> handleSaveResult(CloudEvent cloudEvent, ListenableFuture<Void> saveFuture) {
        Futures.addCallback(saveFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_ADDED, cloudEvent.getTenantId(), 1); ;
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to save cloud event", t);
            }
        }, MoreExecutors.directExecutor());

        return saveFuture;
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return cloudEventDao.findCloudEvents(tenantId.getId(), seqIdStart, seqIdEnd, pageLink);
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return tsKvCloudEventDao.findCloudEvents(tenantId.getId(), seqIdStart, seqIdEnd, pageLink);
    }

}
