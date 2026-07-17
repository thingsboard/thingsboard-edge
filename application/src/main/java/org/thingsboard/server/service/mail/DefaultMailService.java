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
import com.google.common.util.concurrent.Futures;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.dao.cloud.CloudEventService;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * On the Edge the MailService is a thin client. Any send that would rely on admin-configured (tenant or
 * system) mail settings cannot be resolved on the Edge, because the mail settings are no longer synced to
 * the Edge. Instead of resolving config, rendering templates and opening SMTP locally, the Edge packages
 * the call into an {@link EdgeMailRequest} and enqueues a SEND_EMAIL cloud event; the Cloud resolves the
 * config, renders and transmits via its own SMTP. The only local send that remains is the rule-node
 * "own SMTP" path, where the caller supplies a fully-configured {@link JavaMailSender} with plaintext
 * credentials (no admin settings involved).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

    private final ScheduledExecutorService timeoutScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("mail-service-watchdog");

    private final MailSenderInternalExecutorService mailExecutorService;
    private final PasswordResetExecutorService passwordResetExecutorService;
    private final RateLimitService rateLimitService;
    private final CloudEventService cloudEventService;

    @Value("${mail.per_tenant_rate_limits:}")
    private String perTenantRateLimitConfig;

    @PreDestroy
    public void destroy() {
        timeoutScheduler.shutdownNow();
    }

    @Override
    public void updateMailConfiguration() {
        // Mail is sent from the Cloud on the Edge; there is no local mail configuration to update.
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.SEND_BASIC)
                .to(email).subject(subject).message(message).build());
    }

    @Override
    public void sendTestMail(JsonNode jsonConfig, String email) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.TEST_MAIL)
                .testConfig(jsonConfig).to(email).build());
    }

    @Override
    public void sendActivationEmail(String activationLink, long ttlMs, String email) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACTIVATION)
                .activationLink(activationLink).ttlMs(ttlMs).to(email).build());
    }

    @Override
    public void sendAccountActivatedEmail(String loginLink, String email) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACCOUNT_ACTIVATED)
                .loginLink(loginLink).to(email).build());
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, long ttlMs, String email) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.RESET_PASSWORD)
                .passwordResetLink(passwordResetLink).ttlMs(ttlMs).to(email).build());
    }

    @Override
    public void sendResetPasswordEmailAsync(String passwordResetLink, long ttlMs, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(passwordResetLink, ttlMs, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(String loginLink, String email) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.PASSWORD_WAS_RESET)
                .loginLink(loginLink).to(email).build());
    }

    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACCOUNT_LOCKOUT)
                .lockoutEmail(lockoutEmail).to(email).maxFailedLoginAttempts(maxFailedLoginAttempts).build());
    }

    @Override
    public void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.TWO_FA)
                .to(email).verificationCode(verificationCode).expirationTimeSeconds(expirationTimeSeconds).build());
    }

    @Override
    public void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        enqueue(TenantId.SYS_TENANT_ID, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.API_USAGE_STATE)
                .apiFeature(apiFeature).stateValue(stateValue).to(email).recordState(recordState).build());
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.SEND_TB_EMAIL)
                .tbEmail(tbEmail).build());
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws ThingsboardException {
        // Rule-node "own SMTP" path: the caller supplies a fully-configured sender with plaintext
        // credentials, so this is sent locally on the Edge without any admin config resolution.
        if (tenantId != null && !tenantId.isSysTenantId() && StringUtils.isNotEmpty(perTenantRateLimitConfig) &&
                !rateLimitService.checkRateLimit(LimitedApi.EMAILS, (Object) tenantId, perTenantRateLimitConfig)) {
            throw new RateLimitExceededException(LimitedApi.EMAILS);
        }
        try {
            MimeMessage mailMsg = javaMailSender.createMimeMessage();
            boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty());
            MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
            helper.setFrom(tbEmail.getFrom());
            helper.setTo(tbEmail.getTo().split("\\s*,\\s*"));
            if (!StringUtils.isBlank(tbEmail.getCc())) {
                helper.setCc(tbEmail.getCc().split("\\s*,\\s*"));
            }
            if (!StringUtils.isBlank(tbEmail.getBcc())) {
                helper.setBcc(tbEmail.getBcc().split("\\s*,\\s*"));
            }
            helper.setSubject(tbEmail.getSubject());
            helper.setText(tbEmail.getBody(), tbEmail.isHtml());

            if (multipart) {
                for (String imgId : tbEmail.getImages().keySet()) {
                    String imgValue = tbEmail.getImages().get(imgId);
                    String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                    byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                    String contentType = helper.getFileTypeMap().getContentType(imgId);
                    InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                    helper.addInline(imgId, iss, contentType);
                }
            }
            sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        // Mail is sent from the Cloud on the Edge; there is no local SMTP connection to test.
    }

    @Override
    public boolean isConfigured(TenantId tenantId) {
        // Mail sending is delegated to the Cloud, which owns the configuration.
        return true;
    }

    private void enqueue(TenantId tenantId, EdgeMailRequest request) throws ThingsboardException {
        try {
            cloudEventService.saveCloudEvent(tenantId, CloudEventType.TENANT, EdgeEventActionType.SEND_EMAIL,
                    tenantId, JacksonUtil.valueToTree(request));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMailWithTimeout(JavaMailSender mailSender, MimeMessage msg, long timeout) throws ThingsboardException {
        var submittedMail = Futures.withTimeout(
                mailExecutorService.submit(() -> mailSender.send(msg)),
                timeout, TimeUnit.MILLISECONDS, timeoutScheduler);
        try {
            submittedMail.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new ThingsboardException("Unable to send mail", ExceptionUtils.getRootCause(e), ThingsboardErrorCode.GENERAL);
        }
    }

    protected ThingsboardException handleException(Throwable exception) {
        if (exception instanceof ThingsboardException thingsboardException) {
            return thingsboardException;
        }
        if (exception instanceof NestedRuntimeException) {
            exception = ((NestedRuntimeException) exception).getMostSpecificCause();
        }
        log.warn("Unable to send mail: {}", exception.getMessage());
        return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
    }

}
