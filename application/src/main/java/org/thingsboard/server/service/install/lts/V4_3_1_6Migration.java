/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.SystemDataLoaderService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Drops the admin settings the cloud used to sync down to the edge. From this version on the edge delegates
 * mail/SMS/notification sends to the cloud, so the local copies are dead weight - their provider secrets are
 * encrypted with the system key, which never leaves the cloud. Only "general" and "connectivity" are kept;
 * the edge generates those itself on install.
 * <p>
 * The system "jwt" settings are regenerated rather than left absent, so the edge signs tokens with a key the
 * cloud never held. That rotates the signing key once, invalidating tokens issued before the upgrade.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_6Migration implements LtsMigration {

    private static final int DEFAULT_PAGE_SIZE = 1024;
    private static final Set<String> EDGE_OWNED_SETTINGS_KEYS = Set.of("general", "connectivity");
    private static final String JWT_KEY = "jwt";

    private final TenantService tenantService;
    private final AdminSettingsService adminSettingsService;
    private final SystemDataLoaderService systemDataLoaderService;

    @Override
    public String getVersion() {
        return "4.3.1.6";
    }

    @Override
    public void apply() {
        log.info("Purging admin settings");
        List<TenantId> scopes = new ArrayList<>();
        scopes.add(TenantId.SYS_TENANT_ID);
        new PageDataIterable<>(tenantService::findTenantsIds, DEFAULT_PAGE_SIZE).forEach(scopes::add);
        boolean systemJwtRemoved = false;
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
                if (TenantId.SYS_TENANT_ID.equals(scope) && JWT_KEY.equals(key)) {
                    systemJwtRemoved = true;
                }
            }
            if (!keysToDelete.isEmpty()) {
                log.info("Purged {} admin settings for tenant [{}]: {}", keysToDelete.size(), scope, keysToDelete);
            }
        }
        if (systemJwtRemoved) {
            regenerateSystemJwtSettings();
        }
    }

    // Leaving the edge with no JWT settings would break token signing, so fail the upgrade instead.
    private void regenerateSystemJwtSettings() {
        try {
            systemDataLoaderService.createRandomJwtSettings();
        } catch (Exception e) {
            throw new RuntimeException("Failed to regenerate system JWT settings after admin settings purge", e);
        }
    }

}
