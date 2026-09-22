/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultMailServiceTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    private CloudEventService cloudEventService;
    private DefaultMailService mailService;

    @BeforeEach
    void setUp() {
        cloudEventService = mock(CloudEventService.class);
        mailService = new DefaultMailService(
                mock(MailSenderInternalExecutorService.class),
                mock(PasswordResetExecutorService.class),
                mock(RateLimitService.class),
                cloudEventService);
    }

    @AfterEach
    void tearDown() {
        mailService.destroy();
    }

    @Test
    void sendActivationEmail_delegatesToCloud() throws Exception {
        mailService.sendActivationEmail("http://activate", 3600000L, "user@acme.io");
        EdgeMailRequest request = captureRequest(TenantId.SYS_TENANT_ID);
        assertThat(request.getMethod()).isEqualTo(EdgeMailRequest.MailMethod.ACTIVATION);
        assertThat(request.getActivationLink()).isEqualTo("http://activate");
        assertThat(request.getTtlMs()).isEqualTo(3600000L);
        assertThat(request.getTo()).isEqualTo("user@acme.io");
    }

    @Test
    void sendTbEmail_delegatesToCloud() throws Exception {
        TbEmail tbEmail = TbEmail.builder().from("noreply@acme.io").to("user@acme.io")
                .subject("Alarm").body("<b>Alarm</b>").html(true).build();
        mailService.send(tenantId, null, tbEmail);
        EdgeMailRequest request = captureRequest(tenantId);
        assertThat(request.getMethod()).isEqualTo(EdgeMailRequest.MailMethod.SEND_TB_EMAIL);
        assertThat(request.getTbEmail()).isNotNull();
        assertThat(request.getTbEmail().getTo()).isEqualTo("user@acme.io");
        assertThat(request.getTbEmail().getSubject()).isEqualTo("Alarm");
    }

    @Test
    void twoFa_delegatesToCloud() throws Exception {
        mailService.sendTwoFaVerificationEmail("user@acme.io", "123456", 120);
        EdgeMailRequest request = captureRequest(TenantId.SYS_TENANT_ID);
        assertThat(request.getMethod()).isEqualTo(EdgeMailRequest.MailMethod.TWO_FA);
        assertThat(request.getVerificationCode()).isEqualTo("123456");
        assertThat(request.getExpirationTimeSeconds()).isEqualTo(120);
    }

    @Test
    void testMail_delegatesToCloud() throws Exception {
        JsonNode config = JacksonUtil.newObjectNode().put("mailFrom", "noreply@acme.io");
        mailService.sendTestMail(config, "user@acme.io");
        EdgeMailRequest request = captureRequest(TenantId.SYS_TENANT_ID);
        assertThat(request.getMethod()).isEqualTo(EdgeMailRequest.MailMethod.TEST_MAIL);
        assertThat(request.getTestConfig()).isEqualTo(config);
        assertThat(request.getTo()).isEqualTo("user@acme.io");
    }

    @Test
    void isConfigured_alwaysTrueOnEdge() {
        assertThat(mailService.isConfigured(tenantId)).isTrue();
    }

    private EdgeMailRequest captureRequest(TenantId expectedTenantId) throws Exception {
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(cloudEventService).saveCloudEvent(eq(expectedTenantId), eq(CloudEventType.TENANT),
                eq(EdgeEventActionType.SEND_EMAIL), eq(expectedTenantId), bodyCaptor.capture());
        return JacksonUtil.convertValue(bodyCaptor.getValue(), EdgeMailRequest.class);
    }

}
