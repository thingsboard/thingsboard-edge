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
package org.thingsboard.server.transport.snmp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.service.ProtoTransportEntityService;
import org.thingsboard.server.transport.snmp.service.SnmpAuthService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportBalancingService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@TbSnmpTransportComponent
@Component
@Slf4j
@RequiredArgsConstructor
public class SnmpTransportContext extends TransportContext {
    @Getter
    private final SnmpTransportService snmpTransportService;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportService transportService;
    private final ProtoTransportEntityService protoEntityService;
    private final SnmpTransportBalancingService balancingService;
    @Getter
    private final SnmpAuthService snmpAuthService;

    private final Map<DeviceId, DeviceSessionContext> sessions = new ConcurrentHashMap<>();
    private final Collection<DeviceId> allSnmpDevicesIds = new ConcurrentLinkedDeque<>();

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void fetchDevicesAndEstablishSessions() {
        log.info("Initializing SNMP devices sessions");

        int batchIndex = 0;
        int batchSize = 512;
        boolean nextBatchExists = true;

        while (nextBatchExists) {
            TransportProtos.GetSnmpDevicesResponseMsg snmpDevicesResponse = protoEntityService.getSnmpDevicesIds(batchIndex, batchSize);
            snmpDevicesResponse.getIdsList().stream()
                    .map(id -> new DeviceId(UUID.fromString(id)))
                    .peek(allSnmpDevicesIds::add)
                    .filter(deviceId -> balancingService.isManagedByCurrentTransport(deviceId.getId()))
                    .map(protoEntityService::getDeviceById)
                    .forEach(device -> getExecutor().execute(() -> establishDeviceSession(device)));

            nextBatchExists = snmpDevicesResponse.getHasNextPage();
            batchIndex++;
        }

        log.debug("Found all SNMP devices ids: {}", allSnmpDevicesIds);
    }

    private void establishDeviceSession(Device device) {
        if (device == null) return;
        log.info("Establishing SNMP session for device {}", device.getId());

        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            return;
        }

