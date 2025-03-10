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
package org.thingsboard.server.service.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDeviceStateManager implements DeviceStateManager {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device connect event to local service. Connect time: [{}].", tenantId.getId(), deviceId.getId(), connectTime);
                    deviceStateService.onDeviceConnect(tenantId, deviceId, connectTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device connect message to core. Connect time: [{}].", tenantId.getId(), deviceId.getId(), connectTime);
                    var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastConnectTime(connectTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceConnectMsg(deviceConnectMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device activity event to local service. Activity time: [{}].", tenantId.getId(), deviceId.getId(), activityTime);
                    deviceStateService.onDeviceActivity(tenantId, deviceId, activityTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device activity message to core. Activity time: [{}].", tenantId.getId(), deviceId.getId(), activityTime);
                    var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastActivityTime(activityTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceActivityMsg(deviceActivityMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device disconnect event to local service. Disconnect time: [{}].", tenantId.getId(), deviceId.getId(), disconnectTime);
                    deviceStateService.onDeviceDisconnect(tenantId, deviceId, disconnectTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device disconnect message to core. Disconnect time: [{}].", tenantId.getId(), deviceId.getId(), disconnectTime);
                    var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastDisconnectTime(disconnectTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceDisconnectMsg(deviceDisconnectMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device inactivity event to local service. Inactivity time: [{}].", tenantId.getId(), deviceId.getId(), inactivityTime);
                    deviceStateService.onDeviceInactivity(tenantId, deviceId, inactivityTime);
                },
                () -> {
                    log.debug("[{}][{}] Sending device inactivity message to core. Inactivity time: [{}].", tenantId.getId(), deviceId.getId(), inactivityTime);
                    var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setLastInactivityTime(inactivityTime)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceInactivityMsg(deviceInactivityMsg)
                            .build();
                }, callback);
    }

    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout, TbCallback callback) {
        forwardToDeviceStateService(tenantId, deviceId,
                deviceStateService -> {
                    log.debug("[{}][{}] Forwarding device inactivity timeout update to local service. Updated inactivity timeout: [{}].", tenantId.getId(), deviceId.getId(), inactivityTimeout);
                    deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, inactivityTimeout);
                },
                () -> {
                    log.debug("[{}][{}] Sending device inactivity timeout update message to core. Updated inactivity timeout: [{}].", tenantId.getId(), deviceId.getId(), inactivityTimeout);
                    var deviceInactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                            .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                            .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                            .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                            .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                            .setInactivityTimeout(inactivityTimeout)
                            .build();
                    return TransportProtos.ToCoreMsg.newBuilder()
                            .setDeviceInactivityTimeoutUpdateMsg(deviceInactivityTimeoutUpdateMsg)
                            .build();
                }, callback);
    }

    private void forwardToDeviceStateService(
            TenantId tenantId, DeviceId deviceId,
            Consumer<DeviceStateService> toDeviceStateService,
            Supplier<TransportProtos.ToCoreMsg> toCore,
            TbCallback callback
    ) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            try {
                toDeviceStateService.accept(deviceStateService.get());
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event.", tenantId.getId(), deviceId.getId(), e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = toCore.get();
            clusterService.pushMsgToCore(tpi, deviceId.getId(), toCoreMsg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}
