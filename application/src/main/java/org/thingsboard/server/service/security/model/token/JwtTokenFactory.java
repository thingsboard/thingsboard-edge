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
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFactory {

    private static final String SCOPES = "scopes";
    private static final String USER_GROUP_IDS = "userGroupIds";
    private static final String USER_ID = "userId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ENABLED = "enabled";
    private static final String IS_PUBLIC = "isPublic";
    private static final String TENANT_ID = "tenantId";
    private static final String CUSTOMER_ID = "customerId";
    private static final String SESSION_ID = "sessionId";

    private final JwtSettingsService jwtSettingsService;
    private final UserPermissionsService userPermissionsService;

    /**
     * Factory method for issuing new JWT Tokens.
     */
    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        if (securityUser.getAuthority() == null) {
            throw new IllegalArgumentException("User doesn't have any privileges");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        Claims claims = Jwts.claims().setSubject(principal.getValue());
        JwtBuilder jwtBuilder = setUpToken(securityUser, securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()), jwtSettingsService.getJwtSettings().getTokenExpirationTime(), claims);
        jwtBuilder.claim(FIRST_NAME, securityUser.getFirstName())
                .claim(LAST_NAME, securityUser.getLastName())
                .claim(ENABLED, securityUser.isEnabled())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID);
        if (securityUser.getTenantId() != null) {
            jwtBuilder.claim(TENANT_ID, securityUser.getTenantId().getId().toString());
        }
        if (securityUser.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, securityUser.getCustomerId().getId().toString());
        }

        String token = jwtBuilder.compact();

        return new AccessJwtToken(token, claims);
    }

    public SecurityUser parseAccessJwtToken(RawAccessJwtToken rawAccessToken) {
        Jws<Claims> jwsClaims = parseTokenClaims(rawAccessToken);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT Token doesn't have any scopes");
        }
        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setEmail(subject);
        securityUser.setAuthority(Authority.parse(scopes.get(0)));
        String tenantId = claims.get(TENANT_ID, String.class);
        if (tenantId != null) {
            securityUser.setTenantId(TenantId.fromUUID(UUID.fromString(tenantId)));
        } else if (securityUser.getAuthority() == Authority.SYS_ADMIN) {
            securityUser.setTenantId(TenantId.SYS_TENANT_ID);
        }
        String customerId = claims.get(CUSTOMER_ID, String.class);
        if (customerId != null) {
            securityUser.setCustomerId(new CustomerId(UUID.fromString(customerId)));
        }
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }

        UserPrincipal principal;
        if (securityUser.getAuthority() != Authority.PRE_VERIFICATION_TOKEN) {
            securityUser.setFirstName(claims.get(FIRST_NAME, String.class));
            securityUser.setLastName(claims.get(LAST_NAME, String.class));
            securityUser.setEnabled(claims.get(ENABLED, Boolean.class));
            boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
            principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
            try {
                securityUser.setUserPermissions(userPermissionsService.getMergedPermissions(securityUser, isPublic));
            } catch (Exception e) {
                throw new BadCredentialsException("Failed to get user permissions", e);
            }
        } else {
            principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, subject);
        }
        securityUser.setUserPrincipal(principal);

        return securityUser;
    }

    public JwtToken createRefreshToken(SecurityUser securityUser) {
        UserPrincipal principal = securityUser.getUserPrincipal();

        Claims claims = Jwts.claims().setSubject(principal.getValue());
        String token = setUpToken(securityUser, Collections.singletonList(Authority.REFRESH_TOKEN.name()), jwtSettingsService.getJwtSettings().getRefreshTokenExpTime(), claims)
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID)
                .setId(UUID.randomUUID().toString()).compact();

        return new AccessJwtToken(token, claims);
    }

    public SecurityUser parseRefreshToken(RawAccessJwtToken rawAccessToken) {
        Jws<Claims> jwsClaims = parseTokenClaims(rawAccessToken);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Refresh Token doesn't have any scopes");
        }
        if (!scopes.get(0).equals(Authority.REFRESH_TOKEN.name())) {
            throw new IllegalArgumentException("Invalid Refresh Token scope");
        }
        boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
        UserPrincipal principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setUserPrincipal(principal);
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }
        return securityUser;
    }

    public JwtToken createPreVerificationToken(SecurityUser user, Integer expirationTime) {
        Claims claims = Jwts.claims().setSubject(user.getEmail());
        JwtBuilder jwtBuilder = setUpToken(user, Collections.singletonList(Authority.PRE_VERIFICATION_TOKEN.name()), expirationTime, claims)
                .claim(TENANT_ID, user.getTenantId().toString());
        if (user.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, user.getCustomerId().toString());
        }
        return new AccessJwtToken(jwtBuilder.compact(), claims);
    }

    private JwtBuilder setUpToken(SecurityUser securityUser, List<String> scopes, long expirationTime, Claims claims) {
        if (StringUtils.isBlank(securityUser.getEmail())) {
            throw new IllegalArgumentException("Cannot create JWT Token without username/email");
        }

        claims.put(USER_ID, securityUser.getId().getId().toString());
        claims.put(SCOPES, scopes);
        if (securityUser.getSessionId() != null) {
            claims.put(SESSION_ID, securityUser.getSessionId());
        }

        ZonedDateTime currentTime = ZonedDateTime.now();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtSettingsService.getJwtSettings().getTokenIssuer())
                .setIssuedAt(Date.from(currentTime.toInstant()))
                .setExpiration(Date.from(currentTime.plusSeconds(expirationTime).toInstant()))
                .signWith(SignatureAlgorithm.HS512, jwtSettingsService.getJwtSettings().getTokenSigningKey());
    }

    public Jws<Claims> parseTokenClaims(JwtToken token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSettingsService.getJwtSettings().getTokenSigningKey())
                    .parseClaimsJws(token.getToken());
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (SignatureException | ExpiredJwtException expiredEx) {
            log.debug("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(token, "JWT Token expired", expiredEx);
        }
    }

    public JwtPair createTokenPair(SecurityUser securityUser) {
        JwtToken accessToken = createAccessJwtToken(securityUser);
        JwtToken refreshToken = createRefreshToken(securityUser);
        return new JwtPair(accessToken.getToken(), refreshToken.getToken());
    }

}
