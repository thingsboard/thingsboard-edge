/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@TbCoreComponent
public class TenantCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    public void createTenantIfNotExists(TenantId tenantId) throws Exception {
        try {
            cloudSynchronizationManager.getSync().set(true);
            Tenant tenant = edgeCtx.getTenantService().findTenantById(tenantId);
            if (tenant != null) {
                return;
            }
            tenant = new Tenant();
            tenant.setTitle("Tenant");
            tenant.setId(tenantId);
            tenant.setCreatedTime(Uuids.unixTimestamp(tenantId.getId()));
            Tenant savedTenant = edgeCtx.getTenantService().saveTenant(tenant, null, false);

            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.CUSTOMER);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ASSET);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DEVICE);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ENTITY_VIEW);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.EDGE);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DASHBOARD);
            edgeCtx.getEntityGroupService().createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.USER);

            requestForAdditionalData(tenantId, tenantId).get();

            try {
                var apiUsageState = apiUsageStateService.findApiUsageStateByEntityId(savedTenant.getId());
                if (apiUsageState == null) {
                    apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
                }
            } catch (Exception ignored) {}
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public ListenableFuture<Void> processTenantMsgFromCloud(TenantUpdateMsg tenantUpdateMsg) {
        Tenant tenant = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        if (tenant == null) {
            throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] tenantUpdateMsg {" + tenantUpdateMsg + "} cannot be converted to tenant");
        }
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (tenantUpdateMsg.getMsgType()) {
                case ENTITY_UPDATED_RPC_MESSAGE:
                    edgeCtx.getTenantService().saveTenant(tenant, null, false);
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(tenantUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    public void cleanUp() {
        try {
            cloudSynchronizationManager.getSync().set(true);
            log.debug("Starting clean up procedure");
            PageData<Tenant> tenants = edgeCtx.getTenantService().findTenants(new PageLink(Integer.MAX_VALUE));
            for (Tenant tenant : tenants.getData()) {
                removeTenantAttributes(tenant.getId());
                edgeCtx.getTenantService().deleteTenant(tenant.getId());
            }

            cleanUpSystemTenant();
            log.debug("Clean up procedure successfully finished!");
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void cleanUpSystemTenant() {
        edgeCtx.getAdminSettingsService().deleteAdminSettingsByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getQueueService().deleteQueuesByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getWidgetTypeService().deleteWidgetTypesByTenantId(TenantId.SYS_TENANT_ID);
        edgeCtx.getWidgetsBundleService().deleteWidgetsBundlesByTenantId(TenantId.SYS_TENANT_ID);
        removeTenantAttributes(TenantId.SYS_TENANT_ID);
        edgeCtx.getRoleService().deleteRolesByTenantId(TenantId.SYS_TENANT_ID);
        whiteLabelingService.saveSystemLoginWhiteLabelingParams(new LoginWhiteLabelingParams());
        whiteLabelingService.saveSystemWhiteLabelingParams(new WhiteLabelingParams());
        edgeCtx.getCustomTranslationService().deleteCustomTranslationByTenantId(TenantId.SYS_TENANT_ID);
    }

    private void removeTenantAttributes(TenantId tenantId) {
        try {
            List<AttributeKvEntry> attributeKvEntries = edgeCtx.getAttributesService().findAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE).get();
            List<String> attrKeys = attributeKvEntries.stream().map(KvEntry::getKey).collect(Collectors.toList());
            edgeCtx.getAttributesService().removeAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKeys);
        } catch (Exception e) {
            log.error("Unable to remove tenant attributes", e);
        }
    }

}
