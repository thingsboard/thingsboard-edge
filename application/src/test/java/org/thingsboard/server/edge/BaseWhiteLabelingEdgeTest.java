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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.v1.CustomTranslationProto;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;

abstract public class BaseWhiteLabelingEdgeTest extends AbstractEdgeTest {

    @Test
    public void testWhiteLabeling() throws Exception {
        testWhiteLabeling_sysAdmin();
        testWhiteLabeling_tenant();
        testWhiteLabeling_customer();
        resetSysAdminWhiteLabelingSettings(tenantAdmin.getEmail(), "testPassword1");
    }

    private void testWhiteLabeling_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifyWhiteLabelingUpdate("Sys Admin TB Updated");
    }

    private void testWhiteLabeling_tenant() throws Exception {
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        updateAndVerifyWhiteLabelingUpdate("Tenant TB Updated");
    }

    private void testWhiteLabeling_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyWhiteLabelingUpdate("Customer A TB Updated");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyWhiteLabelingUpdate("Sub Customer A TB Updated");
    }

    private void updateAndVerifyWhiteLabelingUpdate(String updatedAppTitle) throws Exception {
        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        edgeImitator.expectMessageAmount(1);
        whiteLabelingParams.setAppTitle(updatedAppTitle);
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WhiteLabelingParamsProto);
        WhiteLabelingParamsProto whiteLabelingParamsProto = (WhiteLabelingParamsProto) latestMessage;
        Assert.assertEquals(updatedAppTitle, whiteLabelingParamsProto.getAppTitle());
    }

    @Test
    public void testLoginWhiteLabeling() throws Exception {
        testLoginWhiteLabeling_sysAdmin();
        testLoginWhiteLabeling_tenant();
        testLoginWhiteLabeling_customer();
        resetSysAdminWhiteLabelingSettings(tenantAdmin.getEmail(), "testPassword1");
    }

    private void testLoginWhiteLabeling_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifyLoginWhiteLabelingUpdate("sysadmin_updated.org");
    }

    private void testLoginWhiteLabeling_tenant() throws Exception {
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        updateAndVerifyLoginWhiteLabelingUpdate("tenant_updated.org");
    }

    private void testLoginWhiteLabeling_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyLoginWhiteLabelingUpdate("customerA_updated.org");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyLoginWhiteLabelingUpdate("subCustomerA_updated.org");
    }

    private void updateAndVerifyLoginWhiteLabelingUpdate(String updatedDomainName) throws Exception {
        LoginWhiteLabelingParams loginWhiteLabelingParams = doGet("/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
        edgeImitator.expectMessageAmount(1);
        loginWhiteLabelingParams.setDomainName(updatedDomainName);
        doPost("/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof LoginWhiteLabelingParamsProto);
        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto = (LoginWhiteLabelingParamsProto) latestMessage;
        Assert.assertEquals(updatedDomainName, loginWhiteLabelingParamsProto.getDomainName());
    }

    @Test
    public void testCustomTranslation() throws Exception {
        testCustomTranslation_sysAdmin();
        testCustomTranslation_tenant();
        testCustomTranslation_customer();
        resetSysAdminWhiteLabelingSettings(tenantAdmin.getEmail(), "testPassword1");
    }

    private void testCustomTranslation_sysAdmin() throws Exception {
        loginSysAdmin();
        updateAndVerifyCustomTranslationUpdate("sys_admin_value_updated");
    }

    private void testCustomTranslation_tenant() throws Exception {
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        updateAndVerifyCustomTranslationUpdate("tenant_value_updated");
    }

    private void testCustomTranslation_customer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        createCustomerUserAndLogin(savedCustomerA, "customerA@thingsboard.org");
        updateAndVerifyCustomTranslationUpdate("customer_value_updated");

        createCustomerUserAndLogin(savedSubCustomerA, "subCustomerA@thingsboard.org");
        updateAndVerifyCustomTranslationUpdate("sub_customer_value_updated");
    }

    private void createCustomerUserAndLogin(Customer customer, String email) throws Exception {
        edgeImitator.expectMessageAmount(2);
        User customerAUser = new User();
        customerAUser.setAuthority(Authority.CUSTOMER_USER);
        customerAUser.setTenantId(savedTenant.getId());
        customerAUser.setCustomerId(customer.getId());
        customerAUser.setEmail(email);
        EntityGroupInfo customerAdminsGroup = findCustomerAdminsGroup(customer);
        User savedCustomerUser = createUser(customerAUser, "customer", customerAdminsGroup.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());  // wait 2 messages - user update msg and user credentials update msg
        loginUser(savedCustomerUser.getEmail(), "customer");
    }

    private void updateAndVerifyCustomTranslationUpdate(String updatedHomeValue) throws Exception {
        CustomTranslation customTranslation = doGet("/api/customTranslation/customTranslation", CustomTranslation.class);
        edgeImitator.expectMessageAmount(1);
        customTranslation.getTranslationMap().put("en_US", JacksonUtil.OBJECT_MAPPER.writeValueAsString(getCustomTranslationHomeObject(updatedHomeValue)));
        doPost("/api/customTranslation/customTranslation", customTranslation, CustomTranslation.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomTranslationProto);
        CustomTranslationProto customTranslationProto = (CustomTranslationProto) latestMessage;
        String enUsLangObject = customTranslationProto.getTranslationMapMap().get("en_US");
        Assert.assertEquals(updatedHomeValue, JacksonUtil.OBJECT_MAPPER.readTree(enUsLangObject).get("home").asText());
    }

}
