// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.lts;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class V4_3_1_8MigrationTest extends AbstractControllerTest {

    @Autowired
    private V4_3_1_8Migration migration;
    @Autowired
    private AdminSettingsService adminSettingsService;
    @Autowired
    private JwtSettingsService jwtSettingsService;

    private JwtSettings originalJwtSettings;

    @Before
    public void saveJwtSettings() {
        originalJwtSettings = jwtSettingsService.reloadJwtSettings();
    }

    @After
    public void restoreJwtSettings() {
        jwtSettingsService.saveJwtSettings(originalJwtSettings);
    }

    @Test
    public void purgesSyncedAdminSettingsAndRotatesJwtSigningKey() {
        saveSettings(TenantId.SYS_TENANT_ID, "mail");
        saveSettings(TenantId.SYS_TENANT_ID, "connectivity");
        saveSettings(tenantId, "notifications");
        saveSettings(tenantId, "general");
        String signingKeyBefore = jwtSettingsService.reloadJwtSettings().getTokenSigningKey();

        migration.apply();

        assertThat(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "mail")).isNull();
        assertThat(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, "notifications")).isNull();
        assertThat(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "connectivity")).isNotNull();
        assertThat(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, "general")).isNotNull();
        JwtSettings jwtSettings = jwtSettingsService.getJwtSettings();
        assertThat(jwtSettings).isNotNull();
        assertThat(jwtSettings.getTokenSigningKey()).isNotEqualTo(signingKeyBefore);
        assertThat(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, JwtSettingsService.ADMIN_SETTINGS_JWT_KEY)
                .getJsonValue().get("tokenSigningKey").asText()).isEqualTo(jwtSettings.getTokenSigningKey());
    }

    private void saveSettings(TenantId scope, String key) {
        AdminSettings settings = adminSettingsService.findAdminSettingsByTenantIdAndKey(scope, key);
        if (settings == null) {
            settings = new AdminSettings();
            settings.setTenantId(scope);
            settings.setKey(key);
        }
        settings.setJsonValue(JacksonUtil.newObjectNode().put("value", key));
        adminSettingsService.saveAdminSettings(scope, settings);
    }

}
