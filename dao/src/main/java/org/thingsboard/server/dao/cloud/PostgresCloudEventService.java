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
package org.thingsboard.server.dao.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
public class PostgresCloudEventService implements CloudEventService {
    private static final String SKIPPING_ADDING_IT_IS_ALREADY_PRESENT =
            "{} Skipping adding of {} event because it's already present in db {} {}";
    private static final String METHOD_CANNOT_BE_USED_FOR_THIS_SERVICE = "Method cannot be used for this service";
    private static final List<EdgeEventActionType> CLOUD_EVENT_ACTION_WITHOUT_DUPLICATES = List.of(
            EdgeEventActionType.ATTRIBUTES_REQUEST,
            EdgeEventActionType.RELATION_REQUEST
    );

    private DataValidator<CloudEvent> cloudEventValidator;
    private CloudEventDao cloudEventDao;
    private TsKvCloudEventDao tsKvCloudEventDao;

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody, Long queueStartTs) throws ExecutionException, InterruptedException {
        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody, queueStartTs).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody, Long queueStartTs) {
        if (shouldAddEventToQueue(tenantId, cloudEventType, cloudEventAction, entityId, queueStartTs)) {
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
                                          EdgeEventActionType cloudEventAction, EntityId entityId, Long queueStartTs) {
        if (queueStartTs == null || queueStartTs <= 0 || !CLOUD_EVENT_ACTION_WITHOUT_DUPLICATES.contains(cloudEventAction)) {
            return true;
        }

        long countMsgsInQueue = cloudEventDao.countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(
                tenantId.getId(), entityId.getId(), cloudEventType, cloudEventAction, queueStartTs, System.currentTimeMillis()
        );

        if (countMsgsInQueue > 0) {
            log.info(SKIPPING_ADDING_IT_IS_ALREADY_PRESENT, tenantId, cloudEventAction, entityId, cloudEventType);
            return false;
        }

        return true;
    }

    @Override
    public ListenableFuture<Void> saveAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);

        return cloudEventDao.saveAsync(cloudEvent);
    }

    @Override
    public ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);

        return tsKvCloudEventDao.saveAsync(cloudEvent);
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return cloudEventDao.findCloudEvents(tenantId.getId(), seqIdStart, seqIdEnd, pageLink);
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return tsKvCloudEventDao.findCloudEvents(tenantId.getId(), seqIdStart, seqIdEnd, pageLink);
    }

    @Override
    public void unsubscribeConsumers() {
        throw new UnsupportedOperationException(METHOD_CANNOT_BE_USED_FOR_THIS_SERVICE);
    }

    @Override
    public void cleanupEvents(long ttl) {
        cloudEventDao.cleanupEvents(ttl);
        tsKvCloudEventDao.cleanupEvents(ttl);
    }

}
