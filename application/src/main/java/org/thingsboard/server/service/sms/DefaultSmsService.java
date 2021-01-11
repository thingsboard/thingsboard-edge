/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DefaultSmsService implements SmsService {

    @Value("${actors.rule.allow_system_sms_service}")
    private boolean allowSystemSmsService;

    private final SmsSenderFactory smsSenderFactory;
    private final AdminSettingsService adminSettingsService;
    private final AttributesService attributesService;
    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageClient;

    public DefaultSmsService(SmsSenderFactory smsSenderFactory, AdminSettingsService adminSettingsService, AttributesService attributesService,
                             TbApiUsageStateService apiUsageStateService, TbApiUsageReportClient apiUsageClient) {
        this.smsSenderFactory = smsSenderFactory;
        this.adminSettingsService = adminSettingsService;
        this.attributesService = attributesService;
        this.apiUsageStateService = apiUsageStateService;
        this.apiUsageClient = apiUsageClient;
    }

    @Override
    public void sendSms(TenantId tenantId, String[] numbersTo, String message) throws ThingsboardException {
       ConfigEntry configEntry = getConfig(tenantId, "sms", allowSystemSmsService);
       SmsProviderConfiguration configuration = JacksonUtil.convertValue(configEntry.jsonConfig, SmsProviderConfiguration.class);
       SmsSender smsSender = this.smsSenderFactory.createSmsSender(configuration);
       if (!configEntry.isSystem || apiUsageStateService.getApiUsageState(tenantId).isSmsSendEnabled()) {
            int smsCount = 0;
            try {
                for (String numberTo : numbersTo) {
                    smsCount += this.sendSms(smsSender, numberTo, message);
                }
            } finally {
                if (configEntry.isSystem && smsCount > 0) {
                    apiUsageClient.report(tenantId, ApiUsageRecordKey.SMS_EXEC_COUNT, smsCount);
                }
            }
        } else {
            throw new RuntimeException("SMS sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException {
        SmsSender testSmsSender;
        try {
            testSmsSender = this.smsSenderFactory.createSmsSender(testSmsRequest.getProviderConfiguration());
        } catch (Exception e) {
            throw handleException(e);
        }
        this.sendSms(testSmsSender, testSmsRequest.getNumberTo(), testSmsRequest.getMessage());
        testSmsSender.destroy();
    }

    private int sendSms(SmsSender smsSender, String numberTo, String message) throws ThingsboardException {
        try {
            return smsSender.sendSms(numberTo, message);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private ConfigEntry getConfig(TenantId tenantId, String key, boolean allowSystemSmsService) throws ThingsboardException {
        try {
            JsonNode jsonConfig = null;
            boolean isSystem = false;
            if (tenantId != null && !tenantId.isNullUid()) {
                String jsonString = getEntityAttributeValue(tenantId, tenantId, key);
                if (!StringUtils.isEmpty(jsonString)) {
                    try {
                        jsonConfig = JacksonUtil.fromString(jsonString, JsonNode.class);
                    } catch (Exception e) {
                    }
                }
                if (jsonConfig != null) {
                    JsonNode useSystemSmsSettingsNode = jsonConfig.get("useSystemSmsSettings");
                    if (useSystemSmsSettingsNode == null || useSystemSmsSettingsNode.asBoolean()) {
                        jsonConfig = null;
                    }
                }
            }
            if (jsonConfig == null) {
                if (!allowSystemSmsService) {
                    throw new RuntimeException("Access to System SMS Service is forbidden!");
                }
                AdminSettings settings = adminSettingsService.findAdminSettingsByKey(tenantId, key);
                if (settings != null) {
                    jsonConfig = settings.getJsonValue();
                    isSystem = true;
                }
            }
            if (jsonConfig == null) {
                throw new IncorrectParameterException("Failed to get sms provider configuration. Settings not found!");
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

    private ThingsboardException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send SMS: {}", message);
        return new ThingsboardException(String.format("Unable to send SMS: %s", message),
                ThingsboardErrorCode.GENERAL);
    }
}
