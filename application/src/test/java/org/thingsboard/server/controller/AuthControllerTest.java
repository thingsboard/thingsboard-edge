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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AuthControllerTest extends AbstractControllerTest {

    @After
    public void tearDown() throws Exception {
        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);

        securitySettings.getPasswordPolicy().setMaximumLength(72);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(false);

        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());
    }

    @Test
    public void testGetUser() throws Exception {
        
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());
        
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
        
        loginTenantAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.TENANT_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(TENANT_ADMIN_EMAIL)));
        
        loginCustomerUser();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.CUSTOMER_USER.name())))
        .andExpect(jsonPath("$.email",is(CUSTOMER_USER_EMAIL)));
    }
    
    @Test
    public void testLoginLogout() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        TimeUnit.SECONDS.sleep(1); //We need to make sure that event for invalidating token was successfully processed;

        logout();
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());

        resetTokens();
    }

    @Test
    public void testRefreshToken() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        refreshToken();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
    }

    @Test
    public void testShouldNotUpdatePasswordWithValueLongerThanDefaultLimit() throws Exception {
        loginTenantAdmin();
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword(RandomStringUtils.randomAlphanumeric(73));
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password must be no more than 72 characters in length.")));
    }

    @Test
    public void testShouldNotAuthorizeUserIfHisPasswordBecameTooLong() throws Exception {
        loginTenantAdmin();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        String newPassword = RandomStringUtils.randomAlphanumeric(16);
        changePasswordRequest.setNewPassword(newPassword);
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isOk());
        loginUser(TENANT_ADMIN_EMAIL, newPassword);

        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);
        securitySettings.getPasswordPolicy().setMaximumLength(15);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(true);
        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());

        //try to login with user password that is not valid after security settings was updated
        doPost("/api/auth/login", new LoginRequest(TENANT_ADMIN_EMAIL, newPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("The entered password violates our policies. If this is your real password, please reset it.")));
    }

    @Test
    public void testShouldNotResetPasswordToTooLongValue() throws Exception {
        loginTenantAdmin();

        JsonNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode()
                .put("email", TENANT_ADMIN_EMAIL);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", this.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + this.currentResetPasswordToken));

        String newPassword = RandomStringUtils.randomAlphanumeric(73);
        JsonNode resetPasswordRequest = JacksonUtil.newObjectNode()
                .put("resetToken", this.currentResetPasswordToken)
                .put("password", newPassword);

        Mockito.doNothing().when(mailService).sendPasswordWasResetEmail(any(), anyString(), anyString());
        doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message",
                                is("Password must be no more than 72 characters in length.")));
    }
}
