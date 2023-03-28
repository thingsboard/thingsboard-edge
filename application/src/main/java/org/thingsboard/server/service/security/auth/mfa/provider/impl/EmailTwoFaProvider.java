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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.EmailTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
public class EmailTwoFaProvider extends OtpBasedTwoFaProvider<EmailTwoFaProviderConfig, EmailTwoFaAccountConfig> {

    private final MailService mailService;

    protected EmailTwoFaProvider(CacheManager cacheManager, MailService mailService) {
        super(cacheManager);
        this.mailService = mailService;
    }

    @Override
    public EmailTwoFaAccountConfig generateNewAccountConfig(User user, EmailTwoFaProviderConfig providerConfig) {
        EmailTwoFaAccountConfig config = new EmailTwoFaAccountConfig();
        config.setEmail(user.getEmail());
        return config;
    }

    @Override
    public void check(TenantId tenantId) throws ThingsboardException {
        try {
            mailService.testConnection(tenantId);
        } catch (Exception e) {
            throw new ThingsboardException("Mail service is not set up", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, EmailTwoFaProviderConfig providerConfig, EmailTwoFaAccountConfig accountConfig) throws ThingsboardException {
        mailService.sendTwoFaVerificationEmail(user.getTenantId(), accountConfig.getEmail(), verificationCode, providerConfig.getVerificationCodeLifetime());
    }

    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.EMAIL;
    }

}
