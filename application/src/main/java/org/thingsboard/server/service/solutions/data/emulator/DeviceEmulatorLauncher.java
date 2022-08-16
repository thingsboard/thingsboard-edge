/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.DeviceEmulatorDefinition;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class DeviceEmulatorLauncher {

    private static final TbQueueCallback EMPTY_CALLBACK = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {

        }

        @Override
        public void onFailure(Throwable t) {

        }
    };
    private final Device device;
    private final DeviceEmulatorDefinition deviceProfile;
    private final ExecutorService oldTelemetryExecutor;
    private final TbClusterService tbClusterService;
    private final PartitionService partitionService;
    private final TbQueueProducerProvider tbQueueProducerProvider;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsSubService;
    private final long publishFrequency;

    private final DeviceEmulator deviceEmulator;
    private ScheduledFuture<?> scheduledFuture;

    @Builder
    public DeviceEmulatorLauncher(Device device, DeviceEmulatorDefinition deviceProfile, ExecutorService oldTelemetryExecutor, TbClusterService tbClusterService,
                                  PartitionService partitionService,
                                  TbQueueProducerProvider tbQueueProducerProvider,
                                  TbServiceInfoProvider serviceInfoProvider,
                                  TelemetrySubscriptionService tsSubService) throws Exception {
        this.device = device;
        this.deviceProfile = deviceProfile;
        this.oldTelemetryExecutor = oldTelemetryExecutor;
        this.tbClusterService = tbClusterService;
        this.partitionService = partitionService;
        this.tbQueueProducerProvider = tbQueueProducerProvider;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tsSubService = tsSubService;
        this.publishFrequency = TimeUnit.SECONDS.toMillis(deviceProfile.getPublishFrequencyInSeconds());
        if (StringUtils.isEmpty(deviceProfile.getClazz())) {
            deviceEmulator = new BasicDeviceEmulator();
        } else {
            deviceEmulator = (DeviceEmulator) Class.forName(deviceProfile.getClazz()).getDeclaredConstructor().newInstance();
        }
        deviceEmulator.init(deviceProfile);
    }

    public void launch() {
        final long latestTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(deviceProfile.getPublishPeriodInDays()) - publishFrequency;
        oldTelemetryExecutor.submit(() -> {
            try {
                if (latestTs < (System.currentTimeMillis() - publishFrequency)) {
                    pushOldTelemetry(latestTs);
                }
                if (this.deviceProfile.getActivityPeriodInMillis() > 0) {
                    tsSubService.saveAttrAndNotify(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                            DefaultDeviceStateService.INACTIVITY_TIMEOUT, this.deviceProfile.getActivityPeriodInMillis(), new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable Void unused) {
                                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, device.getTenantId(), device.getId());
                                    UUID sessionId = UUID.randomUUID();
                                    TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                                            .setSessionInfo(TransportProtos.SessionInfoProto.newBuilder()
                                                    .setSessionIdMSB(sessionId.getMostSignificantBits())
                                                    .setSessionIdLSB(sessionId.getLeastSignificantBits())
                                                    .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                                                    .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                                                    .setDeviceProfileIdMSB(device.getId().getId().getMostSignificantBits())
                                                    .setDeviceProfileIdLSB(device.getId().getId().getLeastSignificantBits())
                                                    .setDeviceName(device.getName())
                                                    .setDeviceType(device.getType())
                                                    .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                                                    .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                                                    .setNodeId(serviceInfoProvider.getServiceId())
                                                    .build())
                                            .setSubscriptionInfo(TransportProtos.SubscriptionInfoProto.newBuilder().setLastActivityTime(System.currentTimeMillis()).build())
                                            .build();
                                    tbQueueProducerProvider.getTbCoreMsgProducer().send(tpi,
                                            new TbProtoQueueMsg<>(device.getUuidId(),
                                                    TransportProtos.ToCoreMsg.newBuilder().setToDeviceActorMsg(msg).build()), EMPTY_CALLBACK
                                    );
                                }

                                @Override
                                public void onFailure(Throwable throwable) {}
                            });
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to upload telemetry for device: ", device.getName(), e);
            }
        });
    }

    private void pushOldTelemetry(long latestTs) throws InterruptedException {
        for (long ts = latestTs; ts < System.currentTimeMillis(); ts += publishFrequency) {
            publishTelemetry(ts);
        }
    }

    private void publishTelemetry(long ts) throws InterruptedException {
        ObjectNode values = deviceEmulator.getValue(ts);
        publishTelemetry(ts, values);
        Thread.sleep(deviceProfile.getPublishPauseInMillis());
    }

    public void stop() {
        scheduledFuture.cancel(true);
    }

    private void publishTelemetry(long ts, ObjectNode value) {
        String msgData = JacksonUtil.toString(value);
        log.debug("[{}] Publishing telemetry: {}", device.getName(), msgData);
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("ts", Long.toString(ts));
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), device.getId(), md, TbMsgDataType.JSON, msgData);
        tbClusterService.pushMsgToRuleEngine(device.getTenantId(), device.getId(), tbMsg, null);
    }
}
