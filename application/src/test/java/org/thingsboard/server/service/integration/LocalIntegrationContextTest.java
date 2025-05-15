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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class LocalIntegrationContextTest {

    LocalIntegrationContext localIntegrationContext;
    IntegrationContextComponent ctx;
    PlatformIntegrationService platformIntegrationService;
    Integration configuration;
    @Mock
    IntegrationCallback<Void> callback;
    @Mock
    Runnable runnable;

    @BeforeEach
    void setUp() {
        configuration = new Integration(new IntegrationId(UUID.randomUUID()));
        platformIntegrationService = mock(PlatformIntegrationService.class);
        ctx = mock(IntegrationContextComponent.class);
        willReturn(platformIntegrationService).given(ctx).getPlatformIntegrationService();

        var mqttClientRetransmissionSettings = new IntegrationMqttClientSettingsComponent();
        mqttClientRetransmissionSettings.setRetransmissionMaxAttempts(3);
        mqttClientRetransmissionSettings.setRetransmissionInitialDelayMillis(5000L);
        mqttClientRetransmissionSettings.setRetransmissionJitterFactor(0.15);

        localIntegrationContext = spy(new LocalIntegrationContext(ctx, configuration, mqttClientRetransmissionSettings));
    }

    @Test
    void testProcessUplinkDataDeviceRun() {
        DeviceUplinkDataProto uplinkData = mock(DeviceUplinkDataProto.class);
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, uplinkData, callback);
        localIntegrationContext.processUplinkData(uplinkData, callback);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataAssetRun() {
        AssetUplinkDataProto uplinkData = mock(AssetUplinkDataProto.class);
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, uplinkData, callback);
        localIntegrationContext.processUplinkData(uplinkData, callback);
        Mockito.verify(runnable).run();
    }

    @Test
    void testCreateEntityViewRun() {
        EntityViewDataProto uplinkData = mock(EntityViewDataProto.class);
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, uplinkData, callback);
        localIntegrationContext.createEntityView(uplinkData, callback);
        Mockito.verify(runnable).run();
    }

}
