/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

@Slf4j
public abstract class IntegrationActivityManager extends AbstractActivityManager<IntegrationActivityKey, Void> {

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;

    @Value("${integrations.activity.reporting_period:3000}")
    private long reportingPeriodMillis;

    @Value("${integrations.activity.reporting_strategy:LAST}")
    private ActivityStrategyType reportingStrategyType;

    @PostConstruct
    public void init() {
        super.init();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
    }

    @Override
    protected long getReportingPeriodMillis() {
        return reportingPeriodMillis;
    }

    @Override
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    @Override
    protected ActivityState<Void> updateState(IntegrationActivityKey key, ActivityState<Void> state) {
        return state;
    }

    @Override
    protected boolean hasExpired(long lastRecordedTime) {
        return (getCurrentTimeMillis() - reportingPeriodMillis) > lastRecordedTime;
    }

    @Override
    protected void onStateExpiry(IntegrationActivityKey key, Void currentMetadata) {
    }

    @Override
    protected void reportActivity(IntegrationActivityKey key, Void metadata, long timeToReport, ActivityReportCallback<IntegrationActivityKey> callback) {
        var tenantId = key.getTenantId();
        var deviceId = key.getDeviceId();
        log.debug("[{}][{}] Reporting activity state. Time to report: [{}].", tenantId.getId(), deviceId.getId(), timeToReport);
        TransportProtos.ToCoreMsg toCoreMsg = buildActivityMsg(tenantId, deviceId, timeToReport);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(deviceId.getId(), toCoreMsg), new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata msgAcknowledged) {
                callback.onSuccess(key, timeToReport);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(key, t);
            }
        });
    }

    private TransportProtos.ToCoreMsg buildActivityMsg(TenantId tenantId, DeviceId deviceId, long lastActivityTime) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        TransportProtos.DeviceActivityProto deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastActivityTime(lastActivityTime)
                .build();
        return TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(deviceActivityMsg)
                .build();
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
