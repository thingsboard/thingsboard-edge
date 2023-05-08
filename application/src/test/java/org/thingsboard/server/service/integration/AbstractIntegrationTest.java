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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractIntegrationTest extends AbstractControllerTest {

    @Autowired
    protected TbIntegrationDownlinkService downlinkService;

    protected Converter uplinkConverter;
    protected Converter downlinkConverter;
    protected Integration integration;

    protected void createConverter(String converterName, ConverterType type, JsonNode converterConfig) {
        Converter newConverter = new Converter();
        newConverter.setTenantId(tenantId);
        newConverter.setName(converterName);
        newConverter.setType(type);
        newConverter.setConfiguration(converterConfig);
        switch (type) {
            case UPLINK:
                uplinkConverter = doPost("/api/converter", newConverter, Converter.class);
                Assert.assertNotNull(uplinkConverter);
                break;
            case DOWNLINK:
                downlinkConverter = doPost("/api/converter", newConverter, Converter.class);
                Assert.assertNotNull(downlinkConverter);
                break;
        }
    }

    protected void createIntegration(String integrationName, IntegrationType type) throws InterruptedException {
        Integration newIntegration = new Integration();
        newIntegration.setTenantId(tenantId);
        newIntegration.setDefaultConverterId(uplinkConverter.getId());
        if (downlinkConverter != null) {
            newIntegration.setDownlinkConverterId(downlinkConverter.getId());
        }
        newIntegration.setName(integrationName);
        newIntegration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        newIntegration.setType(type);
        JsonNode clientConfig = createIntegrationClientConfiguration();
        ObjectNode integrationConfiguration = JacksonUtil.newObjectNode();
        integrationConfiguration.set("clientConfiguration", clientConfig);
        integrationConfiguration.set("metadata", JacksonUtil.newObjectNode());
        newIntegration.setConfiguration(integrationConfiguration);
        newIntegration.setDebugMode(true);
        newIntegration.setEnabled(false);
        newIntegration.setAllowCreateDevicesOrAssets(true);
        integration = doPost("/api/integration", newIntegration, Integration.class);
        Assert.assertNotNull(integration);
    }

    public void enableIntegration() {
        if (!integration.isEnabled()) {
            integration.setEnabled(true);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void disableIntegration() {
        if (integration.isEnabled()) {
            integration.setEnabled(false);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void removeIntegration(Integration integration) throws Exception {
        doDelete("/api/integration/" + integration.getId().getId().toString()).andExpect(status().isOk());
    }

    protected abstract JsonNode createIntegrationClientConfiguration();


    public List<EventInfo> getIntegrationDebugMessages(long startTs, String expectedMessageType, IntegrationDebugMessageStatus expectedStatus, long timeout) throws Exception {
        long endTs = startTs + timeout * 1000;
        List<EventInfo> targetMsgs;
        List<EventInfo> allMsgs;
        do {
            SortOrder sortOrder = new SortOrder("createdTime", SortOrder.Direction.DESC);
            TimePageLink pageLink = new TimePageLink(100, 0, null, sortOrder, startTs, endTs);
            PageData<EventInfo> events = doGetTypedWithTimePageLink("/api/events/INTEGRATION/{entityId}/DEBUG_INTEGRATION?tenantId={tenantId}&",
                    new TypeReference<>() {
                    },
                    pageLink, integration.getId(), integration.getTenantId());
            allMsgs = events.getData();
            targetMsgs = events.getData().stream().filter(event -> expectedMessageType.equals(event.getBody().get("type").asText())
                    && (IntegrationDebugMessageStatus.ANY.equals(expectedStatus)
                    || expectedStatus.name().equals(event.getBody().get("status").asText()))).collect(Collectors.toList());
            if (targetMsgs.size() > 0) {
                break;
            }
            Thread.sleep(100);
        }
        while (System.currentTimeMillis() <= endTs);
        if (allMsgs == null || allMsgs.isEmpty()) {
            log.error("[{} - {}] ALL DEBUG EVENTS ARE EMPTY.", startTs, endTs);
        } else {
            log.error("[{} - {}] THERE ARE {} DEBUG EVENTS ", startTs, endTs, allMsgs.size());
            allMsgs.forEach(event -> log.error("DEBUG EVENT: {}", event));
        }
        return targetMsgs;
    }
}
