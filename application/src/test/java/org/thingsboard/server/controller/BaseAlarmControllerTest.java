/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAlarmControllerTest extends AbstractControllerTest {

    public static final String TEST_ALARM_TYPE = "Test";

    protected final String CUSTOMER_ADMIN_EMAIL = "testadmincustomer@thingsboard.org";
    protected final String CUSTOMER_ADMIN_PASSWORD = "admincustomer";

    protected final String DIFFERENT_CUSTOMER_ADMIN_EMAIL = "testdiffadmincustomer@thingsboard.org";
    protected final String DIFFERENT_CUSTOMER_ADMIN_PASSWORD = "diffadmincustomer";

    protected Device customerDevice;

    private Role role;
    private EntityGroup entityGroup;
    private GroupPermission groupPermission;

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setType(RoleType.GENERIC);
        role.setName("Test customer administrator");
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));

        this.role = doPost("/api/role", role, Role.class);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Test customer administrators");
        entityGroup.setType(EntityType.USER);
        entityGroup.setOwnerId(customerId);
        this.entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        GroupPermission groupPermission = new GroupPermission(
                tenantId,
                this.entityGroup.getId(),
                this.role.getId(),
                null,
                null,
                false
        );
        this.groupPermission =
                doPost("/api/groupPermission", groupPermission, GroupPermission.class);
    }

    @After
    public void teardown() throws Exception {
        loginSysAdmin();
        deleteDifferentTenant();
        clearCustomerAdminPermissionGroup();
    }

    @Test
    public void testCreateAlarmViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();
        createAlarm(TEST_ALARM_TYPE);
    }

    @Test
    public void testCreateAlarmViaCustomerWithoutPermission() throws Exception {
        loginCustomerUser();
        createAlarmAndReturnAction(TEST_ALARM_TYPE).andExpect(status().isForbidden());
    }

    @Test
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        createAlarm(TEST_ALARM_TYPE);
    }

    @Test
    public void testUpdateAlarmViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());
    }

    @Test
    public void testUpdateAlarmViaCustomerWithoutPermission() throws Exception {
        loginCustomerUser();
        createAlarmAndReturnAction(TEST_ALARM_TYPE).andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());
    }

    @Test
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        alarm.setSeverity(AlarmSeverity.MAJOR);
        loginDifferentTenant();
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
        loginDifferentCustomerAdministrator();
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteAlarmViaCustomerWithPermission() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginCustomerAdministrator();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());
    }

    @Test
    public void testDeleteAlarmViaCustomerWithoutPermission() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginCustomerUser();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentTenant();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteAlarmViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
        loginDifferentCustomerAdministrator();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testClearAlarmViaCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());
    }

    @Test
    public void testClearAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());
    }

    @Test
    public void testAcknowledgeAlarmViaCustomerWithoutPermission() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginCustomerUser();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
    }

    @Test
    public void testAcknowledgeAlarmViaCustomerWithPermission() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginCustomerAdministrator();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());
    }

    @Test
    public void testClearAlarmViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
        loginDifferentCustomerAdministrator();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
    }

    @Test
    public void testClearAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentTenant();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
        loginDifferentCustomerAdministrator();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentTenant();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
    }

    private Alarm createAlarm(String type) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        return alarm;
    }

    private ResultActions createAlarmAndReturnAction(String type) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();

        return doPost("/api/alarm", alarm);
    }

    private void clearCustomerAdminPermissionGroup() throws Exception {
        loginTenantAdmin();
        doDelete("/api/groupPermission/" + groupPermission.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityGroup.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/role/" + role.getUuidId())
                .andExpect(status().isOk());
    }

    private User savedCustomerAdministrator;
    private User savedDifferentCustomerAdministrator;

    private void loginCustomerAdministrator() throws Exception {
        if (savedCustomerAdministrator == null) {
            savedCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    customerId,
                    CUSTOMER_ADMIN_EMAIL,
                    CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedCustomerAdministrator.getEmail(), CUSTOMER_ADMIN_PASSWORD);
    }

    private void loginDifferentCustomerAdministrator() throws Exception {
        if (savedDifferentCustomerAdministrator == null) {
            savedDifferentCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    differentCustomerId,
                    DIFFERENT_CUSTOMER_ADMIN_EMAIL,
                    DIFFERENT_CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedDifferentCustomerAdministrator.getEmail(), DIFFERENT_CUSTOMER_ADMIN_PASSWORD);
    }

    private User createCustomerAdministrator(TenantId tenantId, CustomerId customerId, String email, String pass) throws Exception {
        loginTenantAdmin();

        User user = new User();
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setFirstName("customer");
        user.setLastName("admin");
        user.setAuthority(Authority.CUSTOMER_USER);

        user = createUser(user, pass, entityGroup.getId());
        logout();

        return user;
    }
}
