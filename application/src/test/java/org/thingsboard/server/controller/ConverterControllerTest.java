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
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.exception.DataValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
})
@DaoSqlTest
public class ConverterControllerTest extends AbstractControllerTest {
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

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedConverter, savedConverter.getId(), savedConverter.getId(),
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

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Converter(), new Converter(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, 1);

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
    public void testGettingDefaultPayloadForConverters() throws Exception {
        Map<IntegrationType, String> expectedPayloads = Map.of(
                IntegrationType.LORIOT, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.CHIRPSTACK, DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.TTN, DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.TTI, DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE);

        for (IntegrationType integrationType : expectedPayloads.keySet()) {
            String integrationName = "TEST_" + integrationType;
            String urlTemplate = "/api/converter/{converterId}/debugIn?integrationName={integrationName}&integrationType={integrationType}&converterType={converterType}";
            ObjectNode resultNode = doGet(urlTemplate, ObjectNode.class, EntityId.NULL_UUID, integrationName, integrationType.name(), ConverterType.UPLINK.name());

            Assert.assertNotNull(resultNode);
            String metadataStr = resultNode.get("inMetadata").asText();
            Assert.assertFalse(String.format("Returned metadata is empty for integration: %s", integrationType.name()), StringUtils.isBlank(metadataStr));
            ObjectNode inMetadata = JacksonUtil.fromString(metadataStr, ObjectNode.class);

            Assert.assertNotNull(inMetadata);
            Assert.assertEquals(String.format("Unexpected payload %s for integration %s, expected is: %s", resultNode.get("inContent").asText(), integrationType.name(), expectedPayloads.get(integrationType)), expectedPayloads.get(integrationType), resultNode.get("inContent").asText());
            Assert.assertEquals(integrationName, inMetadata.get("integrationName").asText());
            Assert.assertEquals("JSON", resultNode.get("inContentType").asText());

        }
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

    @Test
    public void testSaveConverter_checkLimitWithoutCountingEdgeTemplateConverters() throws Exception {
        loginSysAdmin();
        long limit = 5;
        EntityInfo defaultTenantProfileInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        TenantProfile defaultTenantProfile = doGet("/api/tenantProfile/" + defaultTenantProfileInfo.getId().getId().toString(), TenantProfile.class);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxConverters(limit).build());
        doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);

        loginTenantAdmin();

        // creation of edge template converters will not impact creation of core converters
        for (int i = 0; i < limit; i++) {
            Converter edgeConverter = createConverter("My edge converter before" + i, true);
            doPost("/api/converter", edgeConverter, Converter.class);
        }

        for (int i = 0; i < limit; i++) {
            Converter converter = createConverter("My converter" + i, false);
            doPost("/api/converter", converter, Converter.class);
        }

        // creation of edge template converters allowed in case core converters limit reached
        for (int i = 0; i < limit; i++) {
            Converter edgeConverter = createConverter("My edge converter after" + i, true);
            doPost("/api/converter", edgeConverter, Converter.class);
        }

