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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.dao.customer.CustomerServiceImpl;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@ContextConfiguration(classes = {BaseDeviceControllerTest.Config.class})
public abstract class BaseDeviceControllerTest extends AbstractControllerTest {
    static final TypeReference<PageData<Device>> PAGE_DATA_DEVICE_TYPE_REF = new TypeReference<>() {
    };

    ListeningExecutorService executor;

    List<ListenableFuture<Device>> futures;
    PageData<Device> pageData;

    private Tenant savedTenant;
    private User tenantAdmin;

    @SpyBean
    private GatewayNotificationsService gatewayNotificationsService;

    @Autowired
    private DeviceDao deviceDao;

    static class Config {
        @Bean
        @Primary
        public DeviceDao deviceDao(DeviceDao deviceDao) {
            return Mockito.mock(DeviceDao.class, AdditionalAnswers.delegatesTo(deviceDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

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

        // edge only - temporary method, to fix public customer tests
        doPost("/api/customer/public");
    }

    @After
    public void afterTest() throws Exception {
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device savedDevice = doPost("/api/device", device, Device.class);

        Device oldDevice = new Device(savedDevice);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
        testNotificationUpdateGatewayNever();

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);
        savedDevice.setName("My new device");
        doPost("/api/device", savedDevice, Device.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(savedDevice, savedDevice,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED, ActionType.UPDATED, 1 , 1, 1);
        testNotificationUpdateGatewayOneTime(savedDevice, oldDevice);

        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    @Test
    public void testSaveDeviceWithCredentials() throws Exception {
        String testToken = "TEST_TOKEN";

        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(testToken);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device savedDevice = readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);

        Device oldDevice = new Device(savedDevice);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
        testNotificationUpdateGatewayNever();

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials foundDeviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertNotNull(foundDeviceCredentials);
        Assert.assertNotNull(foundDeviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), foundDeviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, foundDeviceCredentials.getCredentialsType());
        Assert.assertEquals(testToken, foundDeviceCredentials.getCredentialsId());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        savedDevice.setName("My new device");
        doPost("/api/device", savedDevice, Device.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(savedDevice, savedDevice,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED, ActionType.UPDATED, 1 , 1, 1);
        testNotificationUpdateGatewayOneTime(savedDevice, oldDevice);
    }

    @Test
    public void saveDeviceWithViolationOfValidation() throws Exception {
        Device device = new Device();
        device.setName(StringUtils.randomAlphabetic(300));
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotificationUpdateGatewayNever();
        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        device.setTenantId(savedTenant.getId());
        msgError = msgErrorFieldLength("type");
        device.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotificationUpdateGatewayNever();
        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        msgError = msgErrorFieldLength("label");
        device.setType("Normal type");
        device.setLabel(StringUtils.randomAlphabetic(300));
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testUpdateDeviceFromDifferentTenant() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String savedDeviceIdStr = savedDevice.getId().getId().toString();
        doPost("/api/device", savedDevice)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", savedDeviceIdStr))));

        testNotifyEntityEqualsOneTimeServiceNeverError(savedDevice, savedDifferentTenant.getId(),
                savedDifferentTenantUser.getId(), savedDifferentTenantUser.getEmail(), ActionType.UPDATED,
                new ThingsboardException(msgErrorNoFound("Device", savedDevice.getId().getId().toString()),
                        ThingsboardErrorCode.PERMISSION_DENIED));
        testNotificationUpdateGatewayNever();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doDelete("/api/device/" + savedDeviceIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", savedDeviceIdStr))));

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();

        deleteDifferentTenant();
    }

    @Test
    public void testSaveDeviceWithProfileFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        DeviceProfile differentProfile = createDeviceProfile("Different profile");
        differentProfile = doPost("/api/deviceProfile", differentProfile, DeviceProfile.class);

        loginTenantAdmin();
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(differentProfile.getId());
        doPost("/api/device", device).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device can`t be referencing to device profile from different tenant!")));
    }

    @Test
    public void testSaveDeviceWithFirmwareFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        DeviceProfile differentProfile = createDeviceProfile("Different profile");
        differentProfile = doPost("/api/deviceProfile", differentProfile, DeviceProfile.class);
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(differentProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("title");
        firmwareInfo.setVersion("1.0");
        firmwareInfo.setUrl("test.url");
        firmwareInfo.setUsesUrl(true);
        OtaPackageInfo savedFw = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);

        loginTenantAdmin();
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setFirmwareId(savedFw.getId());
        doPost("/api/device", device).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign firmware from different tenant!")));
    }

    @Test
    public void testSaveDeviceWithSoftwareFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        DeviceProfile differentProfile = createDeviceProfile("Different profile");
        differentProfile = doPost("/api/deviceProfile", differentProfile, DeviceProfile.class);
        SaveOtaPackageInfoRequest softwareInfo = new SaveOtaPackageInfoRequest();
        softwareInfo.setDeviceProfileId(differentProfile.getId());
        softwareInfo.setType(SOFTWARE);
        softwareInfo.setTitle("title");
        softwareInfo.setVersion("1.0");
        softwareInfo.setUrl("test.url");
        softwareInfo.setUsesUrl(true);
        OtaPackageInfo savedSw = doPost("/api/otaPackage", softwareInfo, OtaPackageInfo.class);

        loginTenantAdmin();
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setSoftwareId(savedSw.getId());
        doPost("/api/device", device).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign software from different tenant!")));
    }

    @Test
    public void testFindDeviceById() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertNotNull(foundDevice);
        Assert.assertEquals(savedDevice, foundDevice);
    }

    @Test
    public void testFindDeviceTypesByTenantId() throws Exception {
        // TODO: @voba device profiles are not created on edge at the moment
        doPost("/api/deviceProfile", this.createDeviceProfile("typeA"), DeviceProfile.class);
        doPost("/api/deviceProfile", this.createDeviceProfile("typeB"), DeviceProfile.class);
        doPost("/api/deviceProfile", this.createDeviceProfile("typeC"), DeviceProfile.class);

        List<Device> devices = new ArrayList<>();

        int cntEntity = 3;

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        for (int i = 0; i < cntEntity; i++) {
            Device device = new Device();
            device.setName("My device B" + i);
            device.setType("typeB");
            devices.add(doPost("/api/device", device, Device.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);
        testNotificationUpdateGatewayNever();

        for (int i = 0; i < 7; i++) {
            Device device = new Device();
            device.setName("My device C" + i);
            device.setType("typeC");
            devices.add(doPost("/api/device", device, Device.class));
        }
        for (int i = 0; i < 9; i++) {
            Device device = new Device();
            device.setName("My device A" + i);
            device.setType("typeA");
            devices.add(doPost("/api/device", device, Device.class));
        }
        List<EntitySubtype> deviceTypes = doGetTyped("/api/device/types",
                new TypeReference<>() {
                });

        Assert.assertNotNull(deviceTypes);
        Assert.assertEquals(3, deviceTypes.size());
        Assert.assertEquals("typeA", deviceTypes.get(0).getType());
        Assert.assertEquals("typeB", deviceTypes.get(1).getType());
        Assert.assertEquals("typeC", deviceTypes.get(2).getType());

        deleteEntitiesAsync("/api/device/", devices, executor).get(TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void testDeleteDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doDelete("/api/device/" + savedDevice.getId().getId())
                .andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.DELETED, savedDevice.getId().getId().toString());
        testNotificationDeleteGatewayOneTime(savedDevice);

        EntityId savedDeviceId = savedDevice.getId();
        doGet("/api/device/" + savedDeviceId)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", savedDeviceId.getId().toString()))));
    }

    @Test
    public void testSaveDeviceWithEmptyType() throws Exception {
        Device device = new Device();
        device.setName("My device");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device savedDevice = doPost("/api/device", device, Device.class);
        Assert.assertEquals("default", savedDevice.getType());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceWithEmptyName() throws Exception {
        Device device = new Device();
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device name " + msgErrorShouldBeSpecified;
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindDeviceCredentialsByDeviceId() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
    }

    @Test
    public void testSaveDeviceCredentials() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());

        testNotifyEntityMsgToEdgePushMsgToCoreOneTime(savedDevice, savedDevice.getId(), savedDevice.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED, deviceCredentials);
        testNotificationUpdateGatewayNever();

        DeviceCredentials foundDeviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() throws Exception {
        DeviceCredentials deviceCredentials = new DeviceCredentials();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid entity id")));

        testNotifyEntityNever(deviceCredentials.getDeviceId(), new Device());
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsType() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setCredentialsType(null);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device credentials type " + msgErrorShouldBeSpecified;
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DataValidationException(msgError), deviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsId() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setCredentialsId(null);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device credentials id " + msgErrorShouldBeSpecified;
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DeviceCredentialsValidationException(msgError), deviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveNonExistentDeviceCredentials() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        DeviceCredentials newDeviceCredentials = new DeviceCredentials(new DeviceCredentialsId(Uuids.timeBased()));
        newDeviceCredentials.setCreatedTime(deviceCredentials.getCreatedTime());
        newDeviceCredentials.setDeviceId(deviceCredentials.getDeviceId());
        newDeviceCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
        newDeviceCredentials.setCredentialsId(deviceCredentials.getCredentialsId());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Unable to update non-existent device credentials";
        doPost("/api/device/credentials", newDeviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DeviceCredentialsValidationException(msgError), newDeviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceId deviceTimeBasedId = new DeviceId(Uuids.timeBased());
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setDeviceId(deviceTimeBasedId);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", deviceTimeBasedId.toString()))));

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindTenantDevices() throws Exception {
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }

        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);
        testNotificationUpdateGatewayNever();

        List<Device> loadedDevices = new ArrayList<>(cntEntity);
        PageLink pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);

            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devices).containsExactlyInAnyOrderElementsOf(loadedDevices);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        deleteEntitiesAsync("/api/device/", loadedDevices, executor).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, cntEntity, 1);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindTenantDevicesByName() throws Exception {
        String title1 = "Device title 1";

        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devicesTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        futures = new ArrayList<>(75);
        for (int i = 0; i < 75; i++) {
            Device device = new Device();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devicesTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesTitle1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15, 0, title1);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle1);

        List<Device> loadedDevicesTitle2 = new ArrayList<>(75);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle2);

        deleteEntitiesAsync("/api/device/", loadedDevicesTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/device/", loadedDevicesTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantDevicesByType() throws Exception {

        // TODO: @voba device profiles are not created on edge at the moment
        doPost("/api/deviceProfile", this.createDeviceProfile("typeA"), DeviceProfile.class);
        doPost("/api/deviceProfile", this.createDeviceProfile("typeB"), DeviceProfile.class);

        String title1 = "Device title 1";
        String type1 = "typeA";
        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }
        List<Device> devicesType1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        String type2 = "typeB";
        futures = new ArrayList<>(75);
        for (int i = 0; i < 75; i++) {
            Device device = new Device();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }

        List<Device> devicesType2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesType1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>(75);
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesType2);

        deleteEntitiesAsync("/api/device/", loadedDevicesType1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/device/", loadedDevicesType2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testAssignDeviceToTenant() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Device anotherDevice = new Device();
        anotherDevice.setName("My device1");
        anotherDevice.setType("default");
        Device savedAnotherDevice = doPost("/api/device", anotherDevice, Device.class);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(savedDevice.getId());
        relation.setTo(savedAnotherDevice.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType("Contains");
        doPost("/api/relation", relation).andExpect(status().isOk());

        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Different tenant");
        Tenant savedDifferentTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedDifferentTenant);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedDifferentTenant.getId());
        user.setEmail("tenant9@thingsboard.org");
        user.setFirstName("Sam");
        user.setLastName("Downs");

        createUserAndLogin(user, "testPassword1");

        login("tenant2@thingsboard.org", "testPassword1");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device assignedDevice = doPost("/api/tenant/" + savedDifferentTenant.getId().getId() + "/device/"
                + savedDevice.getId().getId(), Device.class);

        doGet("/api/device/" + assignedDevice.getId().getId())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", assignedDevice.getId().getId().toString()))));

        testNotifyEntityOneTimeMsgToEdgeServiceNever(assignedDevice, assignedDevice.getId(), assignedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_TENANT, savedDifferentTenant.getId().getId().toString(), savedDifferentTenant.getTitle());
        testNotificationUpdateGatewayNever();

        login("tenant9@thingsboard.org", "testPassword1");

        Device foundDevice1 = doGet("/api/device/" + assignedDevice.getId().getId(), Device.class);
        Assert.assertNotNull(foundDevice1);

        doGet("/api/relation?fromId=" + savedDevice.getId().getId() + "&fromType=DEVICE&relationType=Contains&toId="
                + savedAnotherDevice.getId().getId() + "&toType=DEVICE")
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", savedAnotherDevice.getId().getId().toString()))));

        loginSysAdmin();
        doDelete("/api/tenant/" + savedDifferentTenant.getId().getId())
                .andExpect(status().isOk());
    }

    protected void testNotificationUpdateGatewayOneTime(Device device, Device oldDevice) {
        Mockito.verify(gatewayNotificationsService, times(1)).onDeviceUpdated(Mockito.eq(device), Mockito.eq(oldDevice));
    }

    protected void testNotificationUpdateGatewayNever() {
        Mockito.verify(gatewayNotificationsService, never()).onDeviceUpdated(Mockito.any(Device.class), Mockito.any(Device.class));
    }

    protected void testNotificationDeleteGatewayOneTime(Device device) {
        Mockito.verify(gatewayNotificationsService, times(1)).onDeviceDeleted(device);
    }

    protected void testNotificationDeleteGatewayNever() {
        Mockito.verify(gatewayNotificationsService, never()).onDeviceDeleted(Mockito.any(Device.class));
    }

    @Test
    public void testDeleteDashboardWithDeleteRelationsOk() throws Exception {
        DeviceId deviceId = createDevice("Device for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), deviceId, "/api/device/" + deviceId);
    }

    @Ignore
    @Test
    public void testDeleteDeviceExceptionWithRelationsTransactional() throws Exception {
        DeviceId deviceId = createDevice("Device for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(deviceDao, savedTenant.getId(), deviceId, "/api/device/" + deviceId);
    }

    @Test
    public void testBulkImportDeviceWithoutCredentials() throws Exception {
        // edge only - device profile are not created - uploaded from cloud
        DeviceProfile deviceProfile = this.createDeviceProfile("some_type");
        doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        String deviceName = "some_device";
        String deviceType = "some_type";
        BulkImportRequest request = new BulkImportRequest();
        request.setFile(String.format("NAME,TYPE\n%s,%s", deviceName, deviceType));
        BulkImportRequest.Mapping mapping = new BulkImportRequest.Mapping();
        BulkImportRequest.ColumnMapping name = new BulkImportRequest.ColumnMapping();
        name.setType(BulkImportColumnType.NAME);
        BulkImportRequest.ColumnMapping type = new BulkImportRequest.ColumnMapping();
        type.setType(BulkImportColumnType.TYPE);
        List<BulkImportRequest.ColumnMapping> columns = new ArrayList<>();
        columns.add(name);
        columns.add(type);

        mapping.setColumns(columns);
        mapping.setDelimiter(',');
        mapping.setUpdate(true);
        mapping.setHeader(true);
        request.setMapping(mapping);

        BulkImportResult<Device> deviceBulkImportResult = doPostWithTypedResponse("/api/device/bulk_import", request, new TypeReference<>() {});

        Assert.assertEquals(1, deviceBulkImportResult.getCreated().get());
        Assert.assertEquals(0, deviceBulkImportResult.getErrors().get());
        Assert.assertEquals(0, deviceBulkImportResult.getUpdated().get());
        Assert.assertTrue(deviceBulkImportResult.getErrorsList().isEmpty());

        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);

        Assert.assertNotNull(savedDevice);
        Assert.assertEquals(deviceName, savedDevice.getName());
        Assert.assertEquals(deviceType, savedDevice.getType());

        DeviceCredentials savedCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertNotNull(savedCredentials);
        Assert.assertNotNull(savedCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), savedCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, savedCredentials.getCredentialsType());
        Assert.assertNotNull(savedCredentials.getCredentialsId());
        Assert.assertEquals(20, savedCredentials.getCredentialsId().length());

        deviceBulkImportResult = doPostWithTypedResponse("/api/device/bulk_import", request, new TypeReference<>() {});

        Assert.assertEquals(0, deviceBulkImportResult.getCreated().get());
        Assert.assertEquals(0, deviceBulkImportResult.getErrors().get());
        Assert.assertEquals(1, deviceBulkImportResult.getUpdated().get());
        Assert.assertTrue(deviceBulkImportResult.getErrorsList().isEmpty());

        Device updatedDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(savedDevice, updatedDevice);

        DeviceCredentials updatedCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertEquals(savedCredentials, updatedCredentials);
    }

    @Test
    public void testBulkImportDeviceWithUpdateFalse() throws Exception {
        String deviceName = "firstDevice";
        String attributeValue = "testValue";
        BulkImportRequest request = new BulkImportRequest();
        request.setFile(String.format("NAME,TYPE,DATA\n%s,%s,%s", deviceName, "thermostat", attributeValue));
        BulkImportRequest.Mapping mapping = new BulkImportRequest.Mapping();
        BulkImportRequest.ColumnMapping name = new BulkImportRequest.ColumnMapping();
        name.setType(BulkImportColumnType.NAME);
        BulkImportRequest.ColumnMapping type = new BulkImportRequest.ColumnMapping();
        type.setType(BulkImportColumnType.TYPE);
        BulkImportRequest.ColumnMapping attribute = new BulkImportRequest.ColumnMapping();
        attribute.setType(BulkImportColumnType.SERVER_ATTRIBUTE);
        attribute.setKey("DATA");
        List<BulkImportRequest.ColumnMapping> columns = new ArrayList<>();
        columns.add(name);
        columns.add(type);
        columns.add(attribute);

        mapping.setColumns(columns);
        mapping.setDelimiter(',');
        mapping.setUpdate(true);
        mapping.setHeader(true);
        request.setMapping(mapping);

        //import device
        doPostWithTypedResponse("/api/device/bulk_import", request, new TypeReference<>() {});
        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);

        //check server attribute value
        List<Map<String, Object>> values = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId() +
                "/values/attributes/SERVER_SCOPE", new TypeReference<>() {
        });
        Map<String, Object> serverAttribute = values.stream().filter(att -> att.get("key").equals("DATA")).findFirst().get();
        Assert.assertEquals(attributeValue, serverAttribute.get("value"));

        //update server attribute value
        String newAttributeValue = "testValue2";
        JsonNode content = JacksonUtil.toJsonNode("{\"DATA\": \"" + newAttributeValue + "\"}");
        doPost("/api/plugins/telemetry/" + EntityType.DEVICE.name() + "/" + savedDevice.getUuidId() + "/SERVER_SCOPE", content)
                .andExpect(status().isOk());

        //reimport devices
        String deviceName2 = "secondDevice";
        String attributeValue2 = "testValue3";

        request.setFile(String.format("NAME,TYPE,DATA\n%s,%s,%s\n%s,%s,%s", deviceName, "thermostat", attributeValue,
                deviceName2, "thermostat", attributeValue2));
        mapping.setUpdate(false);
        doPostWithTypedResponse("/api/device/bulk_import", request, new TypeReference<>() {});
        Device savedDevice2 = doGet("/api/tenant/devices?deviceName=" + deviceName2, Device.class);

        //check attribute value was not changed after reimport
        List<Map<String, Object>> values2 = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId() +
                "/values/attributes/SERVER_SCOPE", new TypeReference<>() {
        });
        Map<String, Object> retrievedServerAttribute2 = values2.stream().filter(att -> att.get("key").equals("DATA")).findFirst().get();
        Assert.assertEquals(newAttributeValue, retrievedServerAttribute2.get("value"));

        //check attribute for second device
        List<Map<String, Object>> values3 = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice2.getId() +
                "/values/attributes/SERVER_SCOPE", new TypeReference<>() {
        });
        Map<String, Object> retrievedServerAttribute3 = values3.stream().filter(att -> att.get("key").equals("DATA")).findFirst().get();
        Assert.assertEquals(attributeValue2, retrievedServerAttribute3.get("value"));
    }

    private Device createDevice(String name) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        return doPost("/api/device", device, Device.class);
    }
}
