/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.ComputeOn;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true"
})
public class CalculatedFieldComputeOnTest extends AbstractControllerTest { // edge only: expectations are inverted vs the cloud

    private static final int TIMEOUT = 60;
    private static final int POLL_INTERVAL = 1;
    private static final String OUTPUT_KEY = "fahrenheitTemp";

    private Tenant savedTenant;

    // edge only
    @Autowired
    private CloudEventService cloudEventService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Compute on tenant");
        savedTenant = saveTenant(tenant);

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("computeOnTenant@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testDefaultComputeOnIsCalculatedOnEdge() throws Exception {
        Device device = givenDeviceWithTemperature("Default device", "default-1234");

        doPost("/api/calculatedField", cf(device.getId(), null), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
    }

    @Test
    public void testComputeOnCloudIsNotCalculatedOnEdge() throws Exception {
        Device device = givenDeviceWithTemperature("Cloud device", "cloud-1234");

        doPost("/api/calculatedField", cf(device.getId(), ComputeOn.CLOUD), CalculatedField.class);

        awaitNoOutput(device.getId());
    }

    @Test
    public void testComputeOnEdgeIsCalculatedOnEdge() throws Exception {
        Device device = givenDeviceWithTemperature("Edge device", "edge-1234");

        doPost("/api/calculatedField", cf(device.getId(), ComputeOn.EDGE), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
    }

    @Test
    public void testSwitchingComputeOnStartsAndStopsCalculationOnEdge() throws Exception {
        Device device = givenDeviceWithTemperature("Switching device", "switch-1234");
        CalculatedField saved = doPost("/api/calculatedField", cf(device.getId(), ComputeOn.CLOUD), CalculatedField.class);

        awaitNoOutput(device.getId());

        saved.setComputeOn(ComputeOn.EDGE);
        saved = doPost("/api/calculatedField", saved, CalculatedField.class);

        awaitOutput(device.getId(), "77.0");

        saved.setComputeOn(ComputeOn.CLOUD);
        doPost("/api/calculatedField", saved, CalculatedField.class);

        postTelemetry(device.getId(), "{\"temperature\":30}");

        await().alias("switch back to CLOUD -> edge stops recalculating").during(10, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(device.getId(), OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.asText()).isEqualTo("77.0");
                });
    }

    // edge only START

    @Test
    public void testImmediateResultOfEdgeComputeOnIsPushedToCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Immediate edge device", "immediate-edge-1234");

        doPost("/api/calculatedField", immediateCf(device.getId(), ComputeOn.EDGE), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
        awaitOutputCloudEvent(device.getId());
    }

    private void awaitOutputCloudEvent(EntityId entityId) {
        await().alias("IMMEDIATE time series result is queued for the cloud").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(cloudEventBodies(entityId, EdgeEventActionType.TIMESERIES_UPDATED, "data"))
                        .as("cloud event with the calculated field time series output")
                        .anySatisfy(body -> {
                            assertThat(body.path("data").path(OUTPUT_KEY).asText()).isEqualTo("77.0");
                            assertThat(body.path("ts").asLong()).isPositive();
                        }));
    }

    @Test
    public void testImmediateResultOfDefaultComputeOnIsAlsoPushedToCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Immediate default device", "immediate-default-1234");

        doPost("/api/calculatedField", immediateCf(device.getId(), null), CalculatedField.class);

