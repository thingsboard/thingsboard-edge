// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.dao.cloud.CloudEventService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSmsServiceTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    private CloudEventService cloudEventService;
    private DefaultSmsService smsService;

    @BeforeEach
    void setUp() {
        cloudEventService = mock(CloudEventService.class);
        smsService = new DefaultSmsService(cloudEventService);
    }

    @Test
    public void sendSms_delegatesToCloud() throws Exception {
        smsService.sendSms(tenantId, null, new String[]{"+15551234567", "+15559876543"}, "Edge alert");
        EdgeSmsRequest request = captureRequest(tenantId);
        assertThat(request.getMethod()).isEqualTo(EdgeSmsRequest.SmsMethod.SEND_SMS);
        assertThat(request.getNumbers()).containsExactly("+15551234567", "+15559876543");
        assertThat(request.getMessage()).isEqualTo("Edge alert");
    }

    @Test
    public void sendTestSms_delegatesToCloud() throws Exception {
        TestSmsRequest testSmsRequest = new TestSmsRequest();
        testSmsRequest.setNumberTo("+15551234567");
        testSmsRequest.setMessage("Test");
        smsService.sendTestSms(testSmsRequest);
        EdgeSmsRequest request = captureRequest(TenantId.SYS_TENANT_ID);
        assertThat(request.getMethod()).isEqualTo(EdgeSmsRequest.SmsMethod.SEND_TEST_SMS);
        assertThat(request.getTestSmsRequest()).isNotNull();
        assertThat(request.getTestSmsRequest().getNumberTo()).isEqualTo("+15551234567");
        assertThat(request.getTestSmsRequest().getMessage()).isEqualTo("Test");
    }

    @Test
    public void isConfigured_alwaysTrueOnEdge() {
        assertThat(smsService.isConfigured(tenantId)).isTrue();
    }

    private EdgeSmsRequest captureRequest(TenantId expectedTenantId) throws Exception {
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(cloudEventService).saveCloudEvent(eq(expectedTenantId), eq(CloudEventType.TENANT),
                eq(EdgeEventActionType.SEND_SMS), eq(expectedTenantId), bodyCaptor.capture());
        return JacksonUtil.convertValue(bodyCaptor.getValue(), EdgeSmsRequest.class);
    }

}
