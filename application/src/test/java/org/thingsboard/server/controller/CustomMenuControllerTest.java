/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
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
    static final TypeReference<PageData<CustomMenuInfo>> PAGE_DATA_CUSTOM_MENU_TYPE_REFERENCE = new TypeReference<>() {
    };

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

        PageLink pageLink = new PageLink(10);
        PageData<CustomMenuInfo> customMenus = doGetTypedWithPageLink("/api/customMenu/infos?", PAGE_DATA_CUSTOM_MENU_TYPE_REFERENCE, pageLink);
        assertThat(customMenus.getData()).hasSize(1);

        // find by scope and assignee type
        PageData<CustomMenuInfo> customMenusByScopeAndAssigneeType = doGetTypedWithPageLink("/api/customMenu/infos?scope=TENANT&assigneeType=USERS&",
                PAGE_DATA_CUSTOM_MENU_TYPE_REFERENCE, pageLink);
        assertThat(customMenusByScopeAndAssigneeType.getData()).hasSize(1);

        // find by another scope
        PageData<CustomMenuInfo> customMenusByAnotherScope = doGetTypedWithPageLink("/api/customMenu/infos?scope=CUSTOMER&",
                PAGE_DATA_CUSTOM_MENU_TYPE_REFERENCE, pageLink);
        assertThat(customMenusByAnotherScope.getData()).hasSize(0);

        // find by another assignee type
        PageData<CustomMenuInfo> customMenusByAnotherAssigneeType = doGetTypedWithPageLink("/api/customMenu/infos?assigneeType=ALL&",
                PAGE_DATA_CUSTOM_MENU_TYPE_REFERENCE, pageLink);
        assertThat(customMenusByAnotherAssigneeType.getData()).hasSize(0);
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
        JsonNode currentCustomerMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentCustomerMenu, customerDefaultConfig);

        loginSubCustomerAdminUser();
        CustomMenuInfo subCustomerMenu = createDefaultCustomMenu("Test subcustomer menu", CMScope.CUSTOMER);
        subCustomerMenu = doPost("/api/customMenu", subCustomerMenu, CustomMenu.class);

        CustomMenuConfig subCustomerDefaultConfig = putRandomMenuConfig(subCustomerMenu.getId());
        JsonNode currentSubCustomerMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentSubCustomerMenu, subCustomerDefaultConfig);

        // check all child menus are visible for customer
        loginCustomerAdminUser();
        CustomMenuInfo subCustomerMenuUnderCustomer = doGet("/api/customMenu/" + subCustomerMenu.getId() + "/info", CustomMenu.class);
        assertThat(subCustomerMenuUnderCustomer.getName()).isEqualTo(subCustomerMenu.getName());

        // check all customer menus are visible for tenant
        loginTenantAdmin();
        CustomMenuInfo subCustomerMenuUnderTenant = doGet("/api/customMenu/" + subCustomerMenu.getId() + "/info", CustomMenu.class);
        assertThat(subCustomerMenuUnderTenant.getName()).isEqualTo(subCustomerMenu.getName());

        // delete subcustomer default menu
        loginCustomerAdminUser();
        doDelete("/api/customMenu/" + subCustomerMenu.getId());
        JsonNode currentSubCustomerMenu2 = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentSubCustomerMenu2, customerDefaultConfig);
        
        loginCustomerAdminUser();
        // delete customer default menu
        doDelete("/api/customMenu/" + customerDefaultMenu.getId());

        JsonNode currentCustomerMenu2 = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentCustomerMenu2, tenantMenuConfig);

        loginTenantAdmin();
        // delete tenant default menu
        doDelete("/api/customMenu/" + tenantDefaultMenu.getId());
        JsonNode currentTenantMenu2 = doGet("/api/customMenu", JsonNode.class);
        assertThat(currentTenantMenu2.get("items")).hasSize(0);
    }

    @Test
    public void testOverrideTenantDefaultMenu() throws Exception {
        loginTenantAdmin();
        CustomMenuInfo tenantDefaultMenu = createDefaultCustomMenu("Test tenant menu", CMScope.TENANT);
        tenantDefaultMenu = doPost("/api/customMenu", tenantDefaultMenu, CustomMenu.class);
        idsToRemove.add(tenantDefaultMenu.getUuidId());

        CustomMenuInfo newTenantDefaultMenu = createDefaultCustomMenu("Test tenant menu2", CMScope.TENANT);
        String errorMessage = getErrorMessage(doPost("/api/customMenu", newTenantDefaultMenu)
                .andExpect(status().isBadRequest()));
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

        //check assignee list
        List<EntityInfo> entityInfos = readResponse(doGet("/api/customMenu/" + menuForSpecificUsers.getId() + "/assigneeList")
                .andExpect(status().isOk()), new TypeReference<List<EntityInfo>>() {
        });
        assertThat(entityInfos).hasSize(1);
        assertThat(entityInfos.get(0).getId()).isEqualTo(tenantAdminUserId);

        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenu, tenantMenuConfig);

        loginUser(SECOND_TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD);
        JsonNode secondTenantAdminCurrentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(secondTenantAdminCurrentMenu, defaultTenantMenuConfig);
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

        //check assignee list
        List<EntityInfo> entityInfos = readResponse(doGet("/api/customMenu/" + menuForSpecificUsers.getId() + "/assigneeList")
                .andExpect(status().isOk()), new TypeReference<List<EntityInfo>>() {
        });
        assertThat(entityInfos).hasSize(1);
        assertThat(entityInfos.get(0).getId()).isEqualTo(customerUserId);

        loginCustomerUser();
        JsonNode customerUserMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(customerUserMenu, customerMenuConfig);

        loginCustomerAdminUser();
        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenu, defaultCustomerMenuConfig);
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
        JsonNode customerUserMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(customerUserMenu, customerMenuConfig);

        login(CUSTOMER_B_USER_EMAIL, CUSTOMER_B_USER_PASSWORD);
        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenu, defaultCustomerMenuConfig);
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
        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenu, tenantMenuConfig);

        //change assignee to CUSTOMERS
        doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/NO_ASSIGN", List.of());
        JsonNode currentMenuAfterUpdate = doGet("/api/customMenu", JsonNode.class);
        assertThat(currentMenuAfterUpdate.get("items")).hasSize(0);

        //change assignee to ALL
        String errorMessage = getErrorMessage(doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/ALL", List.of())
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("There is already default menu for scope TENANT");

        //force change assignee to ALL
        doPut("/api/customMenu/" + tenantMenu.getId() + "/assign/ALL?force=true", List.of());
        JsonNode currentMenuAfterUpdateToALL = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenuAfterUpdateToALL, tenantMenuConfig);

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
        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenu, customerMenuConfig);

        //change assignee to CUSTOMERS
        loginTenantAdmin();
        doPut("/api/customMenu/" + customerMenu.getId() + "/assign/NO_ASSIGN", List.of());

        loginCustomerUser();
        JsonNode currentMenuAfterUpdate = doGet("/api/customMenu", JsonNode.class);
        assertThat(currentMenuAfterUpdate.get("items")).hasSize(0);

        //change assignee to ALL
        loginTenantAdmin();
        String errorMessage = getErrorMessage(doPut("/api/customMenu/" + customerMenu.getId() + "/assign/ALL", List.of())
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).isEqualTo("There is already default menu for scope CUSTOMER");

        //force change assignee to ALL
        doPut("/api/customMenu/" + customerMenu.getId() + "/assign/ALL?force=true", List.of());
        CustomMenu updatedDefaultMenu = doGet("/api/customMenu/" + defaultCustomerMenu.getId() + "/info", CustomMenu.class);
        assertThat(updatedDefaultMenu.getAssigneeType()).isEqualTo(CMAssigneeType.NO_ASSIGN);

        loginCustomerUser();
        JsonNode currentMenuAfterUpdateToALL = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenuAfterUpdateToALL, customerMenuConfig);
    }

    @Test
    public void testCustomMenuEtag() throws Exception {
        loginTenantAdmin();
        CustomMenuInfo tenantDefaultMenu = createDefaultCustomMenu("Tenant level customer menu ", CMScope.CUSTOMER);
        tenantDefaultMenu = doPost("/api/customMenu", tenantDefaultMenu, CustomMenu.class);
        putRandomMenuConfig(tenantDefaultMenu.getId());

        loginCustomerAdminUser();
        CustomMenuInfo customerDefaultMenu = createDefaultCustomMenu("Test customer menu", CMScope.CUSTOMER);
        customerDefaultMenu = doPost("/api/customMenu", customerDefaultMenu, CustomMenu.class);
        CustomMenuConfig customerDefaultConfig = putRandomMenuConfig(customerDefaultMenu.getId());

        loginSubCustomerAdminUser();
        CustomMenuInfo subCustomerMenu = createDefaultCustomMenu("Test subcustomer menu", CMScope.CUSTOMER);
        subCustomerMenu = doPost("/api/customMenu", subCustomerMenu, CustomMenu.class);
        CustomMenuConfig subCustomerDefaultConfig = putRandomMenuConfig(subCustomerMenu.getId());

        //wait till cache invalidated by tenant id
        Thread.sleep(1000);
        String etag = getUserCustomMenu().getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();
        assertThat(getUserCustomMenu(etag).getResponse().getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());

        //update menu on customer level
        loginCustomerAdminUser();
        putRandomMenuConfig(customerDefaultMenu.getId());

        loginSubCustomerAdminUser();
        MvcResult userMenuAfterUpdateOnCustomerLevel = getUserCustomMenu(etag);
        String eTagAfterUpdate = userMenuAfterUpdateOnCustomerLevel.getResponse().getHeader("ETag");
        assertThat(eTagAfterUpdate).isEqualTo(etag);

        JsonNode userCustomMenu = readResponse(userMenuAfterUpdateOnCustomerLevel, JsonNode.class);
        assertCustomMenuConfig(userCustomMenu, subCustomerDefaultConfig);

        //update menu on tenant level
        loginTenantAdmin();
        putRandomMenuConfig(tenantDefaultMenu.getId());

        loginSubCustomerAdminUser();
        MvcResult userMenuAfterTenantUpdate = getUserCustomMenu(eTagAfterUpdate);
        String eTagAfterTenantUpdate = userMenuAfterTenantUpdate.getResponse().getHeader("ETag");
        assertThat(eTagAfterTenantUpdate).isEqualTo(eTagAfterUpdate);

        JsonNode userCustomMenuAfterUpdateOnTenantLevel = readResponse(userMenuAfterTenantUpdate, JsonNode.class);
        assertCustomMenuConfig(userCustomMenuAfterUpdateOnTenantLevel, subCustomerDefaultConfig);
        assertThat(getUserCustomMenu(eTagAfterTenantUpdate).getResponse().getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
    }

    @Test
    public void testShouldInvalidateCacheAfterAssignListUpdate() throws Exception {
        loginTenantAdmin();

        CustomMenu customMenu = new CustomMenu();
        customMenu.setName(RandomStringUtils.randomAlphabetic(10));
        customMenu.setScope(CMScope.TENANT);
        customMenu.setAssigneeType(CMAssigneeType.NO_ASSIGN);
        customMenu = doPost("/api/customMenu", customMenu, CustomMenu.class);
        idsToRemove.add(customMenu.getUuidId());
        CustomMenuConfig tenantMenuConfig = putRandomMenuConfig(customMenu.getId());

        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertThat(currentMenu.get("items")).isEmpty();

        //assign menu to user
        doPut("/api/customMenu/" + customMenu.getId() + "/assign/USERS", List.of(tenantAdminUserId.getId()));

        JsonNode currentMenuAfterAssign = doGet("/api/customMenu", JsonNode.class);
        assertCustomMenuConfig(currentMenuAfterAssign, tenantMenuConfig);
    }

    @Test
    public void testGetCustomMenuViaPublicCustomer() throws Exception {
        loginCustomerAdminUser();

        EntityGroupInfo dashboardGroup = createSharedPublicEntityGroup(
                "Public dashboard",
                EntityType.DASHBOARD,
                customerId
        );
        String publicId = dashboardGroup.getAdditionalInfo().get("publicCustomerId").asText();

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        JsonNode currentMenu = doGet("/api/customMenu", JsonNode.class);
        assertThat(currentMenu.get("items")).isEmpty();
    }

    private MvcResult getUserCustomMenu() throws Exception {
        return getUserCustomMenu(null);
    }

    private MvcResult getUserCustomMenu(String etag) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (etag != null) {
            headers.setIfNoneMatch(etag);
        }
        return doGet("/api/customMenu", headers).andReturn();
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
        customMenu.setConfig(new CustomMenuConfig());
        return customMenu;
    }

    private void assertCustomMenuConfig(JsonNode currentCustomerMenu, CustomMenuConfig customerDefaultConfig) {
        assertThat(currentCustomerMenu.get("items").get(0).get("dashboardId").asText()).isEqualTo(((HomeMenuItem) customerDefaultConfig.getItems().get(0)).getDashboardId());
        assertThat(currentCustomerMenu.get("items").get(1).get("name").asText()).isEqualTo(((DefaultMenuItem) customerDefaultConfig.getItems().get(1)).getName());
        assertThat(currentCustomerMenu.get("items").get(2).get("url").asText()).isEqualTo(((CustomMenuItem) customerDefaultConfig.getItems().get(2)).getUrl());
    }

}
