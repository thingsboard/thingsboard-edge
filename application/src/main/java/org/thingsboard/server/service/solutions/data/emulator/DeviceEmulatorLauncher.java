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
package org.thingsboard.server.service.solutions.data.emulator;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
public class DeviceEmulatorLauncher extends AbstractEmulatorLauncher<Device> {

    @Builder
    public DeviceEmulatorLauncher(Device entity, EmulatorDefinition emulatorDefinition, ExecutorService oldTelemetryExecutor, TbClusterService tbClusterService,
                                  PartitionService partitionService,
                                  TbQueueProducerProvider tbQueueProducerProvider,
                                  TbServiceInfoProvider serviceInfoProvider,
                                  TelemetrySubscriptionService tsSubService) throws Exception {
        super(entity, emulatorDefinition, oldTelemetryExecutor, tbClusterService, partitionService, tbQueueProducerProvider, serviceInfoProvider, tsSubService);
    }

    @Override
    protected void postProcessEntity(Device entity) {
        if (this.emulatorDefinition.getActivityPeriodInMillis() > 0) {
            tsSubService.saveAttrAndNotify(entity.getTenantId(), entity.getId(), DataConstants.SERVER_SCOPE,
                    DefaultDeviceStateService.INACTIVITY_TIMEOUT, this.emulatorDefinition.getActivityPeriodInMillis(), new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void unused) {
                            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, entity.getTenantId(), entity.getId());
                            UUID sessionId = UUID.randomUUID();
                            TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                                    .setSessionInfo(TransportProtos.SessionInfoProto.newBuilder()
                                            .setSessionIdMSB(sessionId.getMostSignificantBits())
                                            .setSessionIdLSB(sessionId.getLeastSignificantBits())
                                            .setDeviceIdMSB(entity.getId().getId().getMostSignificantBits())
                                            .setDeviceIdLSB(entity.getId().getId().getLeastSignificantBits())
                                            .setDeviceProfileIdMSB(entity.getId().getId().getMostSignificantBits())
                                            .setDeviceProfileIdLSB(entity.getId().getId().getLeastSignificantBits())
                                            .setDeviceName(entity.getName())
                                            .setDeviceType(entity.getType())
                                            .setTenantIdMSB(entity.getTenantId().getId().getMostSignificantBits())
                                            .setTenantIdLSB(entity.getTenantId().getId().getLeastSignificantBits())
                                            .setNodeId(serviceInfoProvider.getServiceId())
                                            .build())
                                    .setSubscriptionInfo(TransportProtos.SubscriptionInfoProto.newBuilder().setLastActivityTime(System.currentTimeMillis()).build())
                                    .build();
                            tbQueueProducerProvider.getTbCoreMsgProducer().send(tpi,
                                    new TbProtoQueueMsg<>(entity.getUuidId(),
                                            TransportProtos.ToCoreMsg.newBuilder().setToDeviceActorMsg(msg).build()), EMPTY_CALLBACK
                            );
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                        }
                    });
        }
    }
}
