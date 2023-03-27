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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseGroupPermissionControllerTest extends AbstractControllerTest {

    private IdComparator<GroupPermission> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;
    private EntityGroup savedUserGroup;
    private EntityGroup savedDeviceGroup;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        savedTenant = doPost("/api/tenant", getNewTenant(), Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        savedUserGroup = new EntityGroup();
        savedUserGroup.setType(EntityType.USER);
        savedUserGroup.setName("UserGroup");
        savedUserGroup = doPost("/api/entityGroup", savedUserGroup, EntityGroup.class);

        savedDeviceGroup = new EntityGroup();
        savedDeviceGroup.setType(EntityType.DEVICE);
        savedDeviceGroup.setName("DeviceGroup");
        savedDeviceGroup = doPost("/api/entityGroup", savedDeviceGroup, EntityGroup.class);

    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindGroupPermissionById() throws Exception {
        GroupPermission groupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);
        GroupPermission foundGroupPermission = doGet("/api/groupPermission/" + groupPermission.getId().getId().toString(), GroupPermission.class);
        Assert.assertNotNull(foundGroupPermission);
        assertEquals(groupPermission, foundGroupPermission);
    }

    @Test
    public void testSaveGroupPermission() throws Exception {
        GroupPermission groupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);
        Assert.assertNotNull(groupPermission);
        Assert.assertNotNull(groupPermission.getId());
        Assert.assertTrue(groupPermission.getCreatedTime() > 0);
        assertEquals(savedTenant.getId(), groupPermission.getTenantId());
    }

    private GroupPermission getNewSavedGroupPermission(String roleName, EntityGroup userGroup, EntityGroup entityGroup) throws Exception {
        Role savedRole = createRole(roleName);
        savedRole = doPost("/api/role", savedRole, Role.class);
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedRole.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        if (entityGroup != null) {
            groupPermission.setEntityGroupId(entityGroup.getId());
            groupPermission.setEntityGroupType(entityGroup.getType());
        }

        groupPermission = doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        return groupPermission;
    }

    @Test
    public void testDeleteGroupPermission() throws Exception {
        GroupPermission savedGroupPermission = getNewSavedGroupPermission("Test Role", savedUserGroup, null);

        doDelete("/api/groupPermission/" + savedGroupPermission.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/groupPermission/" + savedGroupPermission.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetUserGroupPermissions() throws Exception {
        List<GroupPermission> groupPermissions = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            groupPermissions.add(getNewSavedGroupPermission("Test role " + i, savedUserGroup, null));
        }

        List<GroupPermissionInfo> loadedGroupPermissionsInfo = doGetTyped("/api/userGroup/"+ savedUserGroup.getId()+"/groupPermissions",
                new TypeReference<List<GroupPermissionInfo>>(){});

        List<GroupPermission> loadedGroupPermissions = loadedGroupPermissionsInfo.stream()
                .map(groupPermissionInfo -> new GroupPermission(groupPermissionInfo)).collect(Collectors.toList());

        Collections.sort(groupPermissions, idComparator);
        Collections.sort(loadedGroupPermissions, idComparator);

        assertEquals(groupPermissions, loadedGroupPermissions);
    }

    @Test
    public void testGetEntityGroupPermissions() throws Exception {
        List<GroupPermission> groupPermissions = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            groupPermissions.add(getNewSavedGroupPermission("Test role " + i, savedUserGroup, savedDeviceGroup));
        }

        List<GroupPermissionInfo> loadedGroupPermissionsInfo = doGetTyped("/api/entityGroup/"+ savedDeviceGroup.getId()+"/groupPermissions",
                new TypeReference<List<GroupPermissionInfo>>(){});

        List<GroupPermission> loadedGroupPermissions = loadedGroupPermissionsInfo.stream()
                .map(groupPermissionInfo -> new GroupPermission(groupPermissionInfo)).collect(Collectors.toList());

        Collections.sort(groupPermissions, idComparator);
        Collections.sort(loadedGroupPermissions, idComparator);

        assertEquals(groupPermissions, loadedGroupPermissions);
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(roleName);
        role.setType(RoleType.GROUP);
        return role;
    }

    private Tenant getNewTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        return tenant;
    }
}
