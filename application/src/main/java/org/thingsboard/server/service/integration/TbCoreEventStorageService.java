/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbCoreEventStorageService implements EventStorageService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final ActorSystemContext actorSystemContext;
    private final TelemetrySubscriptionService telemetrySubscriptionService;
    private final EventService eventService;

    @Override
    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        actorSystemContext.persistLifecycleEvent(tenantId, entityId, lcEvent, e);
    }

    @Override
    public void persistStatistics(TenantId tenantId, IntegrationId id, long ts, IntegrationStatistics statistics, ComponentLifecycleEvent currentState) {
        String serviceId = serviceInfoProvider.getServiceId();

        eventService.saveAsync(StatisticsEvent.builder()
                .tenantId(tenantId)
                .entityId(id.getId())
                .serviceId(serviceInfoProvider.getServiceId())
                .messagesProcessed(statistics.getMessagesProcessed())
                .errorsOccurred(statistics.getErrorsOccurred())
                .build());

        List<TsKvEntry> statsTs = new ArrayList<>();
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_messagesCount", statistics.getMessagesProcessed())));
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_errorsCount", statistics.getErrorsOccurred())));
        statsTs.add(new BasicTsKvEntry(ts, new StringDataEntry(serviceId + "_state", currentState != null ? currentState.name() : "N/A")));
        telemetrySubscriptionService.saveAndNotifyInternal(tenantId, id, statsTs, new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                log.trace("[{}] Persisted statistics telemetry: {}", id, statistics);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to persist statistics telemetry: {}", id, statistics, t);
            }
        });
    }

}
