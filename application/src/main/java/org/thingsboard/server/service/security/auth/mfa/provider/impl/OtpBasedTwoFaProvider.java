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

import lombok.Data;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.account.OtpBasedTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.OtpBasedTwoFaProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public abstract class OtpBasedTwoFaProvider<C extends OtpBasedTwoFaProviderConfig, A extends OtpBasedTwoFaAccountConfig> implements TwoFaProvider<C, A> {

    private final Cache verificationCodesCache;

    protected OtpBasedTwoFaProvider(CacheManager cacheManager) {
        this.verificationCodesCache = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE);
    }


    @Override
    public final void prepareVerificationCode(SecurityUser user, C providerConfig, A accountConfig) throws ThingsboardException {
        String verificationCode = StringUtils.randomNumeric(6);
        sendVerificationCode(user, verificationCode, providerConfig, accountConfig);
        verificationCodesCache.put(user.getId(), new Otp(System.currentTimeMillis(), verificationCode, accountConfig));
    }

    protected abstract void sendVerificationCode(SecurityUser user, String verificationCode, C providerConfig, A accountConfig) throws ThingsboardException;


    @Override
    public final boolean checkVerificationCode(SecurityUser user, String code, C providerConfig, A accountConfig) {
        Otp correctVerificationCode = verificationCodesCache.get(user.getId(), Otp.class);
        if (correctVerificationCode != null) {
            if (System.currentTimeMillis() - correctVerificationCode.getTimestamp()
                    > TimeUnit.SECONDS.toMillis(providerConfig.getVerificationCodeLifetime())) {
                verificationCodesCache.evict(user.getId());
                return false;
            }
            if (code.equals(correctVerificationCode.getValue())
                    && accountConfig.equals(correctVerificationCode.getAccountConfig())) {
                verificationCodesCache.evict(user.getId());
                return true;
            }
        }
        return false;
    }


    @Data
    public static class Otp implements Serializable {
        private final long timestamp;
        private final String value;
        private final OtpBasedTwoFaAccountConfig accountConfig;
    }

}
