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
package org.thingsboard.server.transport.mqtt.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GatewaySessionHandlerTest {

    @Mock
    private TransportService transportService;

    @Mock
    private DeviceSessionCtx deviceSessionCtx;

    @Mock
    private MqttTransportContext transportContext;

    private GatewaySessionHandler handler;

    @BeforeEach
    public void setup() {
        lenient().when(deviceSessionCtx.getSessionId()).thenReturn(UUID.randomUUID());
        lenient().doNothing().when(transportService).recordActivity(any());
        lenient().when(transportContext.getTransportService()).thenReturn(transportService);
        lenient().when(deviceSessionCtx.getContext()).thenReturn(transportContext);

        var deviceInfo = new TransportDeviceInfo();
        deviceInfo.setDeviceId(new DeviceId(UUID.randomUUID()));
        lenient().when(deviceSessionCtx.getDeviceInfo()).thenReturn(deviceInfo);
        handler = new GatewaySessionHandler(deviceSessionCtx, UUID.randomUUID(), true);
        lenient().when(handler.getNodeId()).thenReturn("nodeId");
    }

    @Test
    public void shouldRecordActivityWhenOnGatewayPing() throws Exception {
        // Given
        ConcurrentHashMap<String, GatewayDeviceSessionContext> devices = new ConcurrentHashMap<>();
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setDeviceId(new DeviceId(UUID.randomUUID()));
        deviceInfo.setTenantId(new TenantId(UUID.randomUUID()));
        deviceInfo.setCustomerId(new CustomerId(UUID.randomUUID()));
        deviceInfo.setDeviceName("device1");
        deviceInfo.setDeviceType("default");
        deviceInfo.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        deviceInfo.setAdditionalInfo("{\"gateway\": true, \"overwriteDeviceActivity\": true}");
        lenient().when(deviceSessionCtx.getDeviceInfo()).thenReturn(deviceInfo);
        GatewayDeviceSessionContext gatewayDeviceSessionContext = new GatewayDeviceSessionContext(handler, deviceInfo, null, null, transportService);
        devices.put("device1", gatewayDeviceSessionContext);
        lenient().when(handler.getNodeId()).thenReturn("nodeId");
        Field devicesField = AbstractGatewaySessionHandler.class.getDeclaredField("devices");
        devicesField.setAccessible(true);
        devicesField.set(handler, devices);

        // When
        handler.onGatewayPing();

        // Then
        verify(transportService).recordActivity(gatewayDeviceSessionContext.getSessionInfo());
    }

    @Test
    public void shouldNotRecordActivityWhenNoDevicesOnGatewayPing() throws Exception {
        // Given
        ConcurrentHashMap<String, GatewayDeviceSessionContext> devices = new ConcurrentHashMap<>();
        Field devicesField = AbstractGatewaySessionHandler.class.getDeclaredField("devices");
        devicesField.setAccessible(true);
        devicesField.set(handler, devices);

        // When
        handler.onGatewayPing();

        // Then
        verify(transportService, never()).recordActivity(any());
    }

    @Test
    public void givenGatewaySessionHandler_WhenCreateWeakMap_thenConcurrentReferenceHashMapClass() {
        GatewaySessionHandler gsh = mock(GatewaySessionHandler.class);
        willCallRealMethod().given(gsh).createWeakMap();

        assertThat(gsh.createWeakMap()).isInstanceOf(ConcurrentReferenceHashMap.class);
    }

}
