/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.controller.AbstractControllerTest;

public abstract class AbstractIntegrationTest extends AbstractControllerTest {

    @Autowired
    protected TbIntegrationDownlinkService downlinkService;

    protected Converter uplinkConverter;
    protected Converter downlinkConverter;
    protected Integration integration;

    protected void createConverter(String converterName, ConverterType type, JsonNode converterConfig) throws Exception {
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

    protected void createIntegration(String integrationName, IntegrationType type) throws Exception {
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
        newIntegration.setEnabled(true);
        newIntegration.setAllowCreateDevicesOrAssets(true);
        integration = doPost("/api/integration", newIntegration, Integration.class);
        Assert.assertNotNull(integration);
    }

    public void enableIntegration() throws Exception {
        if (!integration.isEnabled()) {
            integration.setEnabled(true);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void disableIntegration() throws Exception {
        if (!integration.isEnabled()) {
            integration.setEnabled(false);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void removeIntegration(Integration integration) throws Exception {
        if (integration != null) {
            MvcResult result = doDelete("/api/integration/" + integration.getId().getId().toString()).andReturn();
            Assert.assertEquals(200, result.getResponse().getStatus());
        }
    }

    protected abstract JsonNode createIntegrationClientConfiguration();

}
