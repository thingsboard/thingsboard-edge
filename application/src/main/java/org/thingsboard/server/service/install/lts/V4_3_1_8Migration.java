// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Drops the admin settings the cloud used to sync down to the edge. From this version on the edge delegates
 * mail/SMS/notification sends to the cloud, so the local copies are dead weight - their provider secrets are
 * encrypted with the system key, which never leaves the cloud. Only "general", "connectivity" and "jwt" are kept.
 * <p>
 * The signing key of the system "jwt" settings is rotated, so the edge signs tokens with a key the cloud never
 * held. That invalidates tokens issued before the upgrade.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_8Migration implements LtsMigration {

    private static final int DEFAULT_PAGE_SIZE = 1024;
    private static final Set<String> EDGE_OWNED_SETTINGS_KEYS = Set.of("general", "connectivity", JwtSettingsService.ADMIN_SETTINGS_JWT_KEY);

    private final TenantService tenantService;
    private final AdminSettingsService adminSettingsService;
    private final JwtSettingsService jwtSettingsService;

    @Override
    public String getVersion() {
        return "4.3.1.8";
    }

    @Override
    public void apply() {
        log.info("Purging admin settings");
        List<TenantId> scopes = new ArrayList<>();
        scopes.add(TenantId.SYS_TENANT_ID);
        new PageDataIterable<>(tenantService::findTenantsIds, DEFAULT_PAGE_SIZE).forEach(scopes::add);
        for (TenantId scope : scopes) {
            List<String> keysToDelete = new ArrayList<>();
            PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
            PageData<AdminSettings> page;
            do {
                page = adminSettingsService.findAllByTenantId(scope, pageLink);
                for (AdminSettings adminSettings : page.getData()) {
                    if (!EDGE_OWNED_SETTINGS_KEYS.contains(adminSettings.getKey())) {
                        keysToDelete.add(adminSettings.getKey());
                    }
                }
                pageLink = pageLink.nextPageLink();
            } while (page.hasNext());
            for (String key : keysToDelete) {
                adminSettingsService.deleteAdminSettingsByTenantIdAndKey(scope, key);
            }
            if (!keysToDelete.isEmpty()) {
                log.info("Purged {} admin settings for tenant [{}]: {}", keysToDelete.size(), scope, keysToDelete);
            }
        }
        rotateJwtSigningKey();
    }

    private void rotateJwtSigningKey() {
        JwtSettings jwtSettings = jwtSettingsService.reloadJwtSettings();
        if (jwtSettings == null) {
            return;
        }
        log.info("Rotating JWT signing key");
        jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                RandomStringUtils.secure().nextAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
        jwtSettingsService.saveJwtSettings(jwtSettings);
    }

}
