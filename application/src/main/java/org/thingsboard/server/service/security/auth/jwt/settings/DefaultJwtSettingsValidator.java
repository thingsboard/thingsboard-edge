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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DefaultJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    public void validate(JwtSettings jwtSettings) {
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(15)) {
            throw new DataValidationException("JWT refresh token expiration time should be at least 15 minutes!");
        }
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(1)) {
            throw new DataValidationException("JWT token expiration time should be at least 1 minute!");
        }
        if (jwtSettings.getTokenExpirationTime() >= jwtSettings.getRefreshTokenExpTime()) {
            throw new DataValidationException("JWT token expiration time should greater than JWT refresh token expiration time!");
        }
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be a valid Base64 encoded string! " + e.getMessage());
        }

        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        if (decodedKey.length * Byte.SIZE < 256 && !JwtSettingsService.TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be a Base64 encoded string representing at least 256 bits of data!");
        }

        System.arraycopy(decodedKey, 0, RandomUtils.nextBytes(decodedKey.length), 0, decodedKey.length); //secure memory
    }

}