        awaitOutput(device.getId(), "77.0");
        awaitOutputCloudEvent(device.getId());
    }

    @Test
    public void testImmediateAttributesResultOfEdgeComputeOnIsPushedToCloud() throws Exception {
        Device device = givenDeviceWithTemperature("Immediate edge attributes device", "immediate-edge-attr-1234");

        doPost("/api/calculatedField", immediateAttributesCf(device.getId(), ComputeOn.EDGE), CalculatedField.class);

        awaitAttributeOutput(device.getId(), "77.0");
        awaitAttributesCloudEvent(device.getId(), "cloud event with the calculated field attributes output");
    }

    @Test
    public void testImmediatePropagationResultIsPushedToCloudForRelatedEntity() throws Exception {
        Device device = givenDeviceWithTemperature("Propagation edge device", "propagation-edge-1234");
        Asset asset = createAsset("Propagated edge asset");
        createEntityRelation(asset.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);

        doPost("/api/calculatedField", propagationCf(device.getId(), ComputeOn.EDGE), CalculatedField.class);

        // the result is written to the related asset, never to the originating device
        awaitAttributeOutput(asset.getId(), "77.0");
        awaitAttributesCloudEvent(asset.getId(), "cloud event with the propagated calculated field output");
        assertThat(cloudEventBodies(device.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, "kv"))
                .as("originating device must not receive the propagated output").isEmpty();
    }

    private void awaitAttributesCloudEvent(EntityId entityId, String description) {
        await().alias("IMMEDIATE attributes result is queued for the cloud").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(cloudEventBodies(entityId, EdgeEventActionType.ATTRIBUTES_UPDATED, "kv"))
                        .as(description)
                        .anySatisfy(body -> {
                            assertThat(body.path("kv").path(OUTPUT_KEY).asDouble()).isEqualTo(77.0);
                            assertThat(body.path("scope").asText()).isEqualTo(AttributeScope.SERVER_SCOPE.name());
                            assertThat(body.path("ts").asLong()).isPositive();
                        }));
    }

    private CalculatedField propagationCf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.PROPAGATION);
        calculatedField.setName("Propagate C to F");
        calculatedField.setComputeOn(computeOn);
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));

        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SERVER_SCOPE);
        output.setStrategy(new AttributesImmediateOutputStrategy(false, false, true, true, false));

        PropagationCalculatedFieldConfiguration config = new PropagationCalculatedFieldConfiguration();
        config.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        config.setApplyExpressionToResolvedArguments(true);
        config.setArguments(Map.of("T", argument));
        config.setExpression("return { " + OUTPUT_KEY + ": (T * 9/5) + 32 };");
        config.setOutput(output);
        calculatedField.setConfiguration(config);

        return calculatedField;
    }

    private Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        return doPost("/api/asset", asset, Asset.class);
    }

    private List<JsonNode> cloudEventBodies(EntityId entityId, EdgeEventActionType action, String bodyField) {
        return cloudEventService.findTsKvCloudEvents(savedTenant.getId(), null, null, new TimePageLink(1000)).getData().stream()
                .filter(event -> action == event.getAction())
                .filter(event -> entityId.getId().equals(event.getEntityId()))
                .map(CloudEvent::getEntityBody)
                .filter(body -> body != null && body.path(bodyField).has(OUTPUT_KEY))
                .toList();
    }

    private CalculatedField immediateCf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = cf(entityId, computeOn);
        SimpleCalculatedFieldConfiguration config = (SimpleCalculatedFieldConfiguration) calculatedField.getConfiguration();
        TimeSeriesOutput output = (TimeSeriesOutput) config.getOutput();
        output.setStrategy(new TimeSeriesImmediateOutputStrategy(0, true, true, true, false));
        return calculatedField;
    }

    private CalculatedField immediateAttributesCf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = cf(entityId, computeOn);
        SimpleCalculatedFieldConfiguration config = (SimpleCalculatedFieldConfiguration) calculatedField.getConfiguration();
        AttributesOutput output = new AttributesOutput();
        output.setName(OUTPUT_KEY);
        output.setScope(AttributeScope.SERVER_SCOPE);
        output.setStrategy(new AttributesImmediateOutputStrategy(false, false, true, true, false));
        config.setOutput(output);
        return calculatedField;
    }

    private void awaitAttributeOutput(EntityId entityId, String expectedValue) {
        await().alias("attributes CF is calculated on the edge").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode attributes = doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId()
                            + "/values/attributes/" + AttributeScope.SERVER_SCOPE.name() + "?keys=" + OUTPUT_KEY, JsonNode.class);
                    assertThat(attributes.path(0).path("value").asDouble()).isEqualTo(Double.parseDouble(expectedValue));
                });
    }

    // edge only END

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private Device givenDeviceWithTemperature(String name, String accessToken) throws Exception {
        Device device = createDevice(name, accessToken);
        postTelemetry(device.getId(), "{\"temperature\":25}");
        return device;
    }

    private void awaitOutput(EntityId entityId, String expectedValue) {
        await().alias("CF is calculated on the edge").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(entityId, OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.asText()).isEqualTo(expectedValue);
                });
    }

    private void awaitNoOutput(EntityId entityId) {
        await().alias("CF is not calculated on the edge").during(10, TimeUnit.SECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode value = getLatestTelemetry(entityId, OUTPUT_KEY).path(OUTPUT_KEY).path(0).path("value");
                    assertThat(value.isMissingNode() || value.isNull())
                            .as("CF must not be calculated on the edge, but got value %s", value).isTrue();
                });
    }

    private CalculatedField cf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setComputeOn(computeOn);
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName(OUTPUT_KEY);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");
        config.setOutput(output);
        calculatedField.setConfiguration(config);

        return calculatedField;
    }

}
