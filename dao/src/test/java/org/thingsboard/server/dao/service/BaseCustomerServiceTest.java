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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseCustomerServiceTest extends AbstractBeforeTest {

    private IdComparator<Customer> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void beforeRun() {
        tenantId = before();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveCustomer() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());


        savedCustomer.setTitle("My new customer");

        customerService.saveCustomer(savedCustomer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertEquals(foundCustomer.getTitle(), savedCustomer.getTitle());

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testFindCustomerById() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNotNull(foundCustomer);
        Assert.assertEquals(savedCustomer, foundCustomer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithEmptyTitle() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithEmptyTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithInvalidTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithInvalidEmail() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customer.setEmail("invalid@mail");
        customerService.saveCustomer(customer);
    }

    @Test
    public void testDeleteCustomer() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNull(foundCustomer);
    }

    @Test
    public void testFindCustomersByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 135; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle("Customer" + i);
            customers.add(customerService.saveCustomer(customer));
        }

        List<Customer> loadedCustomers = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customers, idComparator);
        Collections.sort(loadedCustomers, idComparator);

        Assert.assertEquals(customers, loadedCustomers);

        customerService.deleteCustomersByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindCustomersByTenantIdAndTitle() {
        String title1 = "Customer title 1";
        List<Customer> customersTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            customersTitle1.add(customerService.saveCustomer(customer));
        }
        String title2 = "Customer title 2";
        List<Customer> customersTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            customersTitle2.add(customerService.saveCustomer(customer));
        }

        List<Customer> loadedCustomersTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customersTitle1, idComparator);
        Collections.sort(loadedCustomersTitle1, idComparator);

        Assert.assertEquals(customersTitle1, loadedCustomersTitle1);

        List<Customer> loadedCustomersTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customersTitle2, idComparator);
        Collections.sort(loadedCustomersTitle2, idComparator);

        Assert.assertEquals(customersTitle2, loadedCustomersTitle2);

        for (Customer customer : loadedCustomersTitle1) {
            customerService.deleteCustomer(tenantId, customer.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Customer customer : loadedCustomersTitle2) {
            customerService.deleteCustomer(tenantId, customer.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
