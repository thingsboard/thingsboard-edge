/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public abstract class BaseTenantControllerTest extends AbstractControllerTest {
    
    private IdComparator<Tenant> idComparator = new IdComparator<>();

    @Test
    public void testSaveTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        Assert.assertNotNull(savedTenant.getId());
        Assert.assertTrue(savedTenant.getCreatedTime() > 0);
        Assert.assertEquals(tenant.getTitle(), savedTenant.getTitle());
        savedTenant.setTitle("My new tenant");
        doPost("/api/tenant", savedTenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/"+savedTenant.getId().getId().toString(), Tenant.class); 
        Assert.assertEquals(foundTenant.getTitle(), savedTenant.getTitle());
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());
    }
    
    @Test
    public void testFindTenantById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/"+savedTenant.getId().getId().toString(), Tenant.class); 
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(savedTenant, foundTenant);
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());
    }
    
    @Test
    public void testSaveTenantWithEmptyTitle() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        doPost("/api/tenant", tenant)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Tenant title should be specified")));
    }
    
    @Test
    public void testSaveTenantWithInvalidEmail() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        doPost("/api/tenant", tenant)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Invalid email address format")));
    }
    
    @Test
    public void testDeleteTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());        
        doGet("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isNotFound());
    }
    
    @Test
    public void testFindTenants() throws Exception {
        loginSysAdmin();
        List<Tenant> tenants = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(17);
        TextPageData<Tenant> pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        for (int i=0;i<56;i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant"+i);
            tenants.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        
        List<Tenant> loadedTenants = new ArrayList<>();
        pageLink = new TextPageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenants, idComparator);
        Collections.sort(loadedTenants, idComparator);
        
        Assert.assertEquals(tenants, loadedTenants);
        
        for (Tenant tenant : loadedTenants) {
            if (!tenant.getTitle().equals(TEST_TENANT_NAME)) {
                doDelete("/api/tenant/"+tenant.getId().getId().toString())
                .andExpect(status().isOk());        
            }
        }
        
        pageLink = new TextPageLink(17);
        pageData =  doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }
    
    @Test
    public void testFindTenantsByTitle() throws Exception {
        loginSysAdmin();
        String title1 = "Tenant title 1";
        List<Tenant> tenantsTitle1 = new ArrayList<>();
        for (int i=0;i<134;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title1+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle1.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        String title2 = "Tenant title 2";
        List<Tenant> tenantsTitle2 = new ArrayList<>();
        for (int i=0;i<127;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title2+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle2.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        
        List<Tenant> loadedTenantsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Tenant> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenantsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenantsTitle1, idComparator);
        Collections.sort(loadedTenantsTitle1, idComparator);
        
        Assert.assertEquals(tenantsTitle1, loadedTenantsTitle1);
        
        List<Tenant> loadedTenantsTitle2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenantsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantsTitle2, idComparator);
        Collections.sort(loadedTenantsTitle2, idComparator);
        
        Assert.assertEquals(tenantsTitle2, loadedTenantsTitle2);

        for (Tenant tenant : loadedTenantsTitle1) {
            doDelete("/api/tenant/"+tenant.getId().getId().toString())
            .andExpect(status().isOk());        
        }
        
        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (Tenant tenant : loadedTenantsTitle2) {
            doDelete("/api/tenant/"+tenant.getId().getId().toString())
            .andExpect(status().isOk());     
        }
        
        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
