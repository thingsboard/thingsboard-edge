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
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CFArgumentDynamicSourceType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CalculatedFieldCurrentOwnerTest extends AbstractControllerTest {

    public static final int TIMEOUT = 60;
    public static final int POLL_INTERVAL = 1;

    @Test
    public void testCreateCFWithCurrentOwner() throws Exception {
        loginTenantAdmin();

        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedField(testDevice.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":10}"));

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("110");
                });
    }

    @Test
    public void testChangeOwner() throws Exception {
        loginSysAdmin();

        doPost("/api/plugins/telemetry/TENANT/" + tenantId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":50}"));

        loginTenantAdmin();

        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());


        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedField(testDevice.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("105");
                });

        doPost("/api/owner/TENANT/" + tenantId.getId() + "/DEVICE/" + testDevice.getId().getId()).andExpect(status().isOk());

        await().alias("change owner -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("result").get(0).get("value").asText()).isEqualTo("150");
                });
    }

    @Test
    public void testCreateCFWithCurrentOwnerWhenEntityIsProfile() throws Exception {
        loginSysAdmin();

        doPost("/api/plugins/telemetry/TENANT/" + tenantId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":50}"));

        loginTenantAdmin();

        doPost("/api/plugins/telemetry/CUSTOMER/" + customerId.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"attrKey\":5}"));

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);

        Asset asset1 = createAsset("Test asset 1", assetProfile.getId());
        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/ASSET/" + asset1.getId().getId()).andExpect(status().isOk());

        Asset asset2 = createAsset("Test asset 2", assetProfile.getId()); // owner - TENANT

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", buildCalculatedField(assetProfile.getId()), CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ObjectNode result1 = getLatestTelemetry(asset1.getId(), "result");
                    assertThat(result1).isNotNull();
                    assertThat(result1.get("result").get(0).get("value").asText()).isEqualTo("105");

                    //  result of asset 2
                    ObjectNode result2 = getLatestTelemetry(asset2.getId(), "result");
                    assertThat(result2).isNotNull();
                    assertThat(result2.get("result").get(0).get("value").asText()).isEqualTo("150");
                });

        doPost("/api/owner/CUSTOMER/" + customerId.getId() + "/ASSET/" + asset2.getId().getId()).andExpect(status().isOk());

        await().alias("change asset2 owner -> recalculate state for asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 2
                    ObjectNode result2 = getLatestTelemetry(asset2.getId(), "result");
                    assertThat(result2).isNotNull();
                    assertThat(result2.get("result").get(0).get("value").asText()).isEqualTo("105");
                });
    }

    private CalculatedField buildCalculatedField(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");

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
        return calculatedField;
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

}
