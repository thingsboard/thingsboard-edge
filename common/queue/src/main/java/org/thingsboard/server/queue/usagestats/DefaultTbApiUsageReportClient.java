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
package org.thingsboard.server.queue.usagestats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class DefaultTbApiUsageReportClient implements TbApiUsageReportClient {

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;
    @Value("${usage.stats.report.interval:10}")
    private int interval;

    @SuppressWarnings("unchecked")
    private final ConcurrentMap<TenantId, AtomicLong>[] values = new ConcurrentMap[ApiUsageRecordKey.values().length];
    private final PartitionService partitionService;
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgProducer;

    public DefaultTbApiUsageReportClient(PartitionService partitionService, SchedulerComponent scheduler, TbQueueProducerProvider producerProvider) {
        this.partitionService = partitionService;
        this.scheduler = scheduler;
        this.producerProvider = producerProvider;
    }

    @PostConstruct
    private void init() {
        if (enabled) {
            msgProducer = this.producerProvider.getTbUsageStatsMsgProducer();
            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                values[key.ordinal()] = new ConcurrentHashMap<>();
            }
            scheduler.scheduleWithFixedDelay(this::reportStats, new Random().nextInt(interval), interval, TimeUnit.SECONDS);
        }
    }

    private void reportStats() {
        try {
            ConcurrentMap<TenantId, ToUsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                values[key.ordinal()].forEach(((tenantId, atomicLong) -> {
                    long value = atomicLong.getAndSet(0);
                    if (value > 0) {
                        ToUsageStatsServiceMsg.Builder msgBuilder = report.computeIfAbsent(tenantId, id -> {
                            ToUsageStatsServiceMsg.Builder msg = ToUsageStatsServiceMsg.newBuilder();
                            msg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                            msg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
                            return msg;
                        });
                        msgBuilder.addValues(UsageStatsKVProto.newBuilder().setKey(key.name()).setValue(value).build());
                    }
                }));
            }

            report.forEach(((tenantId, builder) -> {
                //TODO: figure out how to minimize messages into the queue. Maybe group by 100s of messages?
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).newByTopic(msgProducer.getDefaultTopic());
                msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), builder.build()), null);
            }));
            if (!report.isEmpty()) {
                log.info("Report statistics for: {} tenants", report.size());
            }
        } catch (Exception e) {
            log.warn("Failed to report statistics: ", e);
        }
    }

    @Override
    public void report(TenantId tenantId, ApiUsageRecordKey key, long value) {
        if (enabled) {
            ConcurrentMap<TenantId, AtomicLong> map = values[key.ordinal()];
            AtomicLong atomicValue = map.computeIfAbsent(tenantId, id -> new AtomicLong());
            atomicValue.addAndGet(value);
        }
    }

    @Override
    public void report(TenantId tenantId, ApiUsageRecordKey key) {
        report(tenantId, key, 1L);
    }
}
