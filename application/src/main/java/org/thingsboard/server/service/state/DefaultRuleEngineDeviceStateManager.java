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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
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
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@TbRuleEngineComponent
public class DefaultRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    public DefaultRuleEngineDeviceStateManager(
            TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService,
            Optional<DeviceStateService> deviceStateServiceOptional, TbClusterService clusterService
    ) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.deviceStateService = deviceStateServiceOptional;
        this.clusterService = clusterService;
    }

    @Getter
    private abstract static class ConnectivityEventInfo {

        private final TenantId tenantId;
        private final DeviceId deviceId;
        private final long eventTime;

        private ConnectivityEventInfo(TenantId tenantId, DeviceId deviceId, long eventTime) {
            this.tenantId = tenantId;
            this.deviceId = deviceId;
            this.eventTime = eventTime;
        }

        abstract void forwardToLocalService();

        abstract TransportProtos.ToCoreMsg toQueueMsg();

    }

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, connectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceConnect(tenantId, deviceId, connectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, activityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceActivity(tenantId, deviceId, activityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, disconnectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceDisconnect(tenantId, deviceId, disconnectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, inactivityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceInactivity(tenantId, deviceId, inactivityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
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
            }
        }, callback);
    }

    private void routeEvent(ConnectivityEventInfo eventInfo, TbCallback callback) {
        var tenantId = eventInfo.getTenantId();
        var deviceId = eventInfo.getDeviceId();
        long eventTime = eventInfo.getEventTime();

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            log.debug("[{}][{}] Forwarding device connectivity event to local service. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            try {
                eventInfo.forwardToLocalService();
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime, e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg msg = eventInfo.toQueueMsg();
            log.debug("[{}][{}] Sending device connectivity message to core. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), msg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}
