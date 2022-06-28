/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.security.auth;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenOutdatingTest {
    private JwtAuthenticationProvider accessTokenAuthenticationProvider;
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    private TokenOutdatingService tokenOutdatingService;
    private ConcurrentMapCacheManager cacheManager;
    private JwtTokenFactory tokenFactory;
    private JwtSettings jwtSettings;

    private UserId userId;

    @BeforeEach
    public void setUp() throws ThingsboardException {
        jwtSettings = new JwtSettings();
        jwtSettings.setTokenIssuer("test.io");
        jwtSettings.setTokenExpirationTime((int) MINUTES.toSeconds(10));
        jwtSettings.setRefreshTokenExpTime((int) DAYS.toSeconds(7));
        jwtSettings.setTokenSigningKey("secret");

        UserPermissionsService userPermissionsService = mock(UserPermissionsService.class);
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());
        when(userPermissionsService.getMergedPermissions(any(), eq(false))).thenReturn(mergedUserPermissions);

        tokenFactory = new JwtTokenFactory(jwtSettings, userPermissionsService);

        cacheManager = new ConcurrentMapCacheManager();
        tokenOutdatingService = new TokenOutdatingService(cacheManager, tokenFactory, jwtSettings);
        tokenOutdatingService.initCache();

        userId = new UserId(UUID.randomUUID());

        UserService userService = mock(UserService.class);

        User user = new User();
        user.setId(userId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("email");
        when(userService.findUserById(any(), eq(userId))).thenReturn(user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        when(userService.findUserCredentialsByUserId(any(), eq(userId))).thenReturn(userCredentials);

        accessTokenAuthenticationProvider = new JwtAuthenticationProvider(tokenFactory, tokenOutdatingService);
        refreshTokenAuthenticationProvider = new RefreshTokenAuthenticationProvider(tokenFactory, userPermissionsService, userService, mock(CustomerService.class), tokenOutdatingService);
    }

    @Test
    public void testOutdateOldUserTokens() throws Exception {
        JwtToken jwtToken = createAccessJwtToken(userId);

        SECONDS.sleep(1); // need to wait before outdating so that outdatage time is strictly after token issue time
        tokenOutdatingService.onUserAuthDataChanged(new UserAuthDataChangedEvent(userId));
        assertTrue(tokenOutdatingService.isOutdated(jwtToken, userId));

        SECONDS.sleep(1);

        JwtToken newJwtToken = tokenFactory.createAccessJwtToken(createMockSecurityUser(userId));
        assertFalse(tokenOutdatingService.isOutdated(newJwtToken, userId));
    }

    @Test
    public void testAuthenticateWithOutdatedAccessToken() throws InterruptedException {
        RawAccessJwtToken accessJwtToken = getRawJwtToken(createAccessJwtToken(userId));

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });

        SECONDS.sleep(1);
        tokenOutdatingService.onUserAuthDataChanged(new UserAuthDataChangedEvent(userId));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });
    }

    @Test
    public void testAuthenticateWithOutdatedRefreshToken() throws InterruptedException {
        RawAccessJwtToken refreshJwtToken = getRawJwtToken(createRefreshJwtToken(userId));

        assertDoesNotThrow(() -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });

        SECONDS.sleep(1);
        tokenOutdatingService.onUserAuthDataChanged(new UserAuthDataChangedEvent(userId));

        assertThrows(CredentialsExpiredException.class, () -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });
    }

    @Test
    public void testTokensOutdatageTimeRemovalFromCache() throws Exception {
        JwtToken jwtToken = createAccessJwtToken(userId);

        SECONDS.sleep(1);
        tokenOutdatingService.onUserAuthDataChanged(new UserAuthDataChangedEvent(userId));

        int refreshTokenExpirationTime = 3;
        jwtSettings.setRefreshTokenExpTime(refreshTokenExpirationTime);

        SECONDS.sleep(refreshTokenExpirationTime - 2);

        assertTrue(tokenOutdatingService.isOutdated(jwtToken, userId));
        assertNotNull(cacheManager.getCache(CacheConstants.USERS_UPDATE_TIME_CACHE).get(userId.getId().toString()));

        SECONDS.sleep(3);

        assertFalse(tokenOutdatingService.isOutdated(jwtToken, userId));
        assertNull(cacheManager.getCache(CacheConstants.USERS_UPDATE_TIME_CACHE).get(userId.getId().toString()));
    }

    private JwtToken createAccessJwtToken(UserId userId) {
        return tokenFactory.createAccessJwtToken(createMockSecurityUser(userId));
    }

    private JwtToken createRefreshJwtToken(UserId userId) {
        return tokenFactory.createRefreshToken(createMockSecurityUser(userId));
    }

    private RawAccessJwtToken getRawJwtToken(JwtToken token) {
        return new RawAccessJwtToken(token.getToken());
    }

    private SecurityUser createMockSecurityUser(UserId userId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setEmail("email");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setAuthority(Authority.CUSTOMER_USER);
        securityUser.setId(userId);
        return securityUser;
    }
}
