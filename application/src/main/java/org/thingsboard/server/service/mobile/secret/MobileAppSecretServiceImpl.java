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
package org.thingsboard.server.service.mobile.secret;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import static org.thingsboard.server.dao.settings.DefaultSecuritySettingsService.DEFAULT_MOBILE_SECRET_KEY_LENGTH;


@Service
@Slf4j
@RequiredArgsConstructor
public class MobileAppSecretServiceImpl extends AbstractCachedService<String, JwtPair, MobileSecretEvictEvent> implements MobileAppSecretService {

    private final JwtTokenFactory tokenFactory;
    private final SecuritySettingsService securitySettingsService;

    @Override
    public String generateMobileAppSecret(SecurityUser securityUser) {
        log.trace("Executing generateSecret for user [{}]", securityUser.getId());
        Integer mobileSecretKeyLength = securitySettingsService.getSecuritySettings().getMobileSecretKeyLength();
        String secret = StringUtils.generateSafeToken(mobileSecretKeyLength == null ? DEFAULT_MOBILE_SECRET_KEY_LENGTH : mobileSecretKeyLength);
        cache.put(secret, tokenFactory.createTokenPair(securityUser));
        return secret;
    }

    @Override
    public JwtPair getJwtPair(String secret) throws ThingsboardException {
        TbCacheValueWrapper<JwtPair> jwtPair = cache.get(secret);
        if (jwtPair != null) {
            return jwtPair.get();
        } else {
            throw new ThingsboardException("Jwt token not found or expired!", ThingsboardErrorCode.JWT_TOKEN_EXPIRED);
        }
    }

    @TransactionalEventListener(classes = MobileSecretEvictEvent.class)
    @Override
    public void handleEvictEvent(MobileSecretEvictEvent event) {
        cache.evict(event.getSecret());
    }

}
