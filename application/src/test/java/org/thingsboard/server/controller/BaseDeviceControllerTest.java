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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
public abstract class BaseDeviceControllerTest extends AbstractControllerTest {
    static final TypeReference<PageData<Device>> PAGE_DATA_DEVICE_TYPE_REF = new TypeReference<>() {
    };

    ListeningExecutorService executor;

    List<ListenableFuture<Device>> futures;
    PageData<Device> pageData;

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        log.debug("beforeTest");
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
    }

    @After
    public void afterTest() throws Exception {
        log.debug("afterTest...");
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId())
                .andExpect(status().isOk());
        log.debug("afterTest done");
    }

    @Test
    public void testSaveDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

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

        savedDevice.setName("My new device");
        doPost("/api/device", savedDevice, Device.class);

        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    @Test
    public void saveDeviceWithViolationOfValidation() throws Exception {
        Device device = new Device();
        device.setName(RandomStringUtils.randomAlphabetic(300));
        device.setType("default");
        doPost("/api/device", device).andExpect(statusReason(containsString("length of name must be equal or less than 255")));
        device.setName("Normal Name");
        device.setType(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/device", device).andExpect(statusReason(containsString("length of type must be equal or less than 255")));
        device.setType("Normal type");
        device.setLabel(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/device", device).andExpect(statusReason(containsString("length of label must be equal or less than 255")));
    }

    @Test
    public void testUpdateDeviceFromDifferentTenant() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        loginDifferentTenant();
        doPost("/api/device", savedDevice, Device.class, status().isNotFound());
        deleteDifferentTenant();
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
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setName("My device B" + i);
            device.setType("typeB");
            devices.add(doPost("/api/device", device, Device.class));
        }
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
                new TypeReference<List<EntitySubtype>>() {
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

        doDelete("/api/device/" + savedDevice.getId().getId())
                .andExpect(status().isOk());

//        gatewayNotificationsService.onDeviceDeleted(device);

        doGet("/api/device/" + savedDevice.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveDeviceWithEmptyType() throws Exception {
        Device device = new Device();
        device.setName("My device");
        Device savedDevice = doPost("/api/device", device, Device.class);
        Assert.assertEquals("default", savedDevice.getType());
    }

    @Test
    public void testSaveDeviceWithEmptyName() throws Exception {
        Device device = new Device();
        device.setType("default");
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device name should be specified")));
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
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());

        DeviceCredentials foundDeviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() throws Exception {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest());
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
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device credentials type should be specified")));
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
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device credentials id should be specified")));
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
        doPost("/api/device/credentials", newDeviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Unable to update non-existent device credentials")));
    }

    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setDeviceId(new DeviceId(Uuids.timeBased()));
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTenantDevices() throws Exception {
        log.debug("testFindTenantDevices");
        futures = new ArrayList<>(178);
        for (int i = 0; i < 178; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        log.debug("await create devices");
        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        log.debug("start reading");
        List<Device> loadedDevices = new ArrayList<>(178);
        PageLink pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);

            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        log.debug("asserting");
        assertThat(devices).containsExactlyInAnyOrderElementsOf(loadedDevices);
        log.debug("delete devices async");
        deleteEntitiesAsync("/api/device/", loadedDevices, executor).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("done");
    }

    @Test
    public void testFindTenantDevicesByName() throws Exception {
        String title1 = "Device title 1";

        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
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
            String suffix = RandomStringUtils.randomAlphanumeric(15);
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
        String title1 = "Device title 1";
        String type1 = "typeA";
        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
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
            String suffix = RandomStringUtils.randomAlphanumeric(15);
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
        Device assignedDevice = doPost("/api/tenant/" + savedDifferentTenant.getId().getId() + "/device/" + savedDevice.getId().getId(), Device.class);

        doGet("/api/device/" + assignedDevice.getId().getId(), Device.class, status().isNotFound());

        login("tenant9@thingsboard.org", "testPassword1");

        Device foundDevice1 = doGet("/api/device/" + assignedDevice.getId().getId(), Device.class);
        Assert.assertNotNull(foundDevice1);

        doGet("/api/relation?fromId=" + savedDevice.getId().getId() + "&fromType=DEVICE&relationType=Contains&toId=" + savedAnotherDevice.getId().getId() + "&toType=DEVICE", EntityRelation.class, status().isNotFound());

        loginSysAdmin();
        doDelete("/api/tenant/" + savedDifferentTenant.getId().getId())
                .andExpect(status().isOk());
    }
}
