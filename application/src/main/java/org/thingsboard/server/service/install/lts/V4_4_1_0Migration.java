// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

/**
 * Migration for version {@code 4.4.1.0}.
 * <p>
 * {@link MigrationService} selects migrations from the injected {@link Migration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories, so this bean is what makes the runner discover version
 * {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}.
 * <p>
 * It needs a slot of its own rather than {@code 4.4.0.0}: that version is already released, and
 * {@link Version#isInRange} is exclusive at the lower bound, so an installation already on {@code 4.4.0.0}
 * would never pick up a change added there.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code MigrationIntegrationTest}.
 * <p>
 * On the edge, {@link #apply()} drops the admin settings the cloud used to sync down to the edge. From this version on the edge delegates
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
public class V4_4_1_0Migration implements Migration {

    private static final int DEFAULT_PAGE_SIZE = 1024;
    private static final Set<String> EDGE_OWNED_SETTINGS_KEYS = Set.of("general", "connectivity", JwtSettingsService.ADMIN_SETTINGS_JWT_KEY);

    private final TenantService tenantService;
    private final AdminSettingsService adminSettingsService;
    private final JwtSettingsService jwtSettingsService;
    private final ApiKeyService apiKeyService;

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

    // Steps must be idempotent: a crash before the version is recorded re-runs them
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
        grantAiModelOperationsToTrendzApiKey();
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

    // Mirrors the AI_MODEL entries of DefaultTrendzSyncService.buildTrendzPermissions() for keys created before this version
    private void grantAiModelOperationsToTrendzApiKey() {
        Map<Authority, Set<Operation>> aiModelOperationsByAuthority = Map.of(
                Authority.TENANT_ADMIN, EnumSet.of(Operation.READ, Operation.INVOKE),
                Authority.CUSTOMER_USER, EnumSet.of(Operation.INVOKE));

        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || trendzApiKey.getPermissions() == null
            || trendzApiKey.getPermissions().getOperationsByResource() == null) {
            log.debug("No internal Trendz API key with stored permissions, nothing to grant");
            return;
        }

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                new HashMap<>(trendzApiKey.getPermissions().getOperationsByResource());
        boolean changed = false;

        for (var entry : aiModelOperationsByAuthority.entrySet()) {
            Authority authority = entry.getKey();
            Set<Operation> aiModelOperations = entry.getValue();
            Map<Resource, Set<Operation>> byResource = byAuthority.get(authority);
            if (byResource == null) {
                continue;
            }
            Map<Resource, Set<Operation>> updatedByResource = new HashMap<>(byResource);
            Set<Operation> operations = updatedByResource.get(Resource.AI_MODEL);
            if (operations != null && operations.containsAll(aiModelOperations)) {
                continue;
            }
            Set<Operation> updated = EnumSet.copyOf(aiModelOperations);
            if (operations != null) {
                updated.addAll(operations);
            }
            updatedByResource.put(Resource.AI_MODEL, updated);
            changed = true;
            byAuthority.put(authority, updatedByResource);
        }

        if (!changed) {
            log.debug("Internal Trendz API key already grants {} on {}", aiModelOperationsByAuthority, Resource.AI_MODEL);
            return;
        }

        trendzApiKey.getPermissions().setOperationsByResource(byAuthority);
        // Keeps the key's value and evicts the cached permissions
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, trendzApiKey);
        log.info("Granted {} on {} to the internal Trendz API key", aiModelOperationsByAuthority, Resource.AI_MODEL);
    }
}
