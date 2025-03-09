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
package org.thingsboard.server.service.security.auth;

import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsValidator;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtTokenFactoryTest {

    private JwtTokenFactory tokenFactory;
    private AdminSettingsService adminSettingsService;
    private JwtSettingsService jwtSettingsService;

    private JwtSettings jwtSettings;

    @Before
    public void beforeEach() {
        jwtSettings = new JwtSettings();
        jwtSettings.setTokenIssuer("tb");
        jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
        jwtSettings.setTokenExpirationTime((int) TimeUnit.HOURS.toSeconds(2));
        jwtSettings.setRefreshTokenExpTime((int) TimeUnit.DAYS.toSeconds(7));

        adminSettingsService = mock(AdminSettingsService.class);
        jwtSettingsService = mockJwtSettingsService();
        mockJwtSettings(jwtSettings);

        tokenFactory = new JwtTokenFactory(jwtSettingsService, Mockito.mock(UserPermissionsService.class));
    }

    @Test
    public void testCreateAndParseAccessJwtToken() {
        SecurityUser securityUser = createSecurityUser();

        testCreateAndParseAccessJwtToken(securityUser);

        securityUser = new SecurityUser(securityUser, true, new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, securityUser.getEmail()), null);
        securityUser.setFirstName(null);
        securityUser.setLastName(null);
        securityUser.setCustomerId(null);

        testCreateAndParseAccessJwtToken(securityUser);
    }

    public void testCreateAndParseAccessJwtToken(SecurityUser securityUser) {
        AccessJwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
        checkExpirationTime(accessToken, jwtSettings.getTokenExpirationTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(accessToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getEmail()).isEqualTo(securityUser.getEmail());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthorities()).isEqualTo(securityUser.getAuthorities());
        assertThat(parsedSecurityUser.isEnabled()).isEqualTo(securityUser.isEnabled());
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getFirstName()).isEqualTo(securityUser.getFirstName());
        assertThat(parsedSecurityUser.getLastName()).isEqualTo(securityUser.getLastName());
        assertThat(parsedSecurityUser.getSessionId()).isNotNull().isEqualTo(securityUser.getSessionId());
    }

    @Test
    public void testCreateAndParseRefreshJwtToken() {
        SecurityUser securityUser = createSecurityUser();

        JwtToken refreshToken = tokenFactory.createRefreshToken(securityUser);
        checkExpirationTime(refreshToken, jwtSettings.getRefreshTokenExpTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseRefreshToken(refreshToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthority()).isNull();
    }

    @Test
    public void testCreateAndParsePreVerificationJwtToken() {
        SecurityUser securityUser = createSecurityUser();
        int tokenLifetime = (int) TimeUnit.MINUTES.toSeconds(30);
        JwtToken preVerificationToken = tokenFactory.createPreVerificationToken(securityUser, tokenLifetime);
        checkExpirationTime(preVerificationToken, tokenLifetime);

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(preVerificationToken.getToken());
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getAuthority()).isEqualTo(Authority.PRE_VERIFICATION_TOKEN);
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType() == UserPrincipal.Type.USER_NAME
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
    }

    @Test
    public void testGetExpirationTimeFromClaims() {
        SecurityUser securityUser = createSecurityUser();

        AccessJwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);

        Claims claims = accessToken.getClaims();
        assertThat(claims.getExpiration()).matches(actualExpirationTime -> {
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.setTime(new Date());
            expirationTime.add(Calendar.SECOND, jwtSettings.getTokenExpirationTime());
            if (actualExpirationTime.equals(expirationTime.getTime())) {
                return true;
            } else if (actualExpirationTime.before(expirationTime.getTime())) {
                int gap = 2;
                expirationTime.add(Calendar.SECOND, -gap);
                return actualExpirationTime.after(expirationTime.getTime());
            } else {
                return false;
            }
        });
    }

    @Test
    public void testSessionId() {
        SecurityUser securityUser = createSecurityUser();
        String sessionId = securityUser.getSessionId();

        String accessToken = tokenFactory.createAccessJwtToken(securityUser).getToken();
        securityUser = tokenFactory.parseAccessJwtToken(accessToken);
        assertThat(securityUser.getSessionId()).isNotNull().isEqualTo(sessionId);

        String newAccessToken = tokenFactory.createTokenPair(securityUser).getToken();
        securityUser = tokenFactory.parseAccessJwtToken(newAccessToken);
        assertThat(securityUser.getSessionId()).isNotNull().isNotEqualTo(sessionId);
    }

    private SecurityUser createSecurityUser() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setEnabled(true);
        securityUser.setFirstName("A");
        securityUser.setLastName("B");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));
        return securityUser;
    }

    private void mockJwtSettings(JwtSettings settings) {
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, JwtSettingsService.ADMIN_SETTINGS_JWT_KEY))
                .thenReturn(adminJwtSettings);
    }

    private DefaultJwtSettingsService mockJwtSettingsService() {
        return new DefaultJwtSettingsService(adminSettingsService, Optional.empty(), new DefaultJwtSettingsValidator(), Optional.empty());
    }

    private void checkExpirationTime(JwtToken jwtToken, int tokenLifetime) {
        Claims claims = tokenFactory.parseTokenClaims(jwtToken.getToken()).getPayload();
        assertThat(claims.getExpiration()).matches(actualExpirationTime -> {
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.setTime(new Date());
            expirationTime.add(Calendar.SECOND, tokenLifetime);
            if (actualExpirationTime.equals(expirationTime.getTime())) {
                return true;
            } else if (actualExpirationTime.before(expirationTime.getTime())) {
                int gap = 2;
                expirationTime.add(Calendar.SECOND, -gap);
                return actualExpirationTime.after(expirationTime.getTime());
            } else {
                return false;
            }
        });
    }

}
