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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomTranslationProto;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
abstract public class AbstractEdgeTest extends AbstractControllerTest {

    protected static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    protected Tenant savedTenant;
    protected TenantId tenantId;
    protected User tenantAdmin;

    protected DeviceProfile thermostatDeviceProfile;

    protected EdgeImitator edgeImitator;
    protected Edge edge;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Autowired
    protected DataDecodingEncodingService dataDecodingEncodingService;

    @Autowired
    protected TbClusterService clusterService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);

        CustomTranslation content = new CustomTranslation();
        content.getTranslationMap().put("key", "sys_admin_value");
        doPost("/api/customTranslation/customTranslation", content, CustomTranslation.class);
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setAppTitle("Sys Admin TB");
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);
        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        loginWhiteLabelingParams.setDomainName("sysadmin.org");
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        content = new CustomTranslation();
        content.getTranslationMap().put("key", "tenant_value");
        doPost("/api/customTranslation/customTranslation", content, CustomTranslation.class);
        whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setAppTitle("Tenant TB");
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);
        loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        loginWhiteLabelingParams.setDomainName("tenant.org");
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);

        // sleep 0.5 second to avoid CREDENTIALS updated message for the user
        // user credentials is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event edge could be already added and additional message will be pushed
        Thread.sleep(500);

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.expectMessageAmount(19);
        edgeImitator.connect();

        requestEdgeRuleChainMetadata();

        verifyEdgeConnectionAndInitialData();
    }

    private void requestEdgeRuleChainMetadata() throws Exception {
        RuleChainId rootRuleChainId = getEdgeRootRuleChainId();
        RuleChainMetadataRequestMsg.Builder builder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(rootRuleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(rootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(builder);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(builder.build());
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
    }

    private RuleChainId getEdgeRootRuleChainId() throws Exception {
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain.getId();
            }
        }
        throw new RuntimeException("Root rule chain not found");
    }

    @After
    public void afterTest() throws Exception {
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {}

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getUuidId())
                .andExpect(status().isOk());

        revertSysAdminWhiteLabelingAndCustomTranslation();
    }

    private void revertSysAdminWhiteLabelingAndCustomTranslation() throws Exception {
        doPost("/api/customTranslation/customTranslation", new CustomTranslation(), CustomTranslation.class);

        doPost("/api/whiteLabel/loginWhiteLabelParams", new LoginWhiteLabelingParams(), LoginWhiteLabelingParams.class);
    }

    private void installation() throws Exception {
        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME,
                createMqttDeviceProfileTransportConfiguration(new JsonTransportPayloadConfiguration(), false));

        extendDeviceProfileData(thermostatDeviceProfile);
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);
    }

    protected void extendDeviceProfileData(DeviceProfile deviceProfile) {
        DeviceProfileData profileData = deviceProfile.getProfileData();
        List<DeviceProfileAlarm> alarms = new ArrayList<>();
        DeviceProfileAlarm deviceProfileAlarm = new DeviceProfileAlarm();
        deviceProfileAlarm.setAlarmType("High Temperature");
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Alarm Details");
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        List<AlarmConditionFilter> condition = new ArrayList<>();
        AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(55.0));
        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.NUMERIC);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        deviceProfileAlarm.setClearRule(alarmRule);
        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        deviceProfileAlarm.setCreateRules(createRules);
        alarms.add(deviceProfileAlarm);
        profileData.setAlarms(alarms);
        profileData.setProvisionConfiguration(new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("123"));
    }

    private void verifyEdgeConnectionAndInitialData() throws Exception {
        Assert.assertTrue(edgeImitator.waitForMessages());

        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);

        testAutoGeneratedCodeByProtobuf(configuration);

        List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        Assert.assertEquals(3, deviceProfileUpdateMsgList.size());
        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> THERMOSTAT_DEVICE_PROFILE_NAME.equals(dfum.getName())).findAny();
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        UUID deviceProfileUUID = new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB());
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotNull(deviceProfile.getProfileData());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms().get(0).getClearRule());

        testAutoGeneratedCodeByProtobuf(deviceProfileUpdateMsg);

        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));

        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);

        List<EntityGroupUpdateMsg> entityGroupUpdateMsgList = edgeImitator.findAllMessagesByType(EntityGroupUpdateMsg.class);
        Assert.assertEquals(2, entityGroupUpdateMsgList.size());

        List<LoginWhiteLabelingParamsProto> loginWlpUpdateMsgList = edgeImitator.findAllMessagesByType(LoginWhiteLabelingParamsProto.class);
        Assert.assertEquals(2, loginWlpUpdateMsgList.size());

        List<WhiteLabelingParamsProto> wlpUpdateMsgList = edgeImitator.findAllMessagesByType(WhiteLabelingParamsProto.class);
        Assert.assertEquals(2, wlpUpdateMsgList.size());

        List<CustomTranslationProto> customTranslationProtoList = edgeImitator.findAllMessagesByType(CustomTranslationProto.class);
        Assert.assertEquals(2, customTranslationProtoList.size());

        Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateOpt.isPresent());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainMetadataUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdMSB());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB());

        validateAdminSettings();

        List<RoleProto> roleProtoList = edgeImitator.findAllMessagesByType(RoleProto.class);
        Assert.assertEquals(2, roleProtoList.size());

        List<RuleChainUpdateMsg> ruleChainUpdateMsgList = edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class);
        Assert.assertEquals(2, ruleChainUpdateMsgList.size());
    }

    private void validateAdminSettings() throws JsonProcessingException {
        List<AdminSettingsUpdateMsg> adminSettingsUpdateMsgs = edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class);
        Assert.assertEquals(2, adminSettingsUpdateMsgs.size());

        for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : adminSettingsUpdateMsgs) {
            if (adminSettingsUpdateMsg.getKey().equals("mail")) {
                validateMailAdminSettings(adminSettingsUpdateMsg);
            }
            if (adminSettingsUpdateMsg.getKey().equals("mailTemplates")) {
                validateMailTemplatesAdminSettings(adminSettingsUpdateMsg);
            }
            if (adminSettingsUpdateMsg.getKey().equals("general")) {
                validateGeneralAdminSettings(adminSettingsUpdateMsg);
            }
        }
    }

    private void validateMailAdminSettings(AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("mailFrom"));
        Assert.assertNotNull(jsonNode.get("smtpProtocol"));
        Assert.assertNotNull(jsonNode.get("smtpHost"));
        Assert.assertNotNull(jsonNode.get("smtpPort"));
        Assert.assertNotNull(jsonNode.get("timeout"));
    }

    private void validateMailTemplatesAdminSettings(AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("accountActivated"));
        Assert.assertNotNull(jsonNode.get("accountLockout"));
        Assert.assertNotNull(jsonNode.get("activation"));
        Assert.assertNotNull(jsonNode.get("passwordWasReset"));
        Assert.assertNotNull(jsonNode.get("resetPassword"));
        Assert.assertNotNull(jsonNode.get("test"));
    }

    private void validateGeneralAdminSettings(AdminSettingsUpdateMsg adminSettingsUpdateMsg) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(adminSettingsUpdateMsg.getJsonValue());
        Assert.assertNotNull(jsonNode.get("baseUrl"));
    }

    protected Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create ota package
        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(thermostatDeviceProfile.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // create device and assign to edge
        edgeImitator.expectMessageAmount(1);
        EntityGroup deviceEntityGroup = new EntityGroup();
        deviceEntityGroup.setType(EntityType.DEVICE);
        deviceEntityGroup.setName(StringUtils.randomAlphanumeric(15));
        deviceEntityGroup = doPost("/api/entityGroup", deviceEntityGroup, EntityGroup.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + deviceEntityGroup.getId().toString() + "/" + EntityType.DEVICE.name(), EntityGroup.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        verifyEntityGroupUpdateMsg(edgeImitator.getLatestMessage(), deviceEntityGroup);

        edgeImitator.expectMessageAmount(1);
        Device savedDevice = saveDevice(StringUtils.randomAlphanumeric(15), THERMOSTAT_DEVICE_PROFILE_NAME, deviceEntityGroup.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // update device
        edgeImitator.expectMessageAmount(1);
        savedDevice.setFirmwareId(firmwareOtaPackageInfo.getId());

        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        deviceData.setTransportConfiguration(transportConfiguration);
        savedDevice.setDeviceData(deviceData);

        savedDevice = doPost("/api/device", savedDevice, Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getFirmwareIdMSB());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getFirmwareIdLSB());
        Optional<DeviceData> deviceDataOpt =
                dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
        Assert.assertTrue(deviceDataOpt.isPresent());
        deviceData = deviceDataOpt.get();
        Assert.assertTrue(deviceData.getTransportConfiguration() instanceof MqttDeviceTransportConfiguration);
        MqttDeviceTransportConfiguration mqttDeviceTransportConfiguration =
                (MqttDeviceTransportConfiguration) deviceData.getTransportConfiguration();
        Assert.assertEquals("tb_rule_engine.thermostat", mqttDeviceTransportConfiguration.getProperties().get("topic"));
        return savedDevice;
    }

    protected Device findDeviceByName(String deviceName) throws Exception {
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Optional<Device> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    protected Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();

        Assert.assertEquals(1, edgeAssets.size());
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals(assetName, asset.getName());
        return asset;
    }

    protected Device saveDevice(String deviceName, String type) {
        return saveDevice(deviceName, type, null);
    }

    protected Device saveDevice(String deviceName, String type, EntityGroupId entityGroupId) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        if (entityGroupId != null) {
            return doPost("/api/device?entityGroupId={entityGroupId}", device, Device.class, entityGroupId.getId().toString());
        } else {
            return doPost("/api/device", device, Device.class);
        }
    }

    protected Asset saveAsset(String assetName) {
        return saveAsset(assetName, "test", null);
    }

    protected Asset saveAsset(String assetName, String type, EntityGroupId entityGroupId) {
        Asset asset = new Asset();
        asset.setName(assetName);
        asset.setType(type);
        if (entityGroupId != null) {
            return doPost("/api/asset?entityGroupId={entityGroupId}", asset, Asset.class, entityGroupId.getId().toString());
        } else {
            return doPost("/api/asset", asset, Asset.class);
        }
    }

    protected OtaPackageInfo saveOtaPackageInfo(DeviceProfileId deviceProfileId) {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("Firmware Edge " + StringUtils.randomAlphanumeric(3));
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareInfo.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    protected EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction,
                                           UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    protected void testAutoGeneratedCodeByProtobuf(MessageLite.Builder builder) throws InvalidProtocolBufferException {
        MessageLite source = builder.build();

        testAutoGeneratedCodeByProtobuf(source);

        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        builder.clear().mergeFrom(target);
    }

    protected void testAutoGeneratedCodeByProtobuf(MessageLite source) throws InvalidProtocolBufferException {
        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        Assert.assertEquals(source, target);
        Assert.assertEquals(source.hashCode(), target.hashCode());
    }

    protected Asset saveAssetOnCloudAndVerifyDeliveryToEdge() throws Exception {
        edgeImitator.expectMessageAmount(1);
        EntityGroup assetEntityGroup = new EntityGroup();
        assetEntityGroup.setType(EntityType.ASSET);
        assetEntityGroup.setName(StringUtils.randomAlphanumeric(15));
        assetEntityGroup = doPost("/api/entityGroup", assetEntityGroup, EntityGroup.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + assetEntityGroup.getId().toString() + "/" + EntityType.ASSET.name(), EntityGroup.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        verifyEntityGroupUpdateMsg(edgeImitator.getLatestMessage(), assetEntityGroup);

        edgeImitator.expectMessageAmount(1);
        Asset savedAsset = saveAsset(StringUtils.randomAlphanumeric(15), "Building", assetEntityGroup.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedAsset;
    }

    private void verifyEntityGroupUpdateMsg(AbstractMessage latestMessage, EntityGroup entityGroup) {
        Assert.assertTrue(latestMessage instanceof EntityGroupUpdateMsg);
        EntityGroupUpdateMsg entityGroupUpdateMsg = (EntityGroupUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, entityGroupUpdateMsg.getMsgType());
        Assert.assertEquals(entityGroupUpdateMsg.getIdMSB(), entityGroup.getUuidId().getMostSignificantBits());
        Assert.assertEquals(entityGroupUpdateMsg.getIdLSB(), entityGroup.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(entityGroupUpdateMsg.getName(), entityGroup.getName());
        Assert.assertEquals(entityGroupUpdateMsg.getType(), entityGroup.getType().name());
    }

    protected EntityView saveEntityView(String entityViewName, String type, DeviceId deviceId, EntityGroupId entityGroupId) {
        EntityView entityView = new EntityView();
        entityView.setName("Edge EntityView 1");
        entityView.setType("test");
        entityView.setEntityId(deviceId);
        if (entityGroupId != null) {
            return doPost("/api/entityView?entityGroupId={entityGroupId}", entityView, EntityView.class, entityGroupId.getId().toString());
        } else {
            return doPost("/api/entityView", entityView, EntityView.class);
        }
    }

    protected Customer saveCustomer(String title, CustomerId parentCustomerId) {
        Customer customer = new Customer();
        customer.setTitle(title);
        customer.setParentCustomerId(parentCustomerId);
        return doPost("/api/customer", customer, Customer.class);
    }

    protected void changeEdgeOwnerToCustomer(Customer customer) throws Exception {
        edgeImitator.expectMessageAmount(3);
        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<CustomerUpdateMsg> customerUpdateMsgs = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateMsgs.isPresent());
        CustomerUpdateMsg customerAUpdateMsg = customerUpdateMsgs.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerAUpdateMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), customerAUpdateMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), customerAUpdateMsg.getIdLSB());
        Assert.assertEquals(customer.getTitle(), customerAUpdateMsg.getTitle());

        List<RoleProto> roleProtos = edgeImitator.findAllMessagesByType(RoleProto.class);
        Assert.assertEquals(2, roleProtos.size());
    }

    protected void changeEdgeOwnerFromCustomerToCustomer(Customer previousCustomer, Customer newCustomer) throws Exception {
        edgeImitator.expectMessageAmount(4);
        doPost("/api/owner/CUSTOMER/" + newCustomer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        List<CustomerUpdateMsg> customerMsgs = edgeImitator.findAllMessagesByType(CustomerUpdateMsg.class);
        Assert.assertEquals(2, customerMsgs.size());

        CustomerUpdateMsg previousCustomerDeleteMsg = customerMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, previousCustomerDeleteMsg.getMsgType());
        Assert.assertEquals(previousCustomer.getUuidId().getMostSignificantBits(), previousCustomerDeleteMsg.getIdMSB());
        Assert.assertEquals(previousCustomer.getUuidId().getLeastSignificantBits(), previousCustomerDeleteMsg.getIdLSB());

        CustomerUpdateMsg newCustomerUpdateMsg =  customerMsgs.get(1);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, newCustomerUpdateMsg.getMsgType());
        Assert.assertEquals(newCustomer.getUuidId().getMostSignificantBits(), newCustomerUpdateMsg.getIdMSB());
        Assert.assertEquals(newCustomer.getUuidId().getLeastSignificantBits(), newCustomerUpdateMsg.getIdLSB());
        Assert.assertEquals(newCustomer.getTitle(), newCustomerUpdateMsg.getTitle());

        List<RoleProto> roleProtos = edgeImitator.findAllMessagesByType(RoleProto.class);
        Assert.assertEquals(2, roleProtos.size());
    }

    protected void changeEdgeOwnerFromCustomerToTenant(Customer customer) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<CustomerUpdateMsg> customerDeleteMsgs = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerDeleteMsgs.isPresent());
        CustomerUpdateMsg customerADeleteMsg = customerDeleteMsgs.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerADeleteMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), customerADeleteMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), customerADeleteMsg.getIdLSB());
    }

    protected void changeEdgeOwnerFromTenantToSubCustomer(Customer parentCustomer, Customer customer) throws Exception {
        edgeImitator.expectMessageAmount(6);
        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        List<CustomerUpdateMsg> customerUpdateMsgs = edgeImitator.findAllMessagesByType(CustomerUpdateMsg.class);
        Assert.assertEquals(2, customerUpdateMsgs.size());
        CustomerUpdateMsg customerAUpdateMsg = customerUpdateMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerAUpdateMsg.getMsgType());
        Assert.assertEquals(parentCustomer.getUuidId().getMostSignificantBits(), customerAUpdateMsg.getIdMSB());
        Assert.assertEquals(parentCustomer.getUuidId().getLeastSignificantBits(), customerAUpdateMsg.getIdLSB());
        Assert.assertEquals(parentCustomer.getTitle(), customerAUpdateMsg.getTitle());
        testAutoGeneratedCodeByProtobuf(customerAUpdateMsg);
        CustomerUpdateMsg subCustomerAUpdateMsg = customerUpdateMsgs.get(1);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, subCustomerAUpdateMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), subCustomerAUpdateMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), subCustomerAUpdateMsg.getIdLSB());
        Assert.assertEquals(customer.getTitle(), subCustomerAUpdateMsg.getTitle());

        List<RoleProto> roleProtos = edgeImitator.findAllMessagesByType(RoleProto.class);
        Assert.assertEquals(4, roleProtos.size());
    }

    protected void changeEdgeOwnerFromSubCustomerToTenant(Customer parentCustomer, Customer customer) throws Exception {
        edgeImitator.expectMessageAmount(2);
        doPost("/api/owner/TENANT/" + tenantId.getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        List<CustomerUpdateMsg> customerDeleteMsgs = edgeImitator.findAllMessagesByType(CustomerUpdateMsg.class);
        Assert.assertEquals(2, customerDeleteMsgs.size());
        CustomerUpdateMsg customerADeleteMsg = customerDeleteMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerADeleteMsg.getMsgType());
        Assert.assertEquals(parentCustomer.getUuidId().getMostSignificantBits(), customerADeleteMsg.getIdMSB());
        Assert.assertEquals(parentCustomer.getUuidId().getLeastSignificantBits(), customerADeleteMsg.getIdLSB());
        CustomerUpdateMsg subCustomerADeleteMsg = customerDeleteMsgs.get(1);
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, subCustomerADeleteMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), subCustomerADeleteMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), subCustomerADeleteMsg.getIdLSB());
    }

    protected void changeEdgeOwnerFromSubCustomerToCustomer(Customer parentCustomer, Customer customer) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doPost("/api/owner/CUSTOMER/" + parentCustomer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<CustomerUpdateMsg> customerDeleteMsgOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerDeleteMsgOpt.isPresent());
        CustomerUpdateMsg customerDeleteMsg = customerDeleteMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerDeleteMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), customerDeleteMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), customerDeleteMsg.getIdLSB());
    }

    protected EntityGroup createEntityGroupAndAssignToEdge(EntityType groupType, String groupName, EntityId ownerId) throws Exception {
        edgeImitator.expectMessageAmount(1);
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(groupType);
        entityGroup.setName(groupName);
        entityGroup.setOwnerId(ownerId);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + savedEntityGroup.getId().toString() + "/" + groupType.name(), EntityGroup.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityGroupUpdateMsg);
        EntityGroupUpdateMsg entityGroupUpdateMsg = (EntityGroupUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, entityGroupUpdateMsg.getMsgType());
        Assert.assertEquals(savedEntityGroup.getUuidId().getMostSignificantBits(), entityGroupUpdateMsg.getIdMSB());
        Assert.assertEquals(savedEntityGroup.getUuidId().getLeastSignificantBits(), entityGroupUpdateMsg.getIdLSB());
        Assert.assertEquals(savedEntityGroup.getName(), entityGroupUpdateMsg.getName());
        Assert.assertEquals(savedEntityGroup.getType().name(), entityGroupUpdateMsg.getType());
        testAutoGeneratedCodeByProtobuf(entityGroupUpdateMsg);
        return savedEntityGroup;
    }

    protected void unAssignEntityGroupFromEdge(EntityGroup entityGroup) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + entityGroup.getUuidId().toString() + "/" + entityGroup.getType().name(), EntityGroup.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityGroupUpdateMsg);
        EntityGroupUpdateMsg entityGroupUpdateMsg = (EntityGroupUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, entityGroupUpdateMsg.getMsgType());
        Assert.assertEquals(entityGroup.getUuidId().getMostSignificantBits(), entityGroupUpdateMsg.getIdMSB());
        Assert.assertEquals(entityGroup.getUuidId().getLeastSignificantBits(), entityGroupUpdateMsg.getIdLSB());
    }

    protected void validateThatEntityGroupAssignedToEdge(EntityGroupId entityGroupId, EntityType groupType) throws Exception {
        validateThatEntityGroupAssignedOrNotToEdge(entityGroupId, groupType, true);
    }

    protected void validateThatEntityGroupNotAssignedToEdge(EntityGroupId entityGroupId, EntityType groupType) throws Exception {
        validateThatEntityGroupAssignedOrNotToEdge(entityGroupId, groupType, false);
    }

    private void validateThatEntityGroupAssignedOrNotToEdge(EntityGroupId entityGroupId, EntityType groupType, boolean assigned) throws Exception {
        List<EntityGroupInfo> entityGroupInfos =
                JacksonUtil.convertValue(doGet("/api/allEntityGroups/edge/" + edge.getUuidId() + "/" + groupType.name(), JsonNode.class), new TypeReference<>() {});
        Assert.assertNotNull(entityGroupInfos);
        List<EntityGroupId> entityGroupIds = entityGroupInfos.stream().map(EntityGroup::getId).collect(Collectors.toList());
        Assert.assertEquals(assigned, entityGroupIds.contains(entityGroupId));
    }

    protected List<EntityGroupInfo> getEntityGroupsByOwnerAndType(EntityId ownerId, EntityType groupType) throws Exception {
        return JacksonUtil.convertValue(
                        doGet("/api/entityGroups/" + ownerId.getEntityType() + "/" + ownerId.getId() + "/" + groupType.name(), JsonNode.class),
                        new TypeReference<>() {});
    }

    protected void addEntitiesToEntityGroup(List<EntityId> entityIds, EntityGroupId entityGroupId) throws Exception {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        doPost("/api/entityGroup/" + entityGroupId.getId() + "/addEntities", entityIdsArray);
    }

    protected void deleteEntitiesFromEntityGroup(List<EntityId> entityIds, EntityGroupId entityGroupId) throws Exception {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        doPost("/api/entityGroup/" + entityGroupId.getId() + "/deleteEntities", entityIdsArray);
    }

    protected EntityGroupInfo findGroupByOwnerIdTypeAndName(EntityId ownerId, EntityType groupType, String name) throws Exception {
        List<EntityGroupInfo> groupsList = getEntityGroupsByOwnerAndType(ownerId, groupType);
        EntityGroupInfo result = null;
        for (EntityGroupInfo tmp : groupsList) {
            if (name.equals(tmp.getName())) {
                result = tmp;
            }
        }
        Assert.assertNotNull(result);
        return result;
    }
}
