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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class WhiteLabelingClientTest extends AbstractContainerTest {

    @Test
    public void testWhiteLabeling() {
        testWhiteLabeling_sysAdmin();
        testWhiteLabeling_tenant();
        testWhiteLabeling_customer();

        // @TODO:
        // add test for login wl
        // add test for custom translation
        // check what is below and if it's required

//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> {
//                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
//                    Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = cloudRestClient.getCurrentLoginWhiteLabelParams();
//                    return edgeLoginWhiteLabelParams.isPresent() &&
//                            cloudLoginWhiteLabelParams.isPresent() &&
//                            edgeLoginWhiteLabelParams.get().equals(cloudLoginWhiteLabelParams.get());
//                });
//
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> {
//                    Optional<CustomTranslation> edgeCustomTranslationOpt = edgeRestClient.getCustomTranslation();
//                    Optional<CustomTranslation> cloudCustomTranslationOpt = cloudRestClient.getCustomTranslation();
//                    if (edgeCustomTranslationOpt.isEmpty() || cloudCustomTranslationOpt.isEmpty()) {
//                        return false;
//                    }
//                    CustomTranslation edgeCustomTranslation = edgeCustomTranslationOpt.get();
//                    if (edgeCustomTranslation.getTranslationMap().get("en_us") == null) {
//                        return false;
//                    }
//                    JsonNode enUsNode = JacksonUtil.OBJECT_MAPPER.readTree(edgeCustomTranslation.getTranslationMap().get("en_us"));
//                    if (!"TENANT_HOME".equals(enUsNode.get("home.home").asText())) {
//                        return false;
//                    }
//                    CustomTranslation cloudCustomTranslation = cloudCustomTranslationOpt.get();
//                    if (cloudCustomTranslation.getTranslationMap().get("en_us") == null) {
//                        return false;
//                    }
//                    enUsNode = JacksonUtil.OBJECT_MAPPER.readTree(cloudCustomTranslation.getTranslationMap().get("en_us"));
//                    if (!"TENANT_HOME".equals(enUsNode.get("home.home").asText())) {
//                        return false;
//                    }
//                    return edgeCustomTranslation.equals(cloudCustomTranslation);
//                });
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
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer A", null);

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
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
        loginIntoEdgeWithRetries("edgeCustomer@thingsboard.org", "customer");
        cloudRestClient.login("edgeCustomer@thingsboard.org", "customer");

        updateAndVerifyWhiteLabelingUpdate("Customer TB Updated");

        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // delete customer
        cloudRestClient.deleteCustomer(savedCustomer.getId());
    }

    private void updateAndVerifyWhiteLabelingUpdate(String updateAppTitle) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setAppTitle(updateAppTitle);
        cloudRestClient.saveWhiteLabelParams(whiteLabelingParams);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
                    Optional<WhiteLabelingParams> cloudWhiteLabelParams = cloudRestClient.getCurrentWhiteLabelParams();
                    return edgeWhiteLabelParams.isPresent() &&
                            cloudWhiteLabelParams.isPresent() &&
                            edgeWhiteLabelParams.get().equals(cloudWhiteLabelParams.get());
                });
    }
}

