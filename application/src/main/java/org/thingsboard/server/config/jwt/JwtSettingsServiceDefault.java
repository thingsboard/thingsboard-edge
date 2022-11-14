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
package org.thingsboard.server.config.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.config.jwt.JwtSettings.ADMIN_SETTINGS_JWT_KEY;
import static org.thingsboard.server.config.jwt.JwtSettings.TOKEN_SIGNING_KEY_DEFAULT;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtSettingsServiceDefault implements JwtSettingsService {

    @Lazy
    private final AdminSettingsService adminSettingsService;
    @Lazy
    private final Optional<TbClusterService> tbClusterService;
    private final JwtSettingsValidator jwtSettingsValidator;
    private final Environment environment;
    @Getter
    private final JwtSettings jwtSettings;
    @Value("${install.upgrade:false}")
    private boolean isUpgrade;

    @PostConstruct
    public void init() {
        if (!isFirstInstall()) {
            reloadJwtSettings();
        }
    }

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    private boolean isFirstInstall() {
        return isInstall() && !isUpgrade;
    }

    @Override
    public void reloadJwtSettings() {
        AdminSettings adminJwtSettings = findJwtAdminSettings();
        if (adminJwtSettings != null) {
            log.info("Reloading the JWT admin settings from database");
            JwtSettings jwtLoaded = mapAdminToJwtSettings(adminJwtSettings);
            jwtSettings.setRefreshTokenExpTime(jwtLoaded.getRefreshTokenExpTime());
            jwtSettings.setTokenExpirationTime(jwtLoaded.getTokenExpirationTime());
            jwtSettings.setTokenIssuer(jwtLoaded.getTokenIssuer());
            jwtSettings.setTokenSigningKey(jwtLoaded.getTokenSigningKey());
        }

        if (hasDefaultTokenSigningKey()) {
            log.warn("WARNING: The platform is configured to use default JWT Signing Key. " +
                    "This is a security issue that needs to be resolved. Please change the JWT Signing Key using the Web UI. " +
                    "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.");
        }
    }

    JwtSettings mapAdminToJwtSettings(AdminSettings adminSettings) {
        Objects.requireNonNull(adminSettings, "adminSettings for JWT is null");
        return JacksonUtil.treeToValue(adminSettings.getJsonValue(), JwtSettings.class);
    }

    AdminSettings mapJwtToAdminSettings(JwtSettings jwtSettings) {
        Objects.requireNonNull(jwtSettings, "jwtSettings is null");
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminJwtSettings.setKey(ADMIN_SETTINGS_JWT_KEY);
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(jwtSettings));
        return adminJwtSettings;
    }

    boolean hasDefaultTokenSigningKey() {
        return TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey());
    }

    /**
     * Create JWT admin settings is intended to be called from Install or Upgrade scripts
     * */
    @Override
    public void createJwtAdminSettings() {
        log.info("Creating JWT admin settings...");
        Objects.requireNonNull(jwtSettings, "JWT settings is null");
        if (isJwtAdminSettingsNotExists()) {
            if (hasDefaultTokenSigningKey() && isFirstInstall()) {
                log.info("JWT token signing key is default. Generating a new random key");
                jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                        RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
            }
            saveJwtSettings(jwtSettings);
        }
    }

    @Override
    public JwtSettings saveJwtSettings(JwtSettings jwtSettings) {
        jwtSettingsValidator.validate(jwtSettings);
        final AdminSettings adminJwtSettings = mapJwtToAdminSettings(jwtSettings);
        final AdminSettings existedSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (existedSettings != null) {
            adminJwtSettings.setId(existedSettings.getId());
        }

        log.info("Saving new JWT admin settings. From this moment, the JWT parameters from YAML and ENV will be ignored");
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminJwtSettings);

        if (!isInstall()) {
            tbClusterService.orElseThrow().broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED);
        }
        reloadJwtSettings();
        return getJwtSettings();
    }

    boolean isJwtAdminSettingsNotExists() {
        return findJwtAdminSettings() == null;
    }

    AdminSettings findJwtAdminSettings() {
        return adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
    }

}
