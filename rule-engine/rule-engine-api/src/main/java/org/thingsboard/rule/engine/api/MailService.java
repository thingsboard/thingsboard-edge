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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.mail.javamail.JavaMailSender;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

public interface MailService {

    void sendEmail(TenantId tenantId, String email, String subject, String message) throws ThingsboardException;

    void sendTestMail(TenantId tenantId, JsonNode config, String email) throws ThingsboardException;

    void sendActivationEmail(TenantId tenantId, String activationLink, String email) throws ThingsboardException;

    void sendAccountActivatedEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException;

    void sendResetPasswordEmail(TenantId tenantId, String passwordResetLink, String email) throws ThingsboardException;

    void sendResetPasswordEmailAsync(TenantId tenantId, String passwordResetLink, String email);

    void sendPasswordWasResetEmail(TenantId tenantId, String loginLink, String email) throws ThingsboardException;

    void sendUserActivatedEmail(TenantId tenantId, String userFullName, String userEmail, String email) throws ThingsboardException;

    void sendUserRegisteredEmail(TenantId tenantId, String userFullName, String userEmail, String targetEmail) throws ThingsboardException;

    void sendTwoFaVerificationEmail(TenantId tenantId, String email, String verificationCode, int expirationTimeSeconds) throws ThingsboardException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws ThingsboardException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, long timeout, JavaMailSender javaMailSender) throws ThingsboardException;

    void sendAccountLockoutEmail(TenantId tenantId, String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws ThingsboardException;

    void sendApiFeatureStateEmail(TenantId tenantId, ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState msg) throws ThingsboardException;

    void testConnection(TenantId tenantId) throws Exception;

}
