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
package org.thingsboard.server.msa.cf;

import com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CFArgumentDynamicSourceType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomer;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class CalculatedFieldCurrentOwnerTest extends AbstractContainerTest {

    public final int TIMEOUT = 60;
    public final int POLL_INTERVAL = 1;

    private TenantId tenantId;
    private UserId tenantAdminId;
    private CustomerId customerId;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");

        customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer")).getId();
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testPerformInitialCalculationWhenCurrentOwner() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 1")).getId();
        String deviceToken = "zm235nIVf263lvnTP2XBE";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 1", deviceProfileId));
        testRestClient.changeOwner(customerId, device.getId());
        testRestClient.postTelemetryAttribute(customerId, AttributeScope.SERVER_SCOPE.name(), JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        testRestClient.postTelemetryAttribute(customerId, AttributeScope.SERVER_SCOPE.name(), JacksonUtil.toJsonNode("{\"attrKey\":15}"));

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("115");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testPerformInitialCalculationWhenOwnerChanged() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        testRestClient.postTelemetryAttribute(tenantId, AttributeScope.SERVER_SCOPE.name(), JacksonUtil.toJsonNode("{\"attrKey\":50}"));

        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 2")).getId();
        String deviceToken = "zmzUVndirwl5jzx8rtgiBE";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 2", deviceProfileId));

        testRestClient.changeOwner(customerId, device.getId());
        testRestClient.postTelemetryAttribute(customerId, AttributeScope.SERVER_SCOPE.name(), JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        CalculatedField savedCalculatedField = createSimpleCalculatedField(device.getId());

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        testRestClient.changeOwner(tenantId, device.getId());

        await().alias("change owner -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode fahrenheitTemp = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    @Test
    public void testEntityIdIsProfileAndCurrentOwner() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        testRestClient.postTelemetry(tenantId, JacksonUtil.toJsonNode("{\"key\":100}"));
        testRestClient.postTelemetry(tenantId, JacksonUtil.toJsonNode("{\"key\":200}"));

        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);

        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":10}"));
        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":17}"));
        testRestClient.postTelemetry(customerId, JacksonUtil.toJsonNode("{\"key\":18}"));

        DeviceProfileId deviceProfileId = testRestClient.postDeviceProfile(defaultDeviceProfile("Device Profile 3")).getId();

        String deviceToken = "zmzUVRfrejhgni82vf6nj3E";
        Device device = testRestClient.postDevice(deviceToken, createDevice("Device 3", deviceProfileId));
        testRestClient.changeOwner(customerId, device.getId());

        String newDeviceToken = "mmmXRIVSgh65nqP2PQE";
        Device newDevice = testRestClient.postDevice(newDeviceToken, createDevice("Device 4", deviceProfileId));

        CalculatedField savedCalculatedField = createScriptCalculatedField(deviceProfileId);

        await().alias("create CF -> perform initial calculation for devices by profile").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode avgValue3 = testRestClient.getLatestTelemetry(device.getId());
                    assertThat(avgValue3).isNotNull();
                    assertThat(avgValue3.get("avgValue").get(0).get("value").asText()).isEqualTo("15.0");

                    JsonNode avgValue4 = testRestClient.getLatestTelemetry(newDevice.getId());
                    assertThat(avgValue4).isNotNull();
                    assertThat(avgValue4.get("avgValue").get(0).get("value").asText()).isEqualTo("150.0");
                });

        testRestClient.changeOwner(tenantId, device.getId());

        await().alias("change owner -> perform calculation for device2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode avgValue = testRestClient.getLatestTelemetry(newDevice.getId());
                    assertThat(avgValue).isNotNull();
                    assertThat(avgValue.get("avgValue").get(0).get("value").asText()).isEqualTo("150.0");
                });

        testRestClient.deleteCalculatedFieldIfExists(savedCalculatedField.getId());
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test" + RandomStringUtils.randomAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("attrKey", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument.setRefEntityKey(refEntityKey);
        argument.setRefDynamicSource(CFArgumentDynamicSourceType.CURRENT_OWNER);
        config.setArguments(Map.of("a", argument));

        config.setExpression("a + 100");

        Output output = new Output();
        output.setName("result");
        output.setType(OutputType.TIME_SERIES);
        output.setDecimalsByDefault(0);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private CalculatedField createScriptCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("Average value:" + RandomStringUtils.randomAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("key", ArgumentType.TS_ROLLING, null);
        argument.setTimeWindow(30000L);
        argument.setLimit(5);
        argument.setRefDynamicSource(CFArgumentDynamicSourceType.CURRENT_OWNER);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("rollingKey", argument));

        config.setExpression("return {\"avgValue\": rollingKey.avg()};");

        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return testRestClient.postCalculatedField(calculatedField);
    }

    private Device createDevice(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return device;
    }

}
