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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CustomTranslationClientTest extends AbstractContainerTest {

    @Test
    public void testCustomTranslation() {
        performTestOnEachEdge(this::_testCustomTranslation);
    }

    private void _testCustomTranslation() {
        testCustomTranslation_sysAdmin();
        testCustomTranslation_tenant();
        testCustomTranslation_customer();
    }

    private void testCustomTranslation_sysAdmin() {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        String locale = "en_US";
        JsonNode enUsSysAdmin = JacksonUtil.newObjectNode().put("home", "home");
        cloudRestClient.saveCustomTranslation(locale, enUsSysAdmin);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<JsonNode> edgeCustomTranslationOpt = edgeRestClient.getMergedCustomTranslation(locale);
                    Optional<JsonNode> cloudCustomTranslationOpt = cloudRestClient.getMergedCustomTranslation(locale);
                    return edgeCustomTranslationOpt.isPresent() &&
                            cloudCustomTranslationOpt.isPresent() &&
                            edgeCustomTranslationOpt.get().equals(cloudCustomTranslationOpt.get());
                });

        cloudRestClient.deleteCustomTranslation(locale);
    }

    private void testCustomTranslation_tenant() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");
        updateAndVerifyCustomTranslationUpdate("TENANT_HOME_UPDATED");
    }

    private void updateAndVerifyCustomTranslationUpdate(String updateHomeTitle) {
        String locale = "en_US";
        JsonNode enUsSysAdmin = JacksonUtil.newObjectNode().put("home", updateHomeTitle);
        cloudRestClient.saveCustomTranslation(locale, enUsSysAdmin);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<JsonNode> edgeCustomTranslationOpt = edgeRestClient.getCustomTranslation(locale);
                    Optional<JsonNode> cloudCustomTranslationOpt = cloudRestClient.getCustomTranslation(locale);
                    return edgeCustomTranslationOpt.isPresent() &&
                            cloudCustomTranslationOpt.isPresent() &&
                            edgeCustomTranslationOpt.get().equals(cloudCustomTranslationOpt.get());
                });

        cloudRestClient.deleteCustomTranslation(locale);
    }

    private void testCustomTranslation_customer() {
        Customer savedCustomer = createCustomerAndAssignEdgeToCustomer();

        updateAndVerifyCustomTranslationUpdate("CUSTOMER_HOME_UPDATED");

        changeOwnerToTenantAndRemoveCustomer(savedCustomer);
    }

    private Customer createCustomerAndAssignEdgeToCustomer() {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer A", null);

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());

        // create user
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(edge.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("edgeCustomer@thingsboard.org");
        User savedUser = cloudRestClient.saveUser(user, false, findCustomerAdminsGroup(savedCustomer).get().getId());
        cloudRestClient.activateUser(savedUser.getId(), "customer", false);

        verifyThatCustomerAdminGroupIsCreatedOnEdge(savedCustomer);

        loginIntoEdgeWithRetries("edgeCustomer@thingsboard.org", "customer");
        cloudRestClient.login("edgeCustomer@thingsboard.org", "customer");

        return savedCustomer;
    }

    private void changeOwnerToTenantAndRemoveCustomer(Customer savedCustomer) {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // delete customer
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // validate that customer was deleted from edge
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // validate that edge customer id was updated
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId().getId()));
    }

}
