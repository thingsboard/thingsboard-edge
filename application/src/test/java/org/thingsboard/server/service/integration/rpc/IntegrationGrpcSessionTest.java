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
package org.thingsboard.server.service.integration.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.service.integration.IntegrationContextComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.util.UUID;

import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class IntegrationGrpcSessionTest {

    IntegrationGrpcSession integrationGrpcSession;
    IntegrationContextComponent ctx;
    PlatformIntegrationService platformIntegrationService;
    Integration configuration;
    Runnable runnable;
    UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        configuration = new Integration(new IntegrationId(UUID.randomUUID()));
        platformIntegrationService = mock(PlatformIntegrationService.class);
        ctx = mock(IntegrationContextComponent.class, Mockito.RETURNS_DEEP_STUBS);
        willReturn(platformIntegrationService).given(ctx).getPlatformIntegrationService();
        integrationGrpcSession = mock(IntegrationGrpcSession.class);
        ReflectionTestUtils.setField(integrationGrpcSession, "ctx", ctx);
        ReflectionTestUtils.setField(integrationGrpcSession, "configuration", configuration);
        ReflectionTestUtils.setField(integrationGrpcSession, "sessionId", sessionId);
        willCallRealMethod().given(integrationGrpcSession).processUplinkMsg(Mockito.any());
        runnable = mock(Runnable.class);
    }

    @Test
    void testProcessUplinkDataDeviceRun() {
        DeviceUplinkDataProto deviceUplinkData = mock(DeviceUplinkDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                        .addDeviceData(deviceUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, sessionId, deviceUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataAssetRun() {

        AssetUplinkDataProto assetUplinkData = mock(AssetUplinkDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addAssetData(assetUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, assetUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataEntityViewRun() {
        EntityViewDataProto entityViewUplinkData = mock(EntityViewDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addEntityViewData(entityViewUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, entityViewUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

}
