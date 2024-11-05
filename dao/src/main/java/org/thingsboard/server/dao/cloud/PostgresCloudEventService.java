/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EntityGroupId;
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
            EdgeEventActionType.RELATION_REQUEST,
            EdgeEventActionType.WIDGET_BUNDLE_TYPES_REQUEST,
            EdgeEventActionType.ENTITY_VIEW_REQUEST,
            EdgeEventActionType.GROUP_ENTITIES_REQUEST,
            EdgeEventActionType.GROUP_PERMISSIONS_REQUEST
    );

    private DataValidator<CloudEvent> cloudEventValidator;
    private CloudEventDao cloudEventDao;
    private TsKvCloudEventDao tsKvCloudEventDao;

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody, EntityGroupId entityGroupId, Long queueStartTs) throws ExecutionException, InterruptedException {
        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody, entityGroupId, queueStartTs).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody, EntityGroupId entityGroupId, Long queueStartTs) {
        if (shouldAddEventToQueue(tenantId, cloudEventType, cloudEventAction, entityId, queueStartTs)) {
            CloudEvent cloudEvent = new CloudEvent(
                    tenantId,
                    cloudEventAction,
                    entityId != null ? entityId.getId() : null,
                    cloudEventType,
                    entityBody,
                    entityGroupId != null ? entityGroupId.getId() : null
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
    public void commit(boolean isTS) {
        throw new UnsupportedOperationException(METHOD_CANNOT_BE_USED_FOR_THIS_SERVICE);
    }

    @Override
    public void cleanupEvents(long ttl) {
        cloudEventDao.cleanupEvents(ttl);
        tsKvCloudEventDao.cleanupEvents(ttl);
    }

}