        SnmpDeviceProfileTransportConfiguration profileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        DeviceSessionContext deviceSessionContext;
        try {
            deviceSessionContext = new DeviceSessionContext(
                    device, deviceProfile, credentials.getCredentialsId(),
                    profileTransportConfiguration, deviceTransportConfiguration, this
            );
            registerSessionMsgListener(deviceSessionContext);
        } catch (Exception e) {
            log.error("Failed to establish session for SNMP device {}: {}", device.getId(), e.toString());
            return;
        }
        sessions.put(device.getId(), deviceSessionContext);
        snmpTransportService.createQueryingTasks(deviceSessionContext);
        log.info("Established SNMP device session for device {}", device.getId());
    }

    private void updateDeviceSession(DeviceSessionContext sessionContext, Device device, DeviceProfile deviceProfile) {
        log.info("Updating SNMP session for device {}", device.getId());

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            destroyDeviceSession(sessionContext);
            return;
        }

        SnmpDeviceProfileTransportConfiguration newProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration newDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        try {
            if (!newProfileTransportConfiguration.equals(sessionContext.getProfileTransportConfiguration())) {
                sessionContext.setProfileTransportConfiguration(newProfileTransportConfiguration);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
                snmpTransportService.cancelQueryingTasks(sessionContext);
                snmpTransportService.createQueryingTasks(sessionContext);
            } else if (!newDeviceTransportConfiguration.equals(sessionContext.getDeviceTransportConfiguration())) {
                sessionContext.setDeviceTransportConfiguration(newDeviceTransportConfiguration);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
            } else {
                log.trace("Configuration of the device {} was not updated", device);
            }
        } catch (Exception e) {
            log.error("Failed to update session for SNMP device {}: {}", sessionContext.getDeviceId(), e.getMessage());
            destroyDeviceSession(sessionContext);
        }
    }

    private void destroyDeviceSession(DeviceSessionContext sessionContext) {
        if (sessionContext == null) return;
        log.info("Destroying SNMP device session for device {}", sessionContext.getDevice().getId());
        sessionContext.close();
        snmpAuthService.cleanUpSnmpAuthInfo(sessionContext);
        transportService.deregisterSession(sessionContext.getSessionInfo());
        snmpTransportService.cancelQueryingTasks(sessionContext);
        sessions.remove(sessionContext.getDeviceId());
        log.trace("Unregistered and removed session");
    }

    private void registerSessionMsgListener(DeviceSessionContext deviceSessionContext) {
        transportService.process(DeviceTransportType.SNMP,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceSessionContext.getToken()).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (msg.hasDeviceInfo()) {
                            SessionInfoProto sessionInfo = SessionInfoCreator.create(
                                    msg, SnmpTransportContext.this, UUID.randomUUID()
                            );

                            transportService.registerAsyncSession(sessionInfo, deviceSessionContext);
                            transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), TransportServiceCallback.EMPTY);
                            transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder().build(), TransportServiceCallback.EMPTY);

                            deviceSessionContext.setSessionInfo(sessionInfo);
                            deviceSessionContext.setDeviceInfo(msg.getDeviceInfo());
                            deviceSessionContext.setConnected(true);
                        } else {
                            log.warn("[{}] Failed to process device auth", deviceSessionContext.getDeviceId());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to process device auth: {}", deviceSessionContext.getDeviceId(), e);
                    }
                });
    }

    @EventListener(DeviceUpdatedEvent.class)
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        log.trace("Got creating or updating device event for device {}", device);
        DeviceTransportType transportType = Optional.ofNullable(device.getDeviceData().getTransportConfiguration())
                .map(DeviceTransportConfiguration::getType)
                .orElse(null);
        if (!allSnmpDevicesIds.contains(device.getId())) {
            if (transportType != DeviceTransportType.SNMP) {
                return;
            }
            allSnmpDevicesIds.add(device.getId());
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                establishDeviceSession(device);
            }
        } else {
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                DeviceSessionContext sessionContext = sessions.get(device.getId());
                if (transportType == DeviceTransportType.SNMP) {
                    if (sessionContext != null) {
                        updateDeviceSession(sessionContext, device, deviceProfileCache.get(device.getDeviceProfileId()));
                    } else {
                        establishDeviceSession(device);
                    }
                } else {
                    log.trace("Transport type was changed to {}", transportType);
                    destroyDeviceSession(sessionContext);
                }
            }
        }
    }

    public void onDeviceDeleted(DeviceSessionContext sessionContext) {
        destroyDeviceSession(sessionContext);
    }

    public void onDeviceProfileUpdated(DeviceProfile deviceProfile, DeviceSessionContext sessionContext) {
        updateDeviceSession(sessionContext, sessionContext.getDevice(), deviceProfile);
    }

    public void onSnmpTransportListChanged() {
        log.trace("SNMP transport list changed. Updating sessions");
        List<DeviceId> deleted = new LinkedList<>();
        for (DeviceId deviceId : allSnmpDevicesIds) {
            if (balancingService.isManagedByCurrentTransport(deviceId.getId())) {
                if (!sessions.containsKey(deviceId)) {
                    Device device = protoEntityService.getDeviceById(deviceId);
                    if (device != null) {
                        log.info("SNMP device {} is now managed by current transport node", deviceId);
                        establishDeviceSession(device);
                    } else {
                        deleted.add(deviceId);
                    }
                }
            } else {
                Optional.ofNullable(sessions.get(deviceId))
                        .ifPresent(sessionContext -> {
                            log.info("SNMP session for device {} is not managed by current transport node anymore", deviceId);
                            destroyDeviceSession(sessionContext);
                        });
            }
        }
        log.trace("Removing deleted SNMP devices: {}", deleted);
        allSnmpDevicesIds.removeAll(deleted);
    }


    public Collection<DeviceSessionContext> getSessions() {
        return sessions.values();
    }

}
