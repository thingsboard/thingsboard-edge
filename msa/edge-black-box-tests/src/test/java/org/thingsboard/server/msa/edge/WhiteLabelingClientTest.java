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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WhiteLabelingClientTest extends AbstractContainerTest {

    @Test
    public void testWhiteLabeling() {
        testWhiteLabeling_sysAdmin();
        testWhiteLabeling_tenant();
        testWhiteLabeling_customer();
    }

    private void testWhiteLabeling_sysAdmin() {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        Optional<WhiteLabelingParams> currentWhiteLabelParamsOpt = cloudRestClient.getCurrentWhiteLabelParams();
        Assert.assertTrue(currentWhiteLabelParamsOpt.isPresent());
        WhiteLabelingParams whiteLabelingParams = currentWhiteLabelParamsOpt.get();
        whiteLabelingParams.setPlatformName("SYSADMIN_PLATFORM_NAME");
        cloudRestClient.saveWhiteLabelParams(whiteLabelingParams);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getWhiteLabelParams(null, null);
                    if (edgeWhiteLabelParams.isEmpty()) {
                        return false;
                    }
                    return "SYSADMIN_PLATFORM_NAME".equals(edgeWhiteLabelParams.get().getPlatformName());
                });
    }

    private void testWhiteLabeling_tenant() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");
        updateAndVerifyWhiteLabelingUpdate("Tenant TB Updated");
    }

    private void testWhiteLabeling_customer() {
        Customer savedCustomer = createCustomerAndAssignEdgeToCustomer();

        updateAndVerifyWhiteLabelingUpdate("Customer TB Updated");

        changeOwnerToTenantAndRemoveCustomer(savedCustomer);
    }

    private void updateAndVerifyWhiteLabelingUpdate(String updateAppTitle) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setAppTitle(updateAppTitle);
        cloudRestClient.saveWhiteLabelParams(whiteLabelingParams);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
                    Optional<WhiteLabelingParams> cloudWhiteLabelParams = cloudRestClient.getCurrentWhiteLabelParams();
                    return edgeWhiteLabelParams.isPresent() &&
                            cloudWhiteLabelParams.isPresent() &&
                            edgeWhiteLabelParams.get().equals(cloudWhiteLabelParams.get());
                });
    }

    @Test
    public void testLoginWhiteLabeling() {
        testLoginWhiteLabeling_sysAdmin();
        testLoginWhiteLabeling_tenant();
        testLoginWhiteLabeling_customer();
    }

    private void testLoginWhiteLabeling_sysAdmin() {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        Optional<LoginWhiteLabelingParams> currentLoginWhiteLabelParamsOpt = cloudRestClient.getCurrentLoginWhiteLabelParams();
        Assert.assertTrue(currentLoginWhiteLabelParamsOpt.isPresent());
        LoginWhiteLabelingParams loginWhiteLabelingParams = currentLoginWhiteLabelParamsOpt.get();
        loginWhiteLabelingParams.setShowNameBottom(Boolean.TRUE);
        cloudRestClient.saveLoginWhiteLabelParams(loginWhiteLabelingParams);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getLoginWhiteLabelParams(null, null);
                    if (edgeLoginWhiteLabelParams.isEmpty()) {
                        return false;
                    }
                    return Boolean.TRUE.equals(edgeLoginWhiteLabelParams.get().getShowNameBottom());
                });
    }

    private void testLoginWhiteLabeling_tenant() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");
        updateAndVerifyLoginWhiteLabelingUpdate("tenant_updated.org");
    }

    private void updateAndVerifyLoginWhiteLabelingUpdate(String updateDomainName) {
        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        loginWhiteLabelingParams.setDomainName(updateDomainName);
        cloudRestClient.saveLoginWhiteLabelParams(loginWhiteLabelingParams);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
                    Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = cloudRestClient.getCurrentLoginWhiteLabelParams();
                    return edgeLoginWhiteLabelParams.isPresent() &&
                            cloudLoginWhiteLabelParams.isPresent() &&
                            edgeLoginWhiteLabelParams.get().equals(cloudLoginWhiteLabelParams.get());
                });
    }

    private void testLoginWhiteLabeling_customer() {
        Customer savedCustomer = createCustomerAndAssignEdgeToCustomer();

        updateAndVerifyLoginWhiteLabelingUpdate("customer_updated.org");

        changeOwnerToTenantAndRemoveCustomer(savedCustomer);
    }

    @Test
    public void testCustomTranslation() throws Exception {
        testCustomTranslation_sysAdmin();
        testCustomTranslation_tenant();
        testCustomTranslation_customer();
    }

    private void testCustomTranslation_sysAdmin() throws Exception {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        Optional<CustomTranslation> currentCustomTranslationOpt = cloudRestClient.getCurrentCustomTranslation();
        Assert.assertTrue(currentCustomTranslationOpt.isPresent());
        CustomTranslation currentCustomTranslation = currentCustomTranslationOpt.get();
        ObjectNode enUsSysAdmin = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        enUsSysAdmin.put("home.home", "SYS_ADMIN_HOME_UPDATED");
        currentCustomTranslation.getTranslationMap().put("en_US", JacksonUtil.OBJECT_MAPPER.writeValueAsString(enUsSysAdmin));
        cloudRestClient.saveCustomTranslation(currentCustomTranslation);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<CustomTranslation> edgeCustomTranslationOpt = edgeRestClient.getCustomTranslation();
                    if (edgeCustomTranslationOpt.isEmpty()) {
                        return false;
                    }
                    CustomTranslation edgeCustomTranslation = edgeCustomTranslationOpt.get();
                    if (edgeCustomTranslation.getTranslationMap().get("en_US") == null) {
                        return false;
                    }
                    JsonNode enUsNode = JacksonUtil.OBJECT_MAPPER.readTree(edgeCustomTranslation.getTranslationMap().get("en_US"));
                    return "SYS_ADMIN_HOME_UPDATED".equals(enUsNode.get("home.home").asText());
                });
    }

    private void testCustomTranslation_tenant() throws Exception {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");
        updateAndVerifyCustomTranslationUpdate("TENANT_HOME_UPDATED");
    }

    private void updateAndVerifyCustomTranslationUpdate(String updateHomeTitle) throws Exception {
        CustomTranslation customTranslation = new CustomTranslation();
        ObjectNode enUsSysAdmin = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        enUsSysAdmin.put("home.home", updateHomeTitle);
        customTranslation.getTranslationMap().put("en_US", JacksonUtil.OBJECT_MAPPER.writeValueAsString(enUsSysAdmin));
        cloudRestClient.saveCustomTranslation(customTranslation);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<CustomTranslation> edgeCustomTranslationOpt = edgeRestClient.getCurrentCustomTranslation();
                    Optional<CustomTranslation> cloudCustomTranslationOpt = cloudRestClient.getCurrentCustomTranslation();
                    return edgeCustomTranslationOpt.isPresent() &&
                            cloudCustomTranslationOpt.isPresent() &&
                            edgeCustomTranslationOpt.get().equals(cloudCustomTranslationOpt.get());
                });
    }

    private void testCustomTranslation_customer() throws Exception {
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
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // validate that edge customer id was updated
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId().getId()));
    }
}

