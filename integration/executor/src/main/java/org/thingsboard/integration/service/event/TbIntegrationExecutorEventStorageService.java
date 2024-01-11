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
package org.thingsboard.integration.service.event;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.EventUtil;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.integration.EventStorageService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TbIntegrationExecutorEventStorageService implements EventStorageService {

    private static final IntegrationCallback<Void> EMPTY_CALLBACK = new IntegrationCallback<>() {
        @Override
        public void onSuccess(Void msg) {

        }

        @Override
        public void onError(Throwable e) {

        }
    };
    private final TbServiceInfoProvider serviceInfoProvider;
    private final IntegrationApiService apiService;

    @Override
    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        TbEventSource eventSource;
        switch (entityId.getEntityType()) {
            case INTEGRATION:
                eventSource = TbEventSource.INTEGRATION;
                break;
            case CONVERTER:
                //We don't care is it up or down link converter during processing of the message on tb-core side.
                eventSource = TbEventSource.UPLINK_CONVERTER;
                break;
            case DEVICE:
                eventSource = TbEventSource.DEVICE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type: " + entityId.getEntityType());
        }

        var event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(serviceInfoProvider.getServiceId())
                .lcEventType(lcEvent.name());
        if (e != null) {
            event.success(false).error(EventUtil.toString(e));
        } else {
            event.success(true);
        }

        var builder = TbIntegrationEventProto.newBuilder().setSource(eventSource)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event.build())));
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setEventSourceIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEventSourceIdLSB(entityId.getId().getLeastSignificantBits());

        apiService.sendEventData(tenantId, entityId, builder.build(), EMPTY_CALLBACK);
    }

    @Override
    public void persistStatistics(TenantId tenantId, IntegrationId id, long ts, IntegrationStatistics statistics, ComponentLifecycleEvent currentState) {
        String serviceId = serviceInfoProvider.getServiceId();

        var builder = TbIntegrationEventProto.newBuilder()
                .setSource(TbEventSource.INTEGRATION)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(StatisticsEvent.builder()
                        .tenantId(tenantId)
                        .entityId(id.getId())
                        .serviceId(serviceInfoProvider.getServiceId())
                        .messagesProcessed(statistics.getMessagesProcessed())
                        .errorsOccurred(statistics.getErrorsOccurred())
                        .build())));
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setEventSourceIdMSB(id.getId().getMostSignificantBits());
        builder.setEventSourceIdLSB(id.getId().getLeastSignificantBits());

        apiService.sendEventData(tenantId, id, builder.build(), EMPTY_CALLBACK);


        List<TsKvEntry> statsTs = new ArrayList<>();
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_messagesCount", statistics.getMessagesProcessed())));
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_errorsCount", statistics.getErrorsOccurred())));
        statsTs.add(new BasicTsKvEntry(ts, new StringDataEntry(serviceId + "_state", currentState != null ? currentState.name() : "N/A")));

        apiService.sendTsData(tenantId, id, TbIntegrationTsDataProto.newBuilder()
                .setSource(TbEventSource.INTEGRATION)
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setEntityIdMSB(id.getId().getMostSignificantBits())
                .setEntityIdLSB(id.getId().getLeastSignificantBits())
                .addAllTsData(KvProtoUtil.tsToTsKvProtos(statsTs)).build(), new IntegrationCallback<>() {
            @Override
            public void onSuccess(Void msg) {
                log.trace("[{}] Pushed integration statistics telemetry: {}", id, statistics);
            }

            @Override
            public void onError(Throwable e) {
                log.warn("[{}] Failed to push integration statistics telemetry: {}", id, statistics, e);
            }
        });
    }
}
