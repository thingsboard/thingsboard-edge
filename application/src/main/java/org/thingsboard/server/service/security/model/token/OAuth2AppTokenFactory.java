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
package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OAuth2AppTokenFactory {

    private static final String CALLBACK_URL_SCHEME = "callbackUrlScheme";

    private static final long MAX_EXPIRATION_TIME_DIFF_MS = TimeUnit.MINUTES.toMillis(5);

    public String validateTokenAndGetCallbackUrlScheme(String appPackage, String appToken, String appSecret) {
        Jws<Claims> jwsClaims;
        try {
            jwsClaims = Jwts.parser().setSigningKey(appSecret).parseClaimsJws(appToken);
        }
        catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException ex) {
            throw new IllegalArgumentException("Invalid Application token: ", ex);
        } catch (ExpiredJwtException expiredEx) {
            throw new IllegalArgumentException("Application token expired", expiredEx);
        }
        Claims claims = jwsClaims.getBody();
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("Application token must have expiration date");
        }
        long timeDiff = expiration.getTime() - System.currentTimeMillis();
        if (timeDiff > MAX_EXPIRATION_TIME_DIFF_MS) {
            throw new IllegalArgumentException("Application token expiration time can't be longer than 5 minutes");
        }
        if (!claims.getIssuer().equals(appPackage)) {
            throw new IllegalArgumentException("Application token issuer doesn't match application package");
        }
        String callbackUrlScheme = claims.get(CALLBACK_URL_SCHEME, String.class);
        if (StringUtils.isEmpty(callbackUrlScheme)) {
            throw new IllegalArgumentException("Application token doesn't have callbackUrlScheme");
        }
        return callbackUrlScheme;
    }

}
