// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudSynchronizationManager;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantCloudProcessorTest {

    @Mock
    private EdgeContextComponent edgeCtx;
    @Mock
    private CloudSynchronizationManager cloudSynchronizationManager;
    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private TenantService tenantService;
    @Mock
    private QueueService queueService;
    @Mock
    private WidgetTypeService widgetTypeService;
    @Mock
    private WidgetsBundleService widgetsBundleService;
    @Mock
    private AttributesService attributesService;

    private TenantCloudProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new TenantCloudProcessor();
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        ReflectionTestUtils.setField(processor, "cloudSynchronizationManager", cloudSynchronizationManager);
        when(cloudSynchronizationManager.getSync()).thenReturn(new ThreadLocal<>());
        when(edgeCtx.getAdminSettingsService()).thenReturn(adminSettingsService);
        when(edgeCtx.getTenantService()).thenReturn(tenantService);
        when(edgeCtx.getQueueService()).thenReturn(queueService);
        when(edgeCtx.getWidgetTypeService()).thenReturn(widgetTypeService);
        when(edgeCtx.getWidgetsBundleService()).thenReturn(widgetsBundleService);
        when(edgeCtx.getAttributesService()).thenReturn(attributesService);
        when(tenantService.findTenants(any(PageLink.class))).thenReturn(PageData.emptyPageData());
        when(attributesService.findAll(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE))
                .thenReturn(Futures.immediateFuture(List.of()));
    }

    @Test
    public void cleanUpKeepsJwtSettings() {
        List<AdminSettings> settings = List.of(adminSettings("general"), adminSettings("mail"),
                adminSettings(JwtSettingsService.ADMIN_SETTINGS_JWT_KEY), adminSettings("connectivity"));
        when(adminSettingsService.findAllByTenantId(eq(TenantId.SYS_TENANT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(settings, 1, settings.size(), false));

        processor.cleanUp();

        verify(adminSettingsService).deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "general");
        verify(adminSettingsService).deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "mail");
        verify(adminSettingsService).deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "connectivity");
        verify(adminSettingsService, never()).deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, JwtSettingsService.ADMIN_SETTINGS_JWT_KEY);
        verify(adminSettingsService, never()).deleteByTenantId(any());
    }

    private static AdminSettings adminSettings(String key) {
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminSettings.setKey(key);
        return adminSettings;
    }

}
