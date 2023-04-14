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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class DefaultMailService implements MailService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String MAIL_PROP = "mail.";
    public static final String TARGET_EMAIL = "targetEmail";
    public static final String UTF_8 = "UTF-8";

    private final AdminSettingsService adminSettingsService;
    private final AttributesService attributesService;
    private final BlobEntityService blobEntityService;
    private final TbApiUsageReportClient apiUsageClient;

    private static final long DEFAULT_TIMEOUT = 10_000;

    @Lazy
    @Autowired
    private TbApiUsageStateService apiUsageStateService;

    @Value("${actors.rule.allow_system_mail_service}")
    private boolean allowSystemMailService;

    @Autowired
    private MailExecutorService mailExecutorService;

    @Autowired
    private PasswordResetExecutorService passwordResetExecutorService;

    public DefaultMailService(AdminSettingsService adminSettingsService, AttributesService attributesService, BlobEntityService blobEntityService, TbApiUsageReportClient apiUsageClient) {
        this.adminSettingsService = adminSettingsService;
        this.attributesService = attributesService;
        this.blobEntityService = blobEntityService;
        this.apiUsageClient = apiUsageClient;
    }

    @Override
    public void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException {
        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendTestMail(TenantId tenantId, JsonNode jsonConfig, String email) throws ThingsboardException {
        JavaMailSenderImpl testMailSender = createMailSender(jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.TEST);

        Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.TEST, model);

        sendMail(testMailSender, mailFrom, email, subject, message, getTimeout(jsonConfig));
    }

    @Override
    public void sendActivationEmail(TenantId tenantId, String activationLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACTIVATION);

        Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACTIVATION, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.RESET_PASSWORD);

        Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.RESET_PASSWORD, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmailAsync(TenantId tenantId, String passwordResetLink, String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(tenantId, passwordResetLink, email);
            } catch (ThingsboardException e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.PASSWORD_WAS_RESET);

        Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.PASSWORD_WAS_RESET, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendUserActivatedEmail(TenantId tenantId, String userFullName, String userEmail, String targetEmail) throws ThingsboardException {
        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.USER_ACTIVATED);

        Map<String, Object> model = new HashMap<>();
        model.put("userFullName", userFullName);
        model.put("userEmail", userEmail);
        model.put(TARGET_EMAIL, targetEmail);

        String message = body(mailTemplates, MailTemplates.USER_ACTIVATED, model);

        sendMail(tenantId, targetEmail, subject, message);
    }

    @Override
    public void sendUserRegisteredEmail(TenantId tenantId, String userFullName, String userEmail, String targetEmail) throws ThingsboardException {
        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.USER_REGISTERED);

        Map<String, Object> model = new HashMap<>();
        model.put("userFullName", userFullName);
        model.put("userEmail", userEmail);
        model.put(TARGET_EMAIL, targetEmail);

        String message = body(mailTemplates, MailTemplates.USER_REGISTERED, model);

        sendMail(tenantId, targetEmail, subject, message);
    }

    private void sendMail(TenantId tenantId, String email,
                          String subject, String message) throws ThingsboardException {
        JsonNode jsonConfig = getConfig(tenantId, "mail");
        JavaMailSenderImpl mailSender = createMailSender(jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");
        sendMail(mailSender, mailFrom, email, subject, message, getTimeout(jsonConfig));
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException {
        ConfigEntry configEntry = getConfig(tenantId, "mail", allowSystemMailService);
        JsonNode jsonConfig = configEntry.jsonConfig;
        JavaMailSenderImpl mailSender = createMailSender(jsonConfig);
        sendMail(tenantId, customerId, tbEmail, mailSender, false, getTimeout(jsonConfig));
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, long timeout, JavaMailSender javaMailSender) throws ThingsboardException {
        sendMail(tenantId, customerId, tbEmail, javaMailSender, true, timeout);
    }

    private void sendMail(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, boolean externalMailSender, long timeout) throws ThingsboardException {
        ConfigEntry configEntry = getConfig(tenantId, "mail", true);
        JsonNode jsonConfig = configEntry.jsonConfig;
        if (externalMailSender || !configEntry.isSystem || apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            String mailFrom = getStringValue(jsonConfig, "mailFrom");
            try {
                MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty())
                        || (tbEmail.getAttachments() != null && !tbEmail.getAttachments().isEmpty());
                MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
                helper.setFrom(StringUtils.isBlank(tbEmail.getFrom()) ? mailFrom : tbEmail.getFrom());
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
                        byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                if (!externalMailSender && configEntry.isSystem) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
                }
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendAccountLockoutEmail(TenantId tenantId, String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException {
        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACCOUNT_LOCKOUT);

        Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        String message = body(mailTemplates, MailTemplates.ACCOUNT_LOCKOUT, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendTwoFaVerificationEmail(TenantId tenantId, String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException {
        sendTemplateEmail(tenantId, email, MailTemplates.TWO_FA_VERIFICATION, Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
        ));
    }

    @Override
    public void sendApiFeatureStateEmail(TenantId tenantId, ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws ThingsboardException {
        JsonNode mailTemplates = getConfig(null, "mailTemplates");
        String subject = null;

        Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        String message = null;

        switch (stateValue) {
            case ENABLED:
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_ENABLED, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_ENABLED);
                break;
            case WARNING:
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(recordState));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_WARNING, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_WARNING);
                break;
            case DISABLED:
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(recordState));
                message = body(mailTemplates, MailTemplates.API_USAGE_STATE_DISABLED, model);
                subject = MailTemplates.subject(mailTemplates, MailTemplates.API_USAGE_STATE_DISABLED);
                break;
        }
        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        JsonNode jsonConfig = getConfig(tenantId, "mail");
        JavaMailSenderImpl mailSender = createMailSender(jsonConfig);
        mailSender.testConnection();
    }

    private void sendTemplateEmail(TenantId tenantId, String email, String template, Map<String, Object> templateModel) throws ThingsboardException {
        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, template);
        String message = body(mailTemplates, template, templateModel);
        sendMail(tenantId, email, subject, message);
    }

    private String toEnabledValueLabel(ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "save";
            case TRANSPORT:
                return "receive";
            case JS:
                return "invoke";
            case RE:
                return "process";
            case EMAIL:
            case SMS:
                return "send";
            case ALARM:
                return "create";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String toDisabledValueLabel(ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "saved";
            case TRANSPORT:
                return "received";
            case JS:
                return "invoked";
            case RE:
                return "processed";
            case EMAIL:
            case SMS:
                return "sent";
            case ALARM:
                return "created";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String toWarningValueLabel(ApiUsageRecordState recordState) {
        String valueInM = recordState.getValueAsString();
        String thresholdInM = recordState.getThresholdAsString();
        switch (recordState.getKey()) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed data points";
            case TRANSPORT_MSG_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed messages";
            case JS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed JavaScript functions";
            case RE_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Email messages";
            case SMS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String toDisabledValueLabel(ApiUsageRecordState recordState) {
        switch (recordState.getKey()) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return recordState.getValueAsString() + " data points";
            case TRANSPORT_MSG_COUNT:
                return recordState.getValueAsString() + " messages";
            case JS_EXEC_COUNT:
                return "JavaScript functions " + recordState.getValueAsString() + " times";
            case RE_EXEC_COUNT:
                return recordState.getValueAsString() + " Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return recordState.getValueAsString() + " Email messages";
            case SMS_EXEC_COUNT:
                return recordState.getValueAsString() + " SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private void sendMail(JavaMailSenderImpl mailSender,
                          String mailFrom, String email,
                          String subject, String message,
                          long timeout) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);
            sendMailWithTimeout(mailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMailWithTimeout(JavaMailSender mailSender, MimeMessage msg, long timeout) {
        var submittedMail = mailExecutorService.submit(() -> mailSender.send(msg));
        try {
            submittedMail.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.debug("Error during mail submission", e);
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new RuntimeException(ExceptionUtils.getRootCause(e));
        }
    }

    private JavaMailSenderImpl createMailSender(JsonNode jsonConfig) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(getStringValue(jsonConfig, "smtpHost"));
        mailSender.setPort(parsePort(getStringValue(jsonConfig, "smtpPort")));
        mailSender.setUsername(getStringValue(jsonConfig, "username"));
        mailSender.setPassword(getStringValue(jsonConfig, "password"));
        mailSender.setJavaMailProperties(createJavaMailProperties(jsonConfig));
        return mailSender;
    }

    private Properties createJavaMailProperties(JsonNode jsonConfig) {
        Properties javaMailProperties = new Properties();
        String protocol = getStringValue(jsonConfig, "smtpProtocol");
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", getStringValue(jsonConfig, "smtpHost"));
        javaMailProperties.put(MAIL_PROP + protocol + ".port", getStringValue(jsonConfig, "smtpPort"));
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", getStringValue(jsonConfig, "timeout"));
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(getStringValue(jsonConfig, "username"))));
        boolean enableTls = false;
        if (jsonConfig.has("enableTls")) {
            if (jsonConfig.get("enableTls").isBoolean() && jsonConfig.get("enableTls").booleanValue()) {
                enableTls = true;
            } else if (jsonConfig.get("enableTls").isTextual()) {
                enableTls = "true".equalsIgnoreCase(jsonConfig.get("enableTls").asText());
            }
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
        if (enableTls && jsonConfig.has("tlsVersion") && !jsonConfig.get("tlsVersion").isNull()) {
            String tlsVersion = jsonConfig.get("tlsVersion").asText();
            if (StringUtils.isNoneEmpty(tlsVersion)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", tlsVersion);
            }
        }

        boolean enableProxy = jsonConfig.has("enableProxy") && jsonConfig.get("enableProxy").asBoolean();

        if (enableProxy) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", jsonConfig.get("proxyHost").asText());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", jsonConfig.get("proxyPort").asText());
            String proxyUser = jsonConfig.get("proxyUser").asText();
            if (StringUtils.isNoneEmpty(proxyUser)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", proxyUser);
            }
            String proxyPassword = jsonConfig.get("proxyPassword").asText();
            if (StringUtils.isNoneEmpty(proxyPassword)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", proxyPassword);
            }
        }

        return javaMailProperties;
    }

    private int parsePort(String strPort) {
        try {
            return Integer.valueOf(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }

    private String getStringValue(JsonNode jsonNode, String key) {
        if (jsonNode.has(key)) {
            return jsonNode.get(key).asText();
        } else {
            return "";
        }
    }

    private long getTimeout(JsonNode jsonConfig) {
        if (jsonConfig.has("timeout")) {
            return jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);
        } else {
            return DEFAULT_TIMEOUT;
        }
    }


    private JsonNode getConfig(TenantId tenantId, String key) throws ThingsboardException {
        return getConfig(tenantId, key, true).jsonConfig;
    }

    private ConfigEntry getConfig(TenantId tenantId, String key, boolean allowSystemMailService) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            boolean isSystem = false;
            if (tenantId != null && !tenantId.isNullUid()) {
                String jsonString = getEntityAttributeValue(tenantId, tenantId, key);
                if (!StringUtils.isEmpty(jsonString)) {
                    try {
                        jsonConfig = objectMapper.readTree(jsonString);
                    } catch (Exception e) {
                    }
                }
                if (jsonConfig != null) {
                    JsonNode useSystemMailSettingsNode = jsonConfig.get("useSystemMailSettings");
                    if (useSystemMailSettingsNode == null || useSystemMailSettingsNode.asBoolean()) {
                        jsonConfig = null;
                    }
                }
            }
            if (jsonConfig == null) {
                if (!allowSystemMailService) {
                    throw new RuntimeException("Access to System Mail Service is forbidden!");
                }
                AdminSettings settings = adminSettingsService.findAdminSettingsByKey(tenantId, key);
                if (settings != null) {
                    jsonConfig = settings.getJsonValue();
                    isSystem = true;
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get mail configuration. Settings not found!");
            }
            return new ConfigEntry(jsonConfig, isSystem);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String getEntityAttributeValue(TenantId tenantId, EntityId entityId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                attributesService.find(tenantId, entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    class ConfigEntry {

        JsonNode jsonConfig;
        boolean isSystem;

        ConfigEntry(JsonNode jsonConfig, boolean isSystem) {
            this.jsonConfig = jsonConfig;
            this.isSystem = isSystem;
        }

    }

    private String body(JsonNode mailTemplates, String template, Map<String, Object> model) throws ThingsboardException {
        try {
            return MailTemplates.body(mailTemplates, template, model);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send mail: {}", message);
        return new ThingsboardException(String.format("Unable to send mail: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}
