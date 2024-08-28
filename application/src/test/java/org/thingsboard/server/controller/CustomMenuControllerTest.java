/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMItemLinkType;
import org.thingsboard.server.common.data.menu.CMItemType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.menu.DefaultMenuItem;
import org.thingsboard.server.common.data.menu.HomeMenuItem;
import org.thingsboard.server.common.data.menu.HomeMenuItemType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.menu.CustomMenuDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class CustomMenuControllerTest extends AbstractControllerTest {

    @Autowired
    private CustomMenuDao customMenuDao;

    protected static final String SECOND_TENANT_ADMIN_EMAIL = "secondtesttenant@thingsboard.org";
    protected static final String CUSTOMER_B_USER_EMAIL = "testcustomerB@thingsboard.org";
    protected static final String CUSTOMER_B_USER_PASSWORD = "customerB";

    private static CustomMenu systemSystemMenu;
    private static CustomMenu systemTenantMenu;
    private static CustomMenu systemCustomerMenu;
    private static List<UUID> idsToRemove = new ArrayList<>();
    protected User secondTenantAdminUser;

    @Before
    public void setup() throws Exception {
        loginSysAdmin();
        systemSystemMenu = doPost("/api/customMenu", createDefaultCustomMenu("System Menu", CMScope.SYSTEM), CustomMenu.class);
        idsToRemove.add(systemSystemMenu.getUuidId());
        systemTenantMenu = doPost("/api/customMenu", createDefaultCustomMenu("Tenant Menu", CMScope.TENANT), CustomMenu.class);
        idsToRemove.add(systemTenantMenu.getUuidId());
        systemCustomerMenu = doPost("/api/customMenu", createDefaultCustomMenu("Customer Menu", CMScope.CUSTOMER), CustomMenu.class);
        idsToRemove.add(systemCustomerMenu.getUuidId());

        loginTenantAdmin();
        secondTenantAdminUser = new User();
        secondTenantAdminUser.setAuthority(Authority.TENANT_ADMIN);
        secondTenantAdminUser.setTenantId(tenantId);
        secondTenantAdminUser.setEmail(SECOND_TENANT_ADMIN_EMAIL);
        secondTenantAdminUser = createUser(secondTenantAdminUser, TENANT_ADMIN_PASSWORD);

        Customer customerB = new Customer();
        customerB.setTitle("Customer B");
        customerB.setTenantId(tenantId);
        Customer savedCustomer = doPost("/api/customer", customerB, Customer.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail(CUSTOMER_B_USER_EMAIL);
        createUser(customerUser, CUSTOMER_B_USER_PASSWORD);
    }

    @After
    public void teardown() throws Exception {
        customMenuDao.removeAllByIds(idsToRemove);
        idsToRemove = new ArrayList<>();
    }

    @Test
    public void testCreateAndUpdateTenantMenu() throws Exception {
        loginTenantAdmin();

        CustomMenu tenantMenu = new CustomMenu();
        tenantMenu.setName(RandomStringUtils.randomAlphabetic(10));
        tenantMenu.setScope(CMScope.TENANT);
        tenantMenu.setAssigneeType(CMAssigneeType.USERS);
        tenantMenu = doPost("/api/customMenu", tenantMenu, CustomMenu.class);
        idsToRemove.add(tenantMenu.getUuidId());

        // get by id
        CustomMenuInfo retrieved = doGet("/api/customMenu/" + tenantMenu.getId() + "/info", CustomMenuInfo.class);
        assertThat(retrieved).isEqualTo(new CustomMenuInfo(tenantMenu));

        //update name
        String newName = "new custom menu name";
        doPut("/api/customMenu/" + tenantMenu.getId() + "/name", newName);
        CustomMenuInfo retrievedAfterUpdate = doGet("/api/customMenu/" + tenantMenu.getId() + "/info", CustomMenuInfo.class);
        tenantMenu.setName(newName);
        assertThat(retrievedAfterUpdate).isEqualTo(new CustomMenuInfo(tenantMenu));
    }

    @Test
    public void testDefaultCustomerMenuHierarchy() throws Exception {
        loginTenantAdmin();
        CustomMenuInfo tenantDefaultMenu = createDefaultCustomMenu("Tenant level customer menu ", CMScope.CUSTOMER);
        tenantDefaultMenu = doPost("/api/customMenu", tenantDefaultMenu, CustomMenu.class);

        CustomMenuConfig tenantMenuConfig = putRandomMenuConfig(tenantDefaultMenu.getId());

        loginCustomerAdminUser();
        CustomMenuInfo customerDefaultMenu = createDefaultCustomMenu("Test customer menu", CMScope.CUSTOMER);
        customerDefaultMenu = doPost("/api/customMenu", customerDefaultMenu, CustomMenu.class);

        CustomMenuConfig customerDefaultConfig = putRandomMenuConfig(customerDefaultMenu.getId());
        CustomMenuConfig currentCustomerMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentCustomerMenu).isEqualTo(customerDefaultConfig);

        loginSubCustomerAdminUser();
        CustomMenuInfo subCustomerMenu = createDefaultCustomMenu("Test subcustomer menu", CMScope.CUSTOMER);
        subCustomerMenu = doPost("/api/customMenu", subCustomerMenu, CustomMenu.class);

        CustomMenuConfig subCustomerDefaultConfig = putRandomMenuConfig(subCustomerMenu.getId());
        CustomMenuConfig currentSubCustomerMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentSubCustomerMenu).isEqualTo(subCustomerDefaultConfig);

        // delete subcustomer default menu
        doDelete("/api/customMenu/" + subCustomerMenu.getId());
        CustomMenuConfig currentSubCustomerMenu2 = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentSubCustomerMenu2).isEqualTo(customerDefaultConfig);

        loginCustomerAdminUser();
        // delete customer default menu
        doDelete("/api/customMenu/" + customerDefaultMenu.getId());

        CustomMenuConfig currentCustomerMenu2 = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentCustomerMenu2).isEqualTo(tenantMenuConfig);

        loginTenantAdmin();
        // delete tenant default menu
        doDelete("/api/customMenu/" + tenantDefaultMenu.getId());
        String currentTenantMenu2 = doGet("/api/customMenu", String.class);
        assertThat(currentTenantMenu2).isEmpty();
    }

    @Test
    public void testOverrideTenantDefaultMenu() throws Exception {
        loginTenantAdmin();
        CustomMenuInfo tenantDefaultMenu = createDefaultCustomMenu("Test tenant menu", CMScope.TENANT);
        tenantDefaultMenu = doPost("/api/customMenu", tenantDefaultMenu, CustomMenu.class);
        idsToRemove.add(tenantDefaultMenu.getUuidId());

        CustomMenuInfo newTenantDefaultMenu = createDefaultCustomMenu("Test tenant menu2", CMScope.TENANT);
        String errorMessage = getErrorMessage(doPost("/api/customMenu", newTenantDefaultMenu)
                .andExpect(status().isConflict()));
        assertThat(errorMessage).isEqualTo("There is already default menu for scope TENANT");

        CustomMenu newDefaultCustomMenu = doPost("/api/customMenu?force=true", newTenantDefaultMenu, CustomMenu.class);
        assertThat(newDefaultCustomMenu.getAssigneeType()).isEqualTo(CMAssigneeType.ALL);

        CustomMenu updatedMenu = doGet("/api/customMenu/" + tenantDefaultMenu.getId() + "/info", CustomMenu.class);
        assertThat(updatedMenu.getAssigneeType()).isEqualTo(CMAssigneeType.NO_ASSIGN);
    }

    @Test
    public void testShouldNotDeleteSystemDefaultMenu() throws Exception {
        loginSysAdmin();
        String errorMessage = getErrorMessage(doDelete("/api/customMenu/" + systemSystemMenu.getId())
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("System default menu can not be deleted");

        errorMessage = getErrorMessage(doDelete("/api/customMenu/" + systemTenantMenu.getId())
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("System default menu can not be deleted");

        errorMessage = getErrorMessage(doDelete("/api/customMenu/" + systemCustomerMenu.getId())
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("System default menu can not be deleted");
    }

    @Test
    public void testCreateTenantMenuWithUserList() throws Exception {
        loginTenantAdmin();

        CustomMenu defaultTenantMenu = new CustomMenu();
        defaultTenantMenu.setName(RandomStringUtils.randomAlphabetic(10));
        defaultTenantMenu.setScope(CMScope.TENANT);
        defaultTenantMenu.setAssigneeType(CMAssigneeType.ALL);
        defaultTenantMenu = doPost("/api/customMenu", defaultTenantMenu, CustomMenu.class);
        idsToRemove.add(defaultTenantMenu.getUuidId());

        CustomMenuConfig defaultTenantMenuConfig = putRandomMenuConfig(defaultTenantMenu.getId());

        CustomMenu menuForSpecificUsers = new CustomMenu();
        menuForSpecificUsers.setName(RandomStringUtils.randomAlphabetic(10));
        menuForSpecificUsers.setScope(CMScope.TENANT);
        menuForSpecificUsers.setAssigneeType(CMAssigneeType.USERS);

        menuForSpecificUsers = doPost("/api/customMenu?assignToList=" + tenantAdminUserId, menuForSpecificUsers, CustomMenu.class);
        idsToRemove.add(menuForSpecificUsers.getUuidId());
        CustomMenuConfig tenantMenuConfig = putRandomMenuConfig(menuForSpecificUsers.getId());

        CustomMenuConfig currentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenu).isEqualTo(tenantMenuConfig);

        loginUser(SECOND_TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD);
        CustomMenuConfig secondTenantAdminCurrentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(secondTenantAdminCurrentMenu).isEqualTo(defaultTenantMenuConfig);
    }

    @Test
    public void testShouldNotCreateTenantMenuWithCustomersAssigneeType() throws Exception {
        loginTenantAdmin();

        CustomMenu tenantMenu = new CustomMenu();
        tenantMenu.setName(RandomStringUtils.randomAlphabetic(10));
        tenantMenu.setScope(CMScope.TENANT);
        tenantMenu.setAssigneeType(CMAssigneeType.CUSTOMERS);
        String errorMessage = getErrorMessage(doPost("/api/customMenu", tenantMenu)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("Tenant custom menu can not be assigned to customers");
    }

    @Test
    public void testCreateCustomerMenuWithUserList() throws Exception {
        loginTenantAdmin();

        CustomMenu defaultCustomerMenu = new CustomMenu();
        defaultCustomerMenu.setName(RandomStringUtils.randomAlphabetic(10));
        defaultCustomerMenu.setScope(CMScope.CUSTOMER);
        defaultCustomerMenu.setAssigneeType(CMAssigneeType.ALL);
        defaultCustomerMenu = doPost("/api/customMenu", defaultCustomerMenu, CustomMenu.class);
        idsToRemove.add(defaultCustomerMenu.getUuidId());
        CustomMenuConfig defaultCustomerMenuConfig = putRandomMenuConfig(defaultCustomerMenu.getId());

        CustomMenu menuForSpecificUsers = new CustomMenu();
        menuForSpecificUsers.setName(RandomStringUtils.randomAlphabetic(10));
        menuForSpecificUsers.setScope(CMScope.CUSTOMER);
        menuForSpecificUsers.setAssigneeType(CMAssigneeType.USERS);

        menuForSpecificUsers = doPost("/api/customMenu?assignToList=" + customerUserId, menuForSpecificUsers, CustomMenu.class);
        idsToRemove.add(menuForSpecificUsers.getUuidId());
        CustomMenuConfig customerMenuConfig = putRandomMenuConfig(menuForSpecificUsers.getId());

        loginCustomerUser();
        CustomMenuConfig customerUserMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(customerUserMenu).isEqualTo(customerMenuConfig);

        loginCustomerAdminUser();
        CustomMenuConfig currentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenu).isEqualTo(defaultCustomerMenuConfig);
    }

    @Test
    public void testCreateCustomerMenuWithCustomerList() throws Exception {
        loginTenantAdmin();

        CustomMenu defaultCustomerMenu = new CustomMenu();
        defaultCustomerMenu.setName(RandomStringUtils.randomAlphabetic(10));
        defaultCustomerMenu.setScope(CMScope.CUSTOMER);
        defaultCustomerMenu.setAssigneeType(CMAssigneeType.ALL);
        defaultCustomerMenu = doPost("/api/customMenu", defaultCustomerMenu, CustomMenu.class);
        idsToRemove.add(defaultCustomerMenu.getUuidId());
        CustomMenuConfig defaultCustomerMenuConfig = putRandomMenuConfig(defaultCustomerMenu.getId());

        CustomMenu menuForSpecificCustomer = new CustomMenu();
        menuForSpecificCustomer.setName(RandomStringUtils.randomAlphabetic(10));
        menuForSpecificCustomer.setScope(CMScope.CUSTOMER);
        menuForSpecificCustomer.setAssigneeType(CMAssigneeType.CUSTOMERS);

        menuForSpecificCustomer = doPost("/api/customMenu?assignToList=" + customerId, menuForSpecificCustomer, CustomMenu.class);
        idsToRemove.add(menuForSpecificCustomer.getUuidId());
        CustomMenuConfig customerMenuConfig = putRandomMenuConfig(menuForSpecificCustomer.getId());

        loginCustomerUser();
        CustomMenuConfig customerUserMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(customerUserMenu).isEqualTo(customerMenuConfig);

        login(CUSTOMER_B_USER_EMAIL, CUSTOMER_B_USER_PASSWORD);
        CustomMenuConfig currentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenu).isEqualTo(defaultCustomerMenuConfig);
    }

    @Test
    public void testAssignTenantMenu() throws Exception {
        loginTenantAdmin();

        CustomMenu defaultTenantMenu = new CustomMenu();
        defaultTenantMenu.setName(RandomStringUtils.randomAlphabetic(10));
        defaultTenantMenu.setScope(CMScope.TENANT);
        defaultTenantMenu.setAssigneeType(CMAssigneeType.ALL);
        defaultTenantMenu = doPost("/api/customMenu", defaultTenantMenu, CustomMenu.class);
        idsToRemove.add(defaultTenantMenu.getUuidId());

        CustomMenu tenantMenu = new CustomMenu();
        tenantMenu.setName(RandomStringUtils.randomAlphabetic(10));
        tenantMenu.setScope(CMScope.TENANT);
        tenantMenu.setAssigneeType(CMAssigneeType.USERS);

        tenantMenu = doPost("/api/customMenu", tenantMenu, CustomMenu.class);
        idsToRemove.add(tenantMenu.getUuidId());
        CustomMenuConfig tenantMenuConfig = putRandomMenuConfig(tenantMenu.getId());

        doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/USERS", List.of(tenantAdminUserId.getId()));
        CustomMenuConfig currentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenu).isEqualTo(tenantMenuConfig);

        //change assignee to CUSTOMERS
        doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/NO_ASSIGN", List.of());
        String currentMenuAfterUpdate = doGet("/api/customMenu", String.class);
        assertThat(currentMenuAfterUpdate).isEmpty();

        //change assignee to ALL
        String errorMessage = getErrorMessage(doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/ALL", List.of())
                .andExpect(status().isConflict()));
        assertThat(errorMessage).isEqualTo("There is already default menu for scope TENANT");

        //force change assignee to ALL
        doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/ALL?force=true", List.of());
        CustomMenuConfig currentMenuAfterUpdateToALL = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenuAfterUpdateToALL).isEqualTo(tenantMenuConfig);

        CustomMenu updatedDefaultMenu = doGet("/api/customMenu/" + defaultTenantMenu.getId() + "/info", CustomMenu.class);
        assertThat(updatedDefaultMenu.getAssigneeType()).isEqualTo(CMAssigneeType.NO_ASSIGN);
    }

    @Test
    public void testAssignCustomerMenu() throws Exception {
        loginTenantAdmin();

        CustomMenu defaultCustomerMenu = new CustomMenu();
        defaultCustomerMenu.setName(RandomStringUtils.randomAlphabetic(10));
        defaultCustomerMenu.setScope(CMScope.CUSTOMER);
        defaultCustomerMenu.setAssigneeType(CMAssigneeType.ALL);
        defaultCustomerMenu = doPost("/api/customMenu", defaultCustomerMenu, CustomMenu.class);
        idsToRemove.add(defaultCustomerMenu.getUuidId());

        CustomMenu customerMenu = new CustomMenu();
        customerMenu.setName(RandomStringUtils.randomAlphabetic(10));
        customerMenu.setScope(CMScope.CUSTOMER);
        customerMenu.setAssigneeType(CMAssigneeType.USERS);

        customerMenu = doPost("/api/customMenu", customerMenu, CustomMenu.class);
        idsToRemove.add(customerMenu.getUuidId());
        CustomMenuConfig customerMenuConfig = putRandomMenuConfig(customerMenu.getId());

        doPut("/api/customMenu/" + customerMenu.getId() + "/assign/USERS", List.of(customerUserId.getId()));
        loginCustomerUser();
        CustomMenuConfig currentMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenu).isEqualTo(customerMenuConfig);

        //change assignee to CUSTOMERS
        loginTenantAdmin();
        doPut("/api/customMenu/" + customerMenu.getId() + "/assign/NO_ASSIGN", List.of());

        loginCustomerUser();
        String currentMenuAfterUpdate = doGet("/api/customMenu", String.class);
        assertThat(currentMenuAfterUpdate).isEmpty();

        //change assignee to ALL
        loginTenantAdmin();
        String errorMessage = getErrorMessage(doPut("/api/customMenu/" + customerMenu.getId() + "/assign/ALL", List.of())
                .andExpect(status().isConflict()));
        assertThat(errorMessage).isEqualTo("There is already default menu for scope CUSTOMER");

        //force change assignee to ALL
        doPut("/api/customMenu/" + customerMenu.getId() + "/assign/ALL?force=true", List.of());
        CustomMenu updatedDefaultMenu = doGet("/api/customMenu/" + defaultCustomerMenu.getId() + "/info", CustomMenu.class);
        assertThat(updatedDefaultMenu.getAssigneeType()).isEqualTo(CMAssigneeType.NO_ASSIGN);

        loginCustomerUser();
        CustomMenuConfig currentMenuAfterUpdateToALL = doGet("/api/customMenu", CustomMenuConfig.class);
        assertThat(currentMenuAfterUpdateToALL).isEqualTo(customerMenuConfig);
    }

    private CustomMenuConfig putRandomMenuConfig(CustomMenuId customMenuId) throws Exception {
        HomeMenuItem homeMenuItem = new HomeMenuItem();
        homeMenuItem.setId("home");
        homeMenuItem.setHomeType(HomeMenuItemType.DASHBOARD);
        homeMenuItem.setDashboardId(RandomStringUtils.randomAlphabetic(10));

        DefaultMenuItem defaultMenuItem = new DefaultMenuItem();
        defaultMenuItem.setId("users");
        defaultMenuItem.setName(RandomStringUtils.randomAlphabetic(10));
        defaultMenuItem.setVisible(true);

        CustomMenuItem customMenuItem = new CustomMenuItem();
        customMenuItem.setName("Mu new menu item");
        customMenuItem.setMenuItemType(CMItemType.LINK);
        customMenuItem.setLinkType(CMItemLinkType.URL);
        customMenuItem.setUrl(RandomStringUtils.randomAlphabetic(10));
        customMenuItem.setIcon("icon");
        customMenuItem.setVisible(true);

        CustomMenuConfig customMenuConfig = new CustomMenuConfig();
        customMenuConfig.setItems(List.of(homeMenuItem, defaultMenuItem, customMenuItem));

        doPut("/api/customMenu/" + customMenuId.getId() + "/config", customMenuConfig);
        return customMenuConfig;
    }

    private static CustomMenu createDefaultCustomMenu(String name, CMScope scope) {
        CustomMenu customMenu = new CustomMenu();
        customMenu.setName(name);
        customMenu.setScope(scope);
        customMenu.setAssigneeType(CMAssigneeType.ALL);
        return customMenu;
    }

}
