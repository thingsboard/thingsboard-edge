/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.mail.internet.MimeMessage;
import java.util.*;

@Service
@Slf4j
public class DefaultMailService implements MailService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String MAIL_PROP = "mail.";
    public static final String TARGET_EMAIL = "targetEmail";
    public static final String UTF_8 = "UTF-8";

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

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

        Map<String, Object> model = new HashMap<String, Object>();
        model.put(TARGET_EMAIL, email);

        String message = MailTemplates.body(mailTemplates, MailTemplates.TEST, model);

        sendMail(testMailSender, mailFrom, email, subject, message);
    }

    @Override
    public void sendActivationEmail(TenantId tenantId, String activationLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACTIVATION);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("activationLink", activationLink);
        model.put(TARGET_EMAIL, email);

        String message = MailTemplates.body(mailTemplates, MailTemplates.ACTIVATION, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = MailTemplates.body(mailTemplates, MailTemplates.ACCOUNT_ACTIVATED, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.RESET_PASSWORD);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("passwordResetLink", passwordResetLink);
        model.put(TARGET_EMAIL, email);

        String message = MailTemplates.body(mailTemplates, MailTemplates.RESET_PASSWORD, model);

        sendMail(tenantId, email, subject, message);
    }

    @Override
    public void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException {

        JsonNode mailTemplates = getConfig(tenantId, "mailTemplates");
        String subject = MailTemplates.subject(mailTemplates, MailTemplates.PASSWORD_WAS_RESET);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        String message = MailTemplates.body(mailTemplates, MailTemplates.PASSWORD_WAS_RESET, model);

        sendMail(tenantId, email, subject, message);
    }

    private void sendMail(TenantId tenantId, String email,
                          String subject, String message) throws ThingsboardException {
        JsonNode jsonConfig = getConfig(tenantId, "mail");
        JavaMailSenderImpl mailSender = createMailSender(jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");
        sendMail(mailSender, mailFrom, email, subject, message);
    }

    @Override
    public void send(TenantId tenantId, String from, String to, String cc, String bcc, String subject, String body) throws ThingsboardException {
        JsonNode jsonConfig = getConfig(tenantId, "mail");
        JavaMailSenderImpl mailSender = createMailSender(jsonConfig);
        String mailFrom = getStringValue(jsonConfig, "mailFrom");
        try {
            MimeMessage mailMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMsg, "UTF-8");
            helper.setFrom(StringUtils.isBlank(from) ? mailFrom : from);
            helper.setTo(to.split("\\s*,\\s*"));
            if (!StringUtils.isBlank(cc)) {
                helper.setCc(cc.split("\\s*,\\s*"));
            }
            if (!StringUtils.isBlank(bcc)) {
                helper.setBcc(bcc.split("\\s*,\\s*"));
            }
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMail(JavaMailSenderImpl mailSender,
                          String mailFrom, String email,
                          String subject, String message) throws ThingsboardException {
        try {
            MimeMessage mimeMsg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw handleException(e);
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
        String enableTls = getStringValue(jsonConfig, "enableTls");
        if (StringUtils.isEmpty(enableTls)) {
            enableTls = "false";
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
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

    private JsonNode getConfig(TenantId tenantId, String key) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            if (tenantId != null && !tenantId.isNullUid()) {
                String jsonString = getEntityAttributeValue(tenantId, key);
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
                AdminSettings settings = adminSettingsService.findAdminSettingsByKey(key);
                if (settings != null) {
                    jsonConfig = settings.getJsonValue();
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get mail configuration. Settings not found!");
            }
            return jsonConfig;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String getEntityAttributeValue(EntityId entityId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                attributesService.find(entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    protected ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        return new ThingsboardException(String.format("Unable to send mail: %s", message),
                ThingsboardErrorCode.GENERAL);
    }

}
