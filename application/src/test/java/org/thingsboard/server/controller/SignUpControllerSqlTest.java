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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class SignUpControllerSqlTest extends AbstractControllerTest {

    public static final String SELF_REG_SETTINGS = "{\"signUpTextMessage\":null," +
            "\"captchaSiteKey\":\"6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI\"," +
            "\"showPrivacyPolicy\":false," +
            "\"showTermsOfUse\":false," +
            "\"adminSettingsId\":null," +
            "\"domainName\":\"localhost\"," +
            "\"captchaSecretKey\":\"6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe\"," +
            "\"privacyPolicy\":null," +
            "\"termsOfUse\":null," +
            "\"notificationEmail\":\"test@test.com\"," +
            "\"defaultDashboardId\":null," +
            "\"defaultDashboardFullscreen\":false," +
            "\"permissions\":[]," +
            "\"pkgName\":null," +
            "\"appSecret\":null," +
            "\"appScheme\":null," +
            "\"appHost\":null}";

    public static final String RECAPTCHA_DUMMY_RESPONSE =
            "03AGdBq25eKTPJzWYaCnIx7JLlcQIEIZHX8IMiXbWb39HnZPMb8fE61JxA_xA-UAcPZhFvWJo00-VukIJXky" +
                    "_Fp6vPUseVA0564BBUKJuRU-jA591Wx46ZDUXqvWtsXpzgSSv5cOXFgaOpkM0pGa9Azw8CaPjVZN0Z13PKsvKogtSpess" +
                    "-n_-OMbVo0tkjGfiy34ih5Uf1l6LhbxVR7NYfzKqZPD2qRM25jcRNugQbxmFpemAaREcjkOmL3tr23EyRVWgDsv032DqiaI" +
                    "IcCoNT3zUoqjfDo1m-yL3kwtO4-WqEOoP2oO353-pqjMYMPaYpjjQJXxHL4qJC1xky4ANAI_th6GtsHwnfLw_sWDHlPgb" +
                    "-IEW8wcD-zZW5TBpagv7p0V08ebdqxCkvb-7p4QrgNXQA_psw4SEHIg";

    public static final String TEST_EMAIL = "force_push@junior.com";

    @SpyBean
    protected TbNotificationEntityService notificationService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected AdminSettingsService adminSettingsService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        doPost("/api/selfRegistration/selfRegistrationParams",
                JacksonUtil.toJsonNode(SELF_REG_SETTINGS), JsonNode.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        adminSettingsService.deleteAdminSettingsByKey(tenantId,
                "selfRegistrationDomainNamePrefix_localhost");
    }

    @Test
    public void testSelfRegisterUser() throws Exception {
        doSignUp(getTestSignUpRequest());

        var user = userService.findUserByEmail(tenantId, TEST_EMAIL);
        Assert.assertNotNull(user);
        Assert.assertEquals(TEST_EMAIL, user.getEmail());

        removeCreatedUser(user);
    }

    @Test
    public void testSelfRegistrationCreateMessageInRuleChain() throws Exception {
        var signUpRequest = getTestSignUpRequest();
        doSignUp(signUpRequest);

        var user = userService.findUserByEmail(tenantId, TEST_EMAIL);
        var customer = customerService.findCustomerById(tenantId, user.getCustomerId());
        var entityGroup = entityGroupService.findOrCreateUserGroup(
                tenantId, customer.getId(), "Self Registration Users", "Autogenerated Self Registration group"
        );

        Assert.assertNotNull(user);
        Assert.assertEquals(TEST_EMAIL, user.getEmail());

        verify(notificationService, times(1)).notifyCreateOrUpdateEntity(
                eq(tenantId),
                eq(customer.getId()),
                eq(customer),
                eq(customer.getId()),
                eq(ActionType.ADDED),
                eq(null)
        );
        verify(notificationService, times(1)).notifyCreateOrUpdateOrDelete(
                eq(tenantId),
                eq(customer.getId()),
                eq(user.getId()),
                argThat(o -> {
                    if (!(o instanceof User)) return false;
                    User u = (User) o;
                    return u.getId().equals(user.getId()) && u.getEmail().equals(user.getEmail());
                }),
                eq(null),
                eq(ActionType.ADDED),
                eq(true),
                eq(null)
        );
        verify(notificationService, times(1)).notifyAddToEntityGroup(
                eq(tenantId),
                eq(user.getId()),
                argThat(o -> {
                    if (!(o instanceof User)) return false;
                    User u = (User) o;
                    return u.getId().equals(user.getId()) && u.getEmail().equals(user.getEmail());
                }),
                eq(customer.getId()),
                eq(entityGroup.getId()),
                eq(null),
                eq(entityGroup.toString()),
                eq(entityGroup.getName())
        );

        removeCreatedUser(user);
    }

    protected SignUpRequest getTestSignUpRequest() {
        var signUpRequest = new SignUpRequest();
        signUpRequest.setEmail(TEST_EMAIL);
        signUpRequest.setFirstName("Test");
        signUpRequest.setLastName("Test");
        signUpRequest.setPassword("abcdef123");
        signUpRequest.setRecaptchaResponse(RECAPTCHA_DUMMY_RESPONSE);
        return signUpRequest;
    }

    protected void doSignUp(SignUpRequest signUpRequest) throws Exception {
        var result = doPostWithTypedResponse(
                "/api/noauth/signup/", signUpRequest,
                new TypeReference<SignUpResult>() {
                }
        );
        Assert.assertEquals("Error while doing SignUp", SignUpResult.SUCCESS, result);
    }

    private void removeCreatedUser(User user) {
        userService.deleteUser(tenantId, user.getId());
        var found = userService.findUserByEmail(tenantId, TEST_EMAIL);
        Assert.assertNull("Expected that created user is deleted but one found!", found);
    }
}
