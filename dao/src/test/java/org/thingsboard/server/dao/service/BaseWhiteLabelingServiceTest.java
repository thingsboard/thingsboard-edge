/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.IOException;

public class BaseWhiteLabelingServiceTest extends AbstractBeforeTest {

    private TenantId tenantId;
    private CustomerId customerId;

    @Before
    public void beforeRun() {
        tenantId = before();
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customerId = customerService.saveCustomer(customer).getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testIsWhiteLabelingAllowed_null_null_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(null, null);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, tenantId));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    private void updateTenantAllowWhiteLabelingSetting(Boolean allowWhiteLabeling, Boolean allowCustomerWhiteLabeling) throws IOException {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (allowWhiteLabeling == null) {
            tenant.setAdditionalInfo(mapper.readTree("{}"));
        } else {
            String additionalInfo = "{\"allowWhiteLabeling\":" + allowWhiteLabeling + ", \"allowCustomerWhiteLabeling\":" + allowCustomerWhiteLabeling + "}";
            tenant.setAdditionalInfo(mapper.readTree(additionalInfo));
            tenantService.saveTenant(tenant);
        }
    }

    private void updateCustomerAllowWhiteLabelingSetting(Boolean allowWhiteLabeling) throws IOException {
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (allowWhiteLabeling == null) {
            customer.setAdditionalInfo(mapper.readTree("{}"));
        } else {
            String additionalInfo = "{\"allowWhiteLabeling\":" + allowWhiteLabeling + "}";
            customer.setAdditionalInfo(mapper.readTree(additionalInfo));
            customerService.saveCustomer(customer);
        }
    }
}
