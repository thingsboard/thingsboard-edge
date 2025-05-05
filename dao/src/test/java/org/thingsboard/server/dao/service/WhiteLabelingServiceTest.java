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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class WhiteLabelingServiceTest extends AbstractServiceTest {

    @Autowired
    CustomerService customerService;
    @Autowired
    WhiteLabelingService whiteLabelingService;
    @Autowired
    DomainService domainService;

    private CustomerId customerId;
    private Domain domain;

    @Before
    public void beforeRun() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customerId = customerService.saveCustomer(customer).getId();

        domain = constructDomain(tenantId, customerId, "my.test.domain");
        domain = domainService.saveDomain(tenantId, domain);
    }

    @Test
    public void testInvalidBaseUrl()  {
        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        loginWhiteLabelingParams.setDomainId(domain.getId());
        String baseUrlWithWhiteSpace = "https://wrong url";
        loginWhiteLabelingParams.setBaseUrl(baseUrlWithWhiteSpace);
        assertThatThrownBy(() -> whiteLabelingService.saveTenantLoginWhiteLabelingParams(tenantId, loginWhiteLabelingParams))
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessage("Base url " + baseUrlWithWhiteSpace + " is invalid");

        String baseUrlWithoutSchema = "wrongurl";
        loginWhiteLabelingParams.setBaseUrl(baseUrlWithoutSchema);
        assertThatThrownBy(() -> whiteLabelingService.saveTenantLoginWhiteLabelingParams(tenantId, loginWhiteLabelingParams))
                .isInstanceOf(IncorrectParameterException.class)
                .hasMessage("Base url " + baseUrlWithoutSchema + " is invalid");
    }

    @Test
    public void testIsWhiteLabelingAllowed_null_null_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(null, null);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_false_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_false_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, false);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_true_true_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(true, true);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertTrue(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertTrue(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_null() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(null);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_false() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(false);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void testIsWhiteLabelingAllowed_false_true_true() throws IOException {
        updateTenantAllowWhiteLabelingSetting(false, true);
        updateCustomerAllowWhiteLabelingSetting(true);
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, null));
        Assert.assertFalse(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
        Assert.assertFalse(whiteLabelingService.isWhiteLabelingAllowed(tenantId, customerId));
    }

    @Test
    public void shouldMergeLoginWhiteLabelingParams() {
        LoginWhiteLabelingParams systemLoginWhiteLabelingParams = new LoginWhiteLabelingParams();
        systemLoginWhiteLabelingParams.setDarkForeground(true);
        systemLoginWhiteLabelingParams.setPageBackgroundColor("#673AB7");

        LoginWhiteLabelingParams tenantLoginWhiteLabelingParams = new LoginWhiteLabelingParams();
        tenantLoginWhiteLabelingParams.setDarkForeground(false);

        tenantLoginWhiteLabelingParams.merge(systemLoginWhiteLabelingParams);
        assertThat(tenantLoginWhiteLabelingParams.getPageBackgroundColor()).isEqualTo("#673AB7");
        assertThat(tenantLoginWhiteLabelingParams.isDarkForeground()).isFalse();
    }

    private void updateTenantAllowWhiteLabelingSetting(Boolean allowWhiteLabeling, Boolean allowCustomerWhiteLabeling) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (allowWhiteLabeling == null) {
            tenant.setAdditionalInfo(JacksonUtil.toJsonNode("{}"));
        } else {
            String additionalInfo = "{\"allowWhiteLabeling\":" + allowWhiteLabeling + ", \"allowCustomerWhiteLabeling\":" + allowCustomerWhiteLabeling + "}";
            tenant.setAdditionalInfo(JacksonUtil.toJsonNode(additionalInfo));
            tenantService.saveTenant(tenant);
        }
    }

    private void updateCustomerAllowWhiteLabelingSetting(Boolean allowWhiteLabeling) throws IOException {
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (allowWhiteLabeling == null) {
            customer.setAdditionalInfo(JacksonUtil.toJsonNode("{}"));
        } else {
            String additionalInfo = "{\"allowWhiteLabeling\":" + allowWhiteLabeling + "}";
            customer.setAdditionalInfo(JacksonUtil.toJsonNode(additionalInfo));
            customerService.saveCustomer(customer);
        }
    }
    private Domain constructDomain(TenantId tenantId, CustomerId customerId, String domainName) {
        Domain domain = new Domain();
        domain.setTenantId(tenantId);
        domain.setCustomerId(customerId);
        domain.setName(domainName);
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(true);
        return domain;
    }
}