        try {
            Converter converter = createConverter("Converter Out Of Limit", false);
            doPost("/api/converter", converter).andExpect(status().is4xxClientError());
        } finally {
            defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxConverters(0).build());
            loginSysAdmin();
            doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);
        }
    }

    @Test
    public void testTTIDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"eui-1000000000000001\",\"deviceType\":\"application-tti-name\"," +
                "\"groupName\":\"IAQ devices\",\"attributes\":{\"devEui\":\"1000000000000001\",\"fPort\":85," +
                "\"correlation_ids\":[\"as:up:01H0PZDGB1NW6NAPD815NGHPF6\",\"gs:conn:01H0FJRSXSYT7VKNYXJ89F95XT\",\"gs:up:host:01H0FJRSY3MZMGPPFBQ4FZV4T8\"," +
                "\"gs:uplink:01H0PZDG4HHGFRTXRTXD4PFTH7\",\"ns:uplink:01H0PZDG4JZ3BM0K6J89EQK1J7\",\"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0PZDG4J02F85RYFPCNSNXCR\"," +
                "\"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0PZDGB081PMP806BJHNHX1A\"],\"bandwidth\":125000," +
                "\"spreading_factor\":7,\"coding_rate\":\"4/5\",\"frequency\":\"868500000\",\"net_id\":\"000013\"," +
                "\"tenant_id\":\"tenant\",\"cluster_id\":\"eu1\",\"cluster_address\":\"eu1.cloud.thethings.industries\"," +
                "\"tenant_address\":\"tenant.eu1.cloud.thethings.industries\",\"device_id\":\"eui-1000000000000001\"," +
                "\"application_id\":\"application-tti-name\",\"join_eui\":\"2000000000000001\",\"dev_addr\":\"20000001\"}," +
                "\"telemetry\":{\"ts\":1684398325906,\"values\":{\"HEX_bytes\":\"01755E030001040001\"," +
                "\"session_key_id\":\"AYfg8rhha5n+FWx0ZaAprA==\",\"f_cnt\":5017,\"frm_payload\":\"AXVeAwABBAAB\"," +
                "\"eui\":\"6A7E111A10000000\",\"rssi\":-24,\"channel_rssi\":-24,\"snr\":12,\"frequency_offset\":\"671\"," +
                "\"channel_index\":2,\"message_id\":\"01H0PZDG4MF9AYSMNY44MAVTDH\",\"forwarder_net_id\":\"000013\"," +
                "\"forwarder_tenant_id\":\"ttn\",\"forwarder_cluster_id\":\"eu1.cloud.thethings.network\",\"forwarder_gateway_eui\":\"6A7E111A10000000\"," +
                "\"forwarder_gateway_id\":\"eui-6a7e111a10000000\",\"home_network_net_id\":\"000013\"," +
                "\"home_network_tenant_id\":\"tenant\",\"home_network_cluster_id\":\"eu1.cloud.thethings.industries\",\"consumed_airtime\":\"0.097536s\"}}}";
        testDecoder("tbel-tti-decoder.raw", DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testTTNDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"eui-1000000000000001\",\"deviceType\":\"application-tts-name\"," +
                "\"groupName\":\"IAQ devices\",\"attributes\":{\"devEui\":\"1000000000000001\",\"fPort\":85," +
                "\"correlation_ids\":[\"as:up:01H0S7ZJQ9MQPMVY49FT3SE07M\",\"gs:conn:01H03BQZ9342X3Y86DJ2P704E5\"," +
                "\"gs:up:host:01H03BQZ99EGAM52KK1300GFKN\",\"gs:uplink:01H0S7ZJGS6D9TJSKJN8XNTMAV\",\"ns:uplink:01H0S7ZJGS9KKD4HTTPKFEMWCV\"," +
                "\"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0S7ZJGSF3M38ZRZVTM38DEC\",\"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0S7ZJQ8R2EH5AA269AKM8DX\"]," +
                "\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\",\"frequency\":\"867100000\"," +
                "\"net_id\":\"000013\",\"tenant_id\":\"ttn\",\"cluster_id\":\"eu1\",\"cluster_address\":\"eu1.cloud.thethings.network\"," +
                "\"device_id\":\"eui-1000000000000001\",\"application_id\":\"application-tts-name\",\"join_eui\":\"2000000000000001\"," +
                "\"dev_addr\":\"20000001\"},\"telemetry\":{\"ts\":1684474415641,\"values\":{\"HEX_bytes\":\"01755E030001040001\"," +
                "\"session_key_id\":\"AYfqmb0pc/1uRZv9xUydgQ==\",\"f_cnt\":10335,\"frm_payload\":\"AXVeAwABBAAB\",\"eui\":\"6A7E111A10000000\"," +
                "\"rssi\":-35,\"channel_rssi\":-35,\"snr\":13.2,\"frequency_offset\":\"69\",\"channel_index\":3,\"consumed_airtime\":\"0.056576s\"}}}";
        testDecoder("tbel-ttn-decoder.raw", DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testChirpstackDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Device name\",\"deviceType\":\"Chirpstack default device profile\"," +
                "\"groupName\":\"IAQ devices\",\"attributes\":{\"deduplicationId\":\"57433366-50a6-4dc2-8145-2df1bbc70d9e\"," +
                "\"tenantId\":\"52f14cd4-c6f1-4fbd-8f87-4025e1d49242\",\"tenantName\":\"ChirpStack\",\"applicationId\":\"ca739e26-7b67-4f14-b69e-d568c22a5a75\"," +
                "\"applicationName\":\"Chirpstack application\",\"deviceProfileId\":\"605d08d4-65f5-4d2c-8a5a-3d2457662f79\"," +
                "\"deviceProfileName\":\"Chirpstack default device profile\",\"devEui\":\"1000000000000001\",\"devAddr\":\"20000001\",\"fPort\":85," +
                "\"frequency\":868500000,\"bandwidth\":125000,\"spreadingFactor\":7,\"codeRate\":\"CR_4_5\"},\"telemetry\":{\"ts\":1684741625404," +
                "\"values\":{\"HEX_bytes\":\"01755D030001040000\",\"dr\":5,\"fCnt\":4,\"confirmed\":false,\"gatewayId\":\"6a7e111a10000000\"," +
                "\"uplinkId\":24022,\"rssi\":-35,\"snr\":11.5,\"channel\":2,\"rfChain\":1,\"context\":\"EFwMtA==\",\"crcStatus\":\"CRC_OK\"}}}";
        testDecoder("tbel-chirpstack-decoder.raw", DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testLoriotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "[{\"deviceName\":\"1000000000000001\",\"deviceType\":\"LoraDevices\"," +
                "\"groupName\":\"IAQ devices\",\"attributes\":{\"fPort\":85,\"dataRange\":\"SF9 BW125 4/5\",\"freq\":867500000,\"offline\":false}," +
                "\"telemetry\":{\"ts\":1684478801936,\"values\":{\"HEX_bytes\":\"01755E030001040001\",\"seqno\":3040," +
                "\"fcnt\":2,\"rssi\":-21,\"snr\":10,\"toa\":206,\"ack\":false,\"bat\":94}}}]";
        testDecoder("tbel-loriot-decoder.raw", DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testAwsIotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Production 1 - 3G7H1j-9zF\",\"deviceType\":\"default\"," +
                "\"groupName\":\"Production\",\"attributes\":{\"deviceId\":\"3G7H1j-9zF\"},\"telemetry\":{\"ts\":1686398400000,\"values\":" +
                "{\"temperature\":25.3,\"humidity\":62.8,\"pressure\":1012.5,\"latitude\":37.7749,\"longitude\":-122.4194," +
                "\"status\":\"active\",\"power_status\":\"on\",\"x\":0.02,\"y\":0.03,\"z\":0.01,\"fault_codes.0\":100,\"fault_codes.1\":204," +
                "\"fault_codes.2\":301,\"battery_level\":78.5}}}";
        testDecoder("tbel-aws-iot-decoder.raw", DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testAzureDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"8F4A2C6D\",\"deviceType\":\"Packing machine\",\"groupName\":\"Control room\"," +
                "\"attributes\":{\"version\":1,\"manufacturer\":\"Example corporation\"},\"telemetry\":{\"ts\":1686306600000," +
                "\"values\":{\"receivedAlarms\":[{\"type\":\"temperature\",\"severity\":\"high\",\"message\":\"Temperature exceeds threshold.\"}," +
                "{\"type\":\"vibration\",\"severity\":\"critical\",\"message\":\"Excessive vibration detected.\"}],\"temperature\":25.5," +
                "\"pressure\":1013.25,\"x\":0.02,\"y\":0.03,\"z\":0.015,\"status\":\"ALARM\",\"batteryLevel\":100,\"batteryStatus\":\"Charging\"}}}";
        testDecoder("tbel-azure-decoder.raw", DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testSigfoxDefaultConverters() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Sigfox-2203961\",\"deviceType\":\"Sigfox device\",\"groupName\":\"Control room devices\"," +
                "\"attributes\":{\"sigfoxId\":\"2203961\",\"deviceTypeId\":\"630ceaea10d051194ec0246e\",\"autoCalibration\":\"on\"," +
                "\"zeroPointAdjusted\":false,\"transmitPower\":\"full\",\"powerControl\":\"off\",\"fwVersion\":2},\"telemetry\":{\"ts\":\"1686298419000\"," +
                "\"values\":{\"temperature\":28.7,\"humidity\":33,\"co2\":582,\"co2Baseline\":420,\"customData1\":\"37\",\"customData2\":\"2\"}}}";
        testDecoder("tbel-sigfox-decoder.raw", DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    public void testDecoder(String decoderFileName, String payloadExample, String expectedResult) throws IOException {
        byte[] bytes = IOUtils.toByteArray(ConverterControllerTest.class.getClassLoader().getResourceAsStream("converters/" + decoderFileName));
        String base64Payload = Base64Utils.encodeToString(payloadExample.getBytes(StandardCharsets.UTF_8));

        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.set("decoder",  new TextNode(new String(bytes)));
        inputParams.set("payload", new TextNode(base64Payload));
        inputParams.set("metadata", JacksonUtil.newObjectNode());

        JsonNode response = doPost("/api/converter/testUpLink?scriptLang=TBEL", inputParams, JsonNode.class);
        String output = response.get("output").asText();
        assertThat(output).isEqualTo(expectedResult);
    }

    private Converter createConverter(String name, boolean edgeTemplate) {
        Converter converter = new Converter();
        converter.setName(name);
        converter.setType(ConverterType.UPLINK);
        converter.setEdgeTemplate(edgeTemplate);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        return converter;
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
