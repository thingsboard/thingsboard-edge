// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import jakarta.activation.DataSource;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
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
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.report.ReportService;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * On the Edge the MailService is a thin client. Any send that would rely on
 * admin-configured (tenant or system) mail settings cannot be resolved on the Edge, because the
 * system encryption key/secrets never leave the Cloud. Instead of resolving config, rendering
 * templates and opening SMTP locally, the Edge packages the call into an {@link EdgeMailRequest}
 * and enqueues a SEND_EMAIL cloud event; the Cloud resolves the config, renders and transmits via
 * its own SMTP. The only local send that remains is the rule-node "own SMTP" path, where the caller
 * supplies a fully-configured {@link JavaMailSender} with plaintext credentials (no secrets, no
 * admin settings involved).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

    private final ScheduledExecutorService timeoutScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("mail-service-watchdog");

    private final BlobEntityService blobEntityService;
    private final MailSenderInternalExecutorService mailExecutorService;
    private final PasswordResetExecutorService passwordResetExecutorService;
    private final RateLimitService rateLimitService;
    private final ReportService reportService;
    private final CloudEventService cloudEventService;

    @Value("${mail.per_tenant_rate_limits:}")
    private String perTenantRateLimitConfig;

    @PreDestroy
    public void destroy() {
        timeoutScheduler.shutdownNow();
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.SEND_BASIC)
                .to(email).subject(subject).message(message).build());
    }

    @Override
    public void sendTestMail(TenantId tenantId, JsonNode jsonConfig, String email) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.TEST_MAIL)
                .testConfig(jsonConfig).to(email).build());
    }

    @Override
    public void sendActivationEmail(TenantId tenantId, String activationLink, long ttlMs, String email) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACTIVATION)
                .activationLink(activationLink).ttlMs(ttlMs).to(email).build());
    }

    @Override
    public void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACCOUNT_ACTIVATED)
                .loginLink(loginLink).to(email).build());
    }

    @Override
    public void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, long ttlMs, String email) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.RESET_PASSWORD)
                .passwordResetLink(passwordResetLink).ttlMs(ttlMs).to(email).build());
    }

    @Override
    public void sendResetPasswordEmailAsync(TenantId tenantId, String passwordResetLink, long ttlMs, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(tenantId, passwordResetLink, ttlMs, email);
            } catch (Exception e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.PASSWORD_WAS_RESET)
                .loginLink(loginLink).to(email).build());
    }

    @Override
    public void sendTwoFaVerificationEmail(TenantId tenantId, String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.TWO_FA)
                .to(email).verificationCode(verificationCode).expirationTimeSeconds(expirationTimeSeconds).build());
    }

    @Override
    public void sendAccountLockoutEmail(TenantId tenantId, String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.ACCOUNT_LOCKOUT)
                .lockoutEmail(lockoutEmail).to(email).maxFailedLoginAttempts(maxFailedLoginAttempts).build());
    }

    @Override
    public void sendApiFeatureStateEmail(TenantId tenantId, ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        enqueue(tenantId, EdgeMailRequest.builder()
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
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, long timeout, JavaMailSender javaMailSender) throws ThingsboardException {
        // Rule-node "own SMTP" path: the caller supplies a fully-configured sender with plaintext
        // credentials, so this is sent locally on the Edge without any admin config / secret resolution.
        if (tenantId != null && !tenantId.isSysTenantId() && StringUtils.isNotEmpty(perTenantRateLimitConfig) &&
                !rateLimitService.checkRateLimit(LimitedApi.EMAILS, (Object) tenantId, perTenantRateLimitConfig)) {
            throw new RateLimitExceededException(LimitedApi.EMAILS);
        }
        try {
            MimeMessage mailMsg = javaMailSender.createMimeMessage();
            boolean multipart = MapUtils.isNotEmpty(tbEmail.getImages())
                    || CollectionsUtil.isNotEmpty(tbEmail.getAttachments())
                    || CollectionsUtil.isNotEmpty(tbEmail.getReports());
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

            if (tbEmail.getAttachments() != null) {
                for (BlobEntityId blobEntityId : tbEmail.getAttachments()) {
                    BlobEntity blobEntity = blobEntityService.findBlobEntityById(tenantId, blobEntityId);
                    if (blobEntity != null) {
                        DataSource dataSource = new ByteArrayDataSource(blobEntity.getData().array(), blobEntity.getContentType());
                        helper.addAttachment(blobEntity.getName(), dataSource);
                    }
                }
            }
            if (tbEmail.getImages() != null) {
                for (String imgId : tbEmail.getImages().keySet()) {
                    String imgValue = tbEmail.getImages().get(imgId);
                    String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                    byte[] bytes = jakarta.xml.bind.DatatypeConverter.parseBase64Binary(value);
                    String contentType = helper.getFileTypeMap().getContentType(imgId);
                    InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                    helper.addInline(imgId, iss, contentType);
                }
            }
            if (tbEmail.getReports() != null) {
                for (ReportId reportId : tbEmail.getReports()) {
                    ReportData reportData = reportService.getReportDataById(tenantId, reportId);
                    if (reportData != null) {
                        DataSource dataSource = new ByteArrayDataSource(reportData.getData(), reportData.getContentType());
                        helper.addAttachment(reportData.getName(), dataSource);
                    }
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
                    tenantId, JacksonUtil.valueToTree(request), null);
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
