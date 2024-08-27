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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
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
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

@Slf4j
@DaoSqlTest
public class CustomMenuControllerTest extends AbstractControllerTest {

    @Before
    public void setup() throws Exception {
        loginSysAdmin();
        // create system default menu
        CustomMenuInfo systemMenu = new CustomMenuInfo();
        systemMenu.setName("System Menu");
        systemMenu.setScope(CMScope.SYSTEM);
        systemMenu.setAssigneeType(CMAssigneeType.ALL);

        doPost("/api/customMenu", systemMenu);

        // create tenant default menu
        CustomMenuInfo tenantMenu = new CustomMenuInfo();
        tenantMenu.setName("Tenant Menu");
        tenantMenu.setScope(CMScope.TENANT);
        tenantMenu.setAssigneeType(CMAssigneeType.ALL);

        doPost("/api/customMenu", tenantMenu);

        // create customer default menu
        CustomMenuInfo customerMenu = new CustomMenuInfo();
        customerMenu.setName("Tenant Menu");
        customerMenu.setScope(CMScope.CUSTOMER);
        customerMenu.setAssigneeType(CMAssigneeType.ALL);

        doPost("/api/customMenu", tenantMenu);
    }

    @After
    public void teardown() throws Exception {
    }

    @Test
    public void testDefaultMenu() throws Exception {
        loginTenantAdmin();
        checkDefaultMenu(CMScope.TENANT, "myHomeDashboard" + RandomStringUtils.randomAlphabetic(5),
                "my tenants " + RandomStringUtils.randomAlphabetic(5),
                "testUrl"+ RandomStringUtils.randomAlphabetic(5));

        loginCustomerAdminUser();
        checkDefaultMenu(CMScope.CUSTOMER, "myHomeDashboard" + RandomStringUtils.randomAlphabetic(5),
                "my tenants " + RandomStringUtils.randomAlphabetic(5),
                "my new item "+ RandomStringUtils.randomAlphabetic(5));

        loginSubCustomerAdminUser();
        checkDefaultMenu(CMScope.CUSTOMER, "myHomeDashboard" + RandomStringUtils.randomAlphabetic(5),
                "my tenants " + RandomStringUtils.randomAlphabetic(5),
                "my new item "+ RandomStringUtils.randomAlphabetic(5));
    }

    private void checkDefaultMenu(CMScope scope, String homeMenuItemDashboard, String defaultMenuItemName, String customMenuItemUrl) throws Exception {
        CustomMenuInfo customMenu = new CustomMenuInfo();
        customMenu.setName("Tenant Menu");
        customMenu.setScope(scope);
        customMenu.setAssigneeType(CMAssigneeType.ALL);
        CustomMenu savedMenu = doPost("/api/customMenu", customMenu, CustomMenu.class);

        //put configuration
        HomeMenuItem homeMenuItem = new HomeMenuItem();
        homeMenuItem.setId("home");
        homeMenuItem.setHomeType(HomeMenuItemType.DASHBOARD);
        homeMenuItem.setDashboardId(homeMenuItemDashboard);

        DefaultMenuItem defaultMenuItem = new DefaultMenuItem();
        defaultMenuItem.setId("tenants");
        defaultMenuItem.setName(defaultMenuItemName);
        defaultMenuItem.setVisible(true);

        CustomMenuItem customMenuItem = new CustomMenuItem();
        customMenuItem.setName("Mu new menu item");
        customMenuItem.setMenuItemType(CMItemType.LINK);
        customMenuItem.setLinkType(CMItemLinkType.URL);
        customMenuItem.setUrl(customMenuItemUrl);
        customMenuItem.setIcon("icon");
        customMenuItem.setVisible(true);

        CustomMenuConfig customMenuConfig = new CustomMenuConfig();
        customMenuConfig.setItems(List.of(homeMenuItem, defaultMenuItem, customMenuItem));

        doPut("/api/customMenu/" + savedMenu.getId() + "/config", customMenuConfig);

        CustomMenuConfig currentTenantMenu = doGet("/api/customMenu", CustomMenuConfig.class);
        Assert.assertEquals(customMenuConfig, currentTenantMenu);
    }

}
