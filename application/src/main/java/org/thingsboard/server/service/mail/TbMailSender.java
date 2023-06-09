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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.mail.MailOauth2Provider;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.service.mail.RefreshTokenExpCheckService.AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS;

@Slf4j
public class TbMailSender extends JavaMailSenderImpl {

    private static final String MAIL_PROP = "mail.";
    private final TbMailContextComponent ctx;
    private final Lock lock;
    private final Boolean oauth2Enabled;
    private volatile String accessToken;
    private volatile long tokenExpires;
    private TenantId tenantId;

    public TbMailSender(TbMailContextComponent ctx, TenantId tenantId, JsonNode jsonConfig) {
        super();
        this.lock = new ReentrantLock();
        this.tokenExpires = 0L;
        this.ctx = ctx;
        this.tenantId = tenantId;
        this.oauth2Enabled = jsonConfig.has("enableOauth2") && jsonConfig.get("enableOauth2").asBoolean();

        setHost(jsonConfig.get("smtpHost").asText());
        setPort(parsePort(jsonConfig.get("smtpPort").asText()));
        setUsername(jsonConfig.get("username").asText());
        if (jsonConfig.has("password")) {
            setPassword(jsonConfig.get("password").asText());
        }
        setJavaMailProperties(createJavaMailProperties(jsonConfig));
    }

    @SneakyThrows
    @Override
    public void doSend(MimeMessage[] mimeMessages, @Nullable Object[] originalMessages) {
        if (oauth2Enabled && (System.currentTimeMillis() > tokenExpires)){
            refreshAccessToken(tenantId);
            setPassword(accessToken);
        }
        super.doSend(mimeMessages, originalMessages);
    }

    private Properties createJavaMailProperties(JsonNode jsonConfig) {
        Properties javaMailProperties = new Properties();
        String protocol = jsonConfig.get("smtpProtocol").asText();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
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

        if (oauth2Enabled) {
            javaMailProperties.put(MAIL_PROP + protocol + ".auth.mechanisms", "XOAUTH2");
        }
        return javaMailProperties;
    }

    public void refreshAccessToken(TenantId tenantId) throws Exception {
        lock.lock();
        try {
            if (System.currentTimeMillis() > tokenExpires) {
                AdminSettings settings;
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    settings = ctx.getAdminSettingsService().findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
                } else {
                    settings = getTenantAdminSettings(tenantId, "mail");
                }
                JsonNode jsonValue = settings.getJsonValue();
                String clientId = jsonValue.get("clientId").asText();
                String clientSecret = jsonValue.get("clientSecret").asText();
                String refreshToken = jsonValue.get("refreshToken").asText();
                String tokenUri = jsonValue.get("tokenUri").asText();
                String providerId = jsonValue.get("providerId").asText();

                TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                        new GenericUrl(tokenUri), refreshToken)
                        .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                        .execute();
                if (MailOauth2Provider.OFFICE_365.name().equals(providerId)) {
                    ((ObjectNode)jsonValue).put("refreshToken", tokenResponse.getRefreshToken());
                    ((ObjectNode)jsonValue).put("refreshTokenExpires", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                    if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                        ctx.getAdminSettingsService().saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
                    } else {
                        saveTenantAdminSettings(tenantId, settings);
                    }
                }
                accessToken = tokenResponse.getAccessToken();
                tokenExpires = System.currentTimeMillis() + (tokenResponse.getExpiresInSeconds().intValue() * 1000);
            }
        } catch (Exception e) {
            log.warn("Unable to retrieve access token: {}", e.getMessage());
            throw new ThingsboardException("Error while retrieving access token: " + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
        finally {
            lock.unlock();
        }
    }

    private AdminSettings getTenantAdminSettings(TenantId tenantId, String key) throws Exception {
        String jsonString = getEntityAttributeValue(tenantId, tenantId, key);
        JsonNode jsonValue = StringUtils.isEmpty(jsonString) ? null : JacksonUtil.toJsonNode(jsonString);
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey(key);
        adminSettings.setJsonValue(jsonValue);
        return adminSettings;
    }

    private void saveTenantAdminSettings(TenantId tenantId, AdminSettings adminSettings) throws Exception {
        String jsonString = adminSettings.getJsonValue() == null ? "" : JacksonUtil.toString(adminSettings.getJsonValue());
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(adminSettings.getKey(), jsonString), ts));
        ctx.getAttributesService().save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes).get();
    }

    private String getEntityAttributeValue(TenantId tenantId, EntityId entityId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                ctx.getAttributesService().find(tenantId, entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }


    private int parsePort(String strPort) {
        try {
            return Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }
}