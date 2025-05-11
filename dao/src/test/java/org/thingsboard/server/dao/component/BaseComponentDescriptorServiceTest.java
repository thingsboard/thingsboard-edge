/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseComponentDescriptorServiceTest {

    private BaseComponentDescriptorService service;
    private ComponentDescriptor componentDescriptor;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(BaseComponentDescriptorService.class);
        tenantId = TenantId.SYS_TENANT_ID;

        // Create a simple component descriptor
        componentDescriptor = new ComponentDescriptor();
        componentDescriptor.setType(ComponentType.ACTION);
        componentDescriptor.setScope(ComponentScope.TENANT);
        componentDescriptor.setClusteringMode(ComponentClusteringMode.ENABLED);
        componentDescriptor.setName("Test Component");
        componentDescriptor.setClazz("org.thingsboard.test.TestComponent");

        // Create configuration descriptor with schema from JSON string
        String configDescriptorJson = """
                {
                  "schema": {
                    "type": "object",
                    "properties": {
                      "testField": {
                        "type": "string"
                      }
                    },
                    "required": ["testField"]
                  }
                }""";

        componentDescriptor.setConfigurationDescriptor(JacksonUtil.toJsonNode(configDescriptorJson));
    }

    @Test
    void testValidate() {
        // Create valid configuration from JSON string
        String validConfigJson = "{\"testField\": \"test value\"}";
        JsonNode validConfig = JacksonUtil.toJsonNode(validConfigJson);

        // Create invalid configuration (missing required field) from JSON string
        String invalidConfigJson = "{}";
        JsonNode invalidConfig = JacksonUtil.toJsonNode(invalidConfigJson);

        // Test valid configuration
        boolean validResult = service.validate(tenantId, componentDescriptor, validConfig);
        assertTrue(validResult, "Valid configuration should pass validation");

        // Test invalid configuration
        boolean invalidResult = service.validate(tenantId, componentDescriptor, invalidConfig);
        assertFalse(invalidResult, "Invalid configuration should fail validation");

        // Test with component descriptor without schema
        ComponentDescriptor noSchemaDescriptor = new ComponentDescriptor(componentDescriptor);
        noSchemaDescriptor.setConfigurationDescriptor(JacksonUtil.toJsonNode("{}"));

        // Should throw exception when schema is missing
        assertThrows(IncorrectParameterException.class, () -> {
            service.validate(tenantId, noSchemaDescriptor, validConfig);
        }, "Should throw exception when schema is missing");
    }

}
