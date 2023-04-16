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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
})
public abstract class BaseConverterControllerTest extends AbstractControllerTest {

    private IdComparator<Converter> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);

        Mockito.reset(tbClusterService, auditLogService);

        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Assert.assertNotNull(savedConverter);
        Assert.assertNotNull(savedConverter.getId());
        Assert.assertTrue(savedConverter.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedConverter.getTenantId());
        Assert.assertEquals(converter.getName(), savedConverter.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedConverter.setName("My new converter");
        doPost("/api/converter", savedConverter, Converter.class);

        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertEquals(foundConverter.getName(), savedConverter.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testFindConverterById() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);
        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
    }

    @Test
    public void testDeleteConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Mockito.reset(tbClusterService, auditLogService);

        String converterIdStr = savedConverter.getId().getId().toString();
        doDelete("/api/converter/" + converterIdStr)
                .andExpect(status().isOk());

        doGet("/api/converter/" + converterIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Converter", converterIdStr))));

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, converterIdStr);

    }

    @Test
    public void testSaveConverterWithEmptyType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter type " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException(msgError));
    }

    @Test
    public void testSaveConverterWithEmptyName() throws Exception {
        Converter converter = new Converter();
        converter.setType(ConverterType.UPLINK);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter name " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException(msgError));
    }


    @Test
    public void testSaveConverterWithEmptyConfiguration() throws Exception {
        Converter converter = new Converter();
        converter.setType(ConverterType.UPLINK);
        converter.setName("My converter");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter configuration " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindTenantConverters() throws Exception {
        List<Converter> converters = new ArrayList<>();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 178;
        for (int i = 0; i < cntEntity; i++) {
            Converter converter = new Converter();
            converter.setName("Converter" + i);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Converter(), new Converter(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);
    }

    @Test
    public void testFindTenantConvertersBySearchText() throws Exception {
        String title1 = "Converter title 1";
        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Converter converter = new Converter();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }
        String title2 = "Converter title 2";
        List<Converter> converters1 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Converter converter = new Converter();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters1.add(doPost("/api/converter", converter, Converter.class));
        }

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        List<Converter> loadedConverters1 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters1, idComparator);
        Collections.sort(loadedConverters1, idComparator);

        Assert.assertEquals(converters1, loadedConverters1);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedConverters.size();
        for (Converter converter : loadedConverters) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Converter(), new Converter(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, cntEntity, 1);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConverters1) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void whenDeletingConverter_thenReferencingIntegrationsAreChecked() throws Exception {
        Converter uplinkConverter = new Converter();
        uplinkConverter.setName("UP");
        uplinkConverter.setType(ConverterType.UPLINK);
        uplinkConverter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        uplinkConverter = doPost("/api/converter", uplinkConverter, Converter.class);

        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("DOWN");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(JacksonUtil.newObjectNode().set("encoder", new TextNode("ab")));
        downlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);

        Integration integration = new Integration();
        integration.setName("III");
        integration.setType(IntegrationType.HTTP);
        integration.setRoutingKey("rrr");
        integration.setDefaultConverterId(uplinkConverter.getId());
        integration.setDownlinkConverterId(downlinkConverter.getId());
        integration.setEnabled(false);
        doPost("/api/integration", integration, Integration.class);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "The converter referenced by the integration cannot be deleted!";
        doDelete("/api/converter/" + uplinkConverter.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(uplinkConverter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), uplinkConverter.getId().getId().toString());

        Mockito.reset(tbClusterService, auditLogService);

        msgError = "The downlink converter referenced by the integration cannot be deleted!";
        doDelete("/api/converter/" + downlinkConverter.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(downlinkConverter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), downlinkConverter.getId().getId().toString());
    }

    @Test
    public void givenUplinkPayloadWithUtf8Chars_testConverting() {
        String textPayload = "Привіт,Genève Hôpital Etterbeek-Ixelles,我们一起去玩吧。,اللغة العربية";
        String base64Payload = Base64Utils.encodeToString(textPayload.getBytes(StandardCharsets.UTF_8));

        String actualPayload = convertForPayload(base64Payload).getFirst();
        assertThat(actualPayload).isEqualTo(textPayload);
    }

    @Test
    public void givenUplinkWithBinaryPayload_testConverting() {
        Map<String, int[]> expectedResults = Map.of(
                "ALxhTl8OTGM=", new int[]{0, 188, 97, 78, 95, 14, 76, 99},
                "f5bI+g==", new int[]{127, 150, 200, 250}
        );
        expectedResults.forEach((base64Payload, bytes) -> {
            int[] actualCharCodes = convertForPayload(base64Payload).getSecond();
            assertThat(actualCharCodes).containsExactly(bytes);
        });
    }

    private Pair<String, int[]> convertForPayload(String base64Payload) {
        String decoderConfiguration = "" +
                "var payloadStr = decodeToString(payload);\n" +
                "var result = {\n" +
                "   telemetry: {\n" +
                "       payload: payload," +
                "       payloadStr: payloadStr\n" +
                "   }\n" +
                "};" +
                "function decodeToString(payload) {\n" +
                "   return String.fromCharCode.apply(String, payload);\n" +
                "}\n" +
                "return result;";

        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.set("decoder", new TextNode(decoderConfiguration));
        inputParams.set("payload", new TextNode(base64Payload));
        inputParams.set("metadata", JacksonUtil.newObjectNode());

        JsonNode output = doPost("/api/converter/testUpLink", inputParams, JsonNode.class);
        JsonNode telemetry = JacksonUtil.toJsonNode(output.get("output").asText()).get("telemetry");
        String payloadStr = telemetry.get("payloadStr").asText();
        int[] payloadCharCodes = Streams.stream(telemetry.get("payload").iterator())
                .mapToInt(JsonNode::asInt)
                .toArray();
        return Pair.of(payloadStr, payloadCharCodes);
    }

}
