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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class CustomMenuControllerTest extends AbstractControllerTest {

    protected final String CUSTOMER_ADMIN_EMAIL = "testadmincustomer@thingsboard.org";
    protected final String CUSTOMER_ADMIN_PASSWORD = "admincustomer";

    protected final String SUB_CUSTOMER_ADMIN_EMAIL = "subcustomer@thingsboard.org";
    protected final String SUB_CUSTOMER_ADMIN_PASSWORD = "subcustomer";

    private Role role;
    private EntityGroup entityGroup;
    private GroupPermission groupPermission;

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

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
        clearCustomerAdminPermissionGroup();
    }

    @Test
    public void testGetCustomMenuByHierarchy() throws Exception {
        //SysAdmin
        loginSysAdmin();

        CustomMenu sysMenu = new CustomMenu();

        CustomMenuItem sysItem = new CustomMenuItem();
        sysItem.setName("System Menu");
        sysMenu.setMenuItems(new ArrayList<>(List.of(sysItem)));

        doPost("/api/customMenu/customMenu", sysMenu);

        CustomMenu foundSysMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundSysMenu);
        Assert.assertEquals(sysMenu, foundSysMenu);

        //TenantAdmin
        loginTenantAdmin();

        foundSysMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundSysMenu);
        Assert.assertEquals(sysMenu, foundSysMenu);

        CustomMenu tenantMenu = new CustomMenu();

        CustomMenuItem tenantItem = new CustomMenuItem();
        tenantItem.setName("Tenant Menu");
        tenantMenu.setMenuItems(new ArrayList<>(List.of(tenantItem)));

        doPost("/api/customMenu/customMenu", tenantMenu);

        CustomMenu foundTenantMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundTenantMenu);
        Assert.assertEquals(tenantMenu, foundTenantMenu);

        //CustomerAdmin
        loginCustomerAdministrator();

        foundTenantMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundTenantMenu);
        Assert.assertEquals(tenantMenu, foundTenantMenu);

        CustomMenu customerMenu = new CustomMenu();

        CustomMenuItem customerItem = new CustomMenuItem();
        customerItem.setName("Customer Menu");
        customerMenu.setMenuItems(new ArrayList<>(List.of(customerItem)));

        doPost("/api/customMenu/customMenu", customerMenu);

        CustomMenu foundCustomerMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundCustomerMenu);
        Assert.assertEquals(customerMenu, foundCustomerMenu);

        Customer subCustomer = new Customer();
        subCustomer.setParentCustomerId(customerId);
        subCustomer.setTitle("Sub Customer");

        Customer savedSubCustomer = doPost("/api/customer", subCustomer, Customer.class);
        createCustomerAdministrator(
                savedCustomerAdministrator.getTenantId(),
                savedSubCustomer.getId(),
                SUB_CUSTOMER_ADMIN_EMAIL,
                SUB_CUSTOMER_ADMIN_PASSWORD
        );

        //SubCustomer
        login(SUB_CUSTOMER_ADMIN_EMAIL, SUB_CUSTOMER_ADMIN_PASSWORD);

        foundCustomerMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundCustomerMenu);
        Assert.assertEquals(customerMenu, foundCustomerMenu);

        CustomMenu subCustomerMenu = new CustomMenu();

        CustomMenuItem subCustomerItem = new CustomMenuItem();
        subCustomerItem.setName("Customer Menu");
        subCustomerMenu.setMenuItems(new ArrayList<>(List.of(subCustomerItem)));

        doPost("/api/customMenu/customMenu", subCustomerMenu);

        CustomMenu foundSubCustomerMenu = doGet("/api/customMenu/customMenu", CustomMenu.class);

        Assert.assertNotNull(foundSubCustomerMenu);
        Assert.assertEquals(subCustomerMenu, foundSubCustomerMenu);
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
        customerAdminUserId = user.getId();
        resetTokens();

        return user;
    }
}
