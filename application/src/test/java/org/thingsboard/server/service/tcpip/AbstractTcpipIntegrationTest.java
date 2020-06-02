/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.tcpip;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.controller.AbstractControllerTest;

public abstract class AbstractTcpipIntegrationTest extends AbstractControllerTest {

    protected Tenant tenant;
    protected Converter converter;
    protected Integration integration;

    protected Converter createConverter(String converterName, JsonNode converterConfig) throws Exception {
        Converter converter = new Converter();
        converter.setTenantId(tenant.getTenantId());
        converter.setName(converterName);
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfig);
        return doPost("/api/converter", converter, Converter.class);
    }

    protected Integration createIntegration(String integrationName, IntegrationType type) throws Exception {
        Integration integration = new Integration();
        integration.setTenantId(tenant.getTenantId());
        integration.setDefaultConverterId(converter.getId());
        integration.setName(integrationName);
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setType(type);
        JsonNode integrationConfig = createIntegrationConfiguration();
        integration.setConfiguration(integrationConfig);
        integration.setDebugMode(true);
        return doPost("/api/integration", integration, Integration.class);
    }

    protected abstract JsonNode createIntegrationConfiguration();

}
