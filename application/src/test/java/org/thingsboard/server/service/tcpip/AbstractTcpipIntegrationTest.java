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
