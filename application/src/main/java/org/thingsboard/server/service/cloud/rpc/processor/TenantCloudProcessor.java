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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TenantCloudProcessor extends BaseEdgeProcessor {

    public void createTenantIfNotExists(TenantId tenantId, Long queueStartTs) throws Exception {
        try {
            cloudSynchronizationManager.getSync().set(true);
            Tenant tenant = tenantService.findTenantById(tenantId);
            if (tenant != null) {
                return;
            }
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            tenant.setId(tenantId);
            tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
            Tenant savedTenant = tenantService.saveTenant(tenant, false);
            apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);

            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.CUSTOMER);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ASSET);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DEVICE);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ENTITY_VIEW);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.EDGE);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DASHBOARD);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.USER);

            requestForAdditionalData(tenantId, tenantId, queueStartTs).get();
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public ListenableFuture<Void> processTenantMsgFromCloud(TenantUpdateMsg tenantUpdateMsg) {
        TenantId entityId = new TenantId(new UUID(tenantUpdateMsg.getIdMSB(), tenantUpdateMsg.getIdLSB()));
        TenantProfileId tenantProfileId = new TenantProfileId(new UUID(tenantUpdateMsg.getProfileIdMSB(), tenantUpdateMsg.getProfileIdLSB()));
        switch (tenantUpdateMsg.getMsgType()) {
            case ENTITY_UPDATED_RPC_MESSAGE:
                processTenantUpdatedMessage(entityId, tenantUpdateMsg, tenantProfileId);
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(tenantUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private void processTenantUpdatedMessage(TenantId entityId, TenantUpdateMsg tenantUpdateMsg, TenantProfileId tenantProfileId) {
        Tenant tenant = tenantService.findTenantById(entityId);
        if (tenant == null) {
            tenant = new Tenant();
            tenant.setId(entityId);
            tenant.setCreatedTime(Uuids.unixTimestamp(entityId.getId()));
        }
        updateTenantProperties(tenant, tenantUpdateMsg, tenantProfileId);
        Tenant savedTenant = tenantService.saveTenant(tenant, false);
        notifyCluster(entityId, savedTenant);
    }

    public void cleanUp() {
        try {
            cloudSynchronizationManager.getSync().set(true);
            log.debug("Starting clean up procedure");
            PageData<Tenant> tenants = tenantService.findTenants(new PageLink(Integer.MAX_VALUE));
            for (Tenant tenant : tenants.getData()) {
                tenantService.deleteTenant(tenant.getId());
            }

            cleanUpSystemTenant();
            log.debug("Clean up procedure successfully finished!");
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void cleanUpSystemTenant() {
        try {
            adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mailTemplates");
            adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            queueService.deleteQueuesByTenantId(TenantId.SYS_TENANT_ID);
            widgetsBundleService.deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
            List<AttributeKvEntry> attributeKvEntries =
                    attributesService.findAll(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, DataConstants.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            attributesService.removeAll(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, DataConstants.SERVER_SCOPE, attrKeys);
            roleService.deleteRolesByTenantId(TenantId.SYS_TENANT_ID);
            whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
            whiteLabelingService.saveSystemWhiteLabelingParams(new WhiteLabelingParams());
            customTranslationService.saveSystemCustomTranslation(new CustomTranslation());
        } catch (Exception e) {
            log.error("Unable to clean up sysadmin tenant", e);
        }
    }

    private void notifyCluster(TenantId tenantId, Tenant savedTenant) {
        tbClusterService.onTenantChange(savedTenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedTenant.getId(), ComponentLifecycleEvent.UPDATED);
    }

    private void updateTenantProperties(Tenant tenant, TenantUpdateMsg tenantUpdateMsg, TenantProfileId tenantProfileId) {
        tenant.setTitle(tenantUpdateMsg.getTitle());
        tenant.setTenantProfileId(tenantProfileId);
        tenant.setRegion(tenantUpdateMsg.getRegion());

        if (tenantUpdateMsg.hasCountry()) {
            tenant.setCountry(tenantUpdateMsg.getCountry());
        }

        if (tenantUpdateMsg.hasState()) {
            tenant.setState(tenantUpdateMsg.getState());
        }

        if (tenantUpdateMsg.hasCity()) {
            tenant.setCity(tenantUpdateMsg.getCity());
        }

        if (tenantUpdateMsg.hasZip()) {
            tenant.setZip(tenantUpdateMsg.getZip());
        }

        if (tenantUpdateMsg.hasAddress()) {
            tenant.setAddress(tenantUpdateMsg.getAddress());
        }

        if (tenantUpdateMsg.hasAddress2()) {
            tenant.setAddress2(tenantUpdateMsg.getAddress2());
        }

        if (tenantUpdateMsg.hasPhone()) {
            tenant.setPhone(tenantUpdateMsg.getPhone());
        }

        if (tenantUpdateMsg.hasEmail()) {
            tenant.setEmail(tenantUpdateMsg.getEmail());
        }

        if (tenantUpdateMsg.hasAdditionalInfo()) {
            tenant.setAdditionalInfo(JacksonUtil.toJsonNode(tenantUpdateMsg.getAdditionalInfo()));
        }
    }

}
