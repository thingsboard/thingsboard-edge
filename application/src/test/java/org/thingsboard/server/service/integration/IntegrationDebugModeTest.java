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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class IntegrationDebugModeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("c7bf4c85-923c-4688-a4b5-0f8a0feb7cd5"));
    private final IntegrationId INTEGRATION_ID = IntegrationId.fromString("c7bf4c85-923c-4688-a4b5-0f8a0feb7cd5");
    private final long DEFAULT_DEBUG_TIMEOUT = TimeUnit.MINUTES.toMillis(15);

    @Mock
    private IntegrationContext context;
    @Mock
    private TbIntegrationInitParams params;

    private TestIntegration integration;

    @BeforeEach
    public void setUp() {
        integration = new TestIntegration();
        given(params.getContext()).willReturn(context);
    }

    @Test
    public void givenDebugFailuresEvents_whenPersistSuccessDebug_thenVerifyDebugNotSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.failures());
        integrationInit(configuration);

        // WHEN
        integration.persistSuccess();

        // THEN
        then(context).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugFailuresEvents_whenPersistFailureDebug_thenVerifyDebugSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.failures());
        integrationInit(configuration);

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).should().saveEvent(any(), any());
    }

    @Test
    public void givenDebugAllUntilEvents_whenPersistFailureDebug_thenVerifyDebugSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.until(System.currentTimeMillis() + DEFAULT_DEBUG_TIMEOUT));
        integrationInit(configuration);

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).should().saveEvent(any(), any());
    }

    @Test
    public void givenDebugAllUntilEvents_whenPersistSuccessDebug_thenVerifyDebugSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.until(System.currentTimeMillis() + DEFAULT_DEBUG_TIMEOUT));
        integrationInit(configuration);

        // WHEN
        integration.persistSuccess();

        // THEN
        then(context).should().saveEvent(any(), any());
    }

    @Test
    public void givenDebugAllUntilEventsIsUp_whenPersistFailureDebug_thenVerifyDebugNotSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.until(System.currentTimeMillis() + DEFAULT_DEBUG_TIMEOUT));
        integrationInit(configuration);

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).should().saveEvent(any(), any());

        // GIVEN
        Mockito.clearInvocations(context);
        configuration.setDebugSettings(DebugSettings.off());

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugFailuresAndDebugAllUntilEventsIsUp_whenPersistSuccessDebug_thenVerifyDebugNotSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.failuresOrUntil(System.currentTimeMillis() + DEFAULT_DEBUG_TIMEOUT));
        integrationInit(configuration);

        // WHEN
        integration.persistSuccess();

        // THEN
        then(context).should().saveEvent(any(), any());

        // GIVEN
        Mockito.clearInvocations(context);
        configuration.setDebugSettings(DebugSettings.off());

        // WHEN
        integration.persistSuccess();

        // THEN
        then(context).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenDebugFailuresAndDebugAllUntilEventsIsUp_whenPersistFailureDebug_thenVerifyDebugSaved() throws Exception {
        // GIVEN
        Integration configuration = new Integration();
        configuration.setDebugSettings(DebugSettings.failuresOrUntil(System.currentTimeMillis() + DEFAULT_DEBUG_TIMEOUT));
        integrationInit(configuration);

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).should().saveEvent(any(), any());

        // GIVEN
        Mockito.clearInvocations(context);
        configuration.setDebugSettings(DebugSettings.failures());

        // WHEN
        integration.persistFailure();

        // THEN
        then(context).should().saveEvent(any(), any());
    }

    private void integrationInit(Integration configuration) throws Exception {
        configuration.setTenantId(TENANT_ID);
        configuration.setId(INTEGRATION_ID);
        configuration.setConfiguration(JacksonUtil.newObjectNode().set("metadata", null));
        given(params.getConfiguration()).willReturn(configuration);
        integration.init(params);
    }

    private static class TestIntegration extends AbstractIntegration {

        @Override
        public void destroy() {
        }

        @Override
        public void process(Object msg) {
        }

        public void persistSuccess() {
            persistDebug(context, "Uplink", ContentType.JSON, "testMsg", "OK", null);
        }

        public void persistFailure() {
            persistDebug(context, "Uplink", ContentType.JSON, "testMsg", "FAILURE", new RuntimeException());
        }
    }
}
