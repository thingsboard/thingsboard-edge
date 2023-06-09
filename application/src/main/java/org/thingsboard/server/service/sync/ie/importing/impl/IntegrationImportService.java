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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class IntegrationImportService extends BaseEntityImportService<IntegrationId, Integration, EntityExportData<Integration>> {

    private final IntegrationService integrationService;
    private final IntegrationManagerService integrationManagerService;

    @Override
    protected void setOwner(TenantId tenantId, Integration integration, IdProvider idProvider) {
        integration.setTenantId(tenantId);
    }

    @Override
    protected Integration findExistingEntity(EntitiesImportCtx ctx, Integration integration, IdProvider idProvider) {
        Integration existingIntegration = super.findExistingEntity(ctx, integration, idProvider);
        if (existingIntegration == null && ctx.isFindExistingByName()) {
            existingIntegration = integrationService.findTenantIntegrationsByName(ctx.getTenantId(), integration.getName()).stream().findFirst().orElse(null);
        }
        return existingIntegration;
    }

    @Override
    protected Integration prepare(EntitiesImportCtx ctx, Integration integration, Integration oldEntity, EntityExportData<Integration> exportData, IdProvider idProvider) {
        if (ctx.isAutoGenerateIntegrationKey()) {
            if (integration.getId() == null) {
                integration.setRoutingKey(UUID.randomUUID().toString());
            } else {
                integration.setRoutingKey(oldEntity.getRoutingKey());
            }
        }
        integration.setDefaultConverterId(idProvider.getInternalId(integration.getDefaultConverterId()));
        integration.setDownlinkConverterId(idProvider.getInternalId(integration.getDownlinkConverterId()));
        return integration;
    }

    @Override
    protected Integration deepCopy(Integration integration) {
        return new Integration(integration);
    }

    //    @SneakyThrows({InterruptedException.class, ExecutionException.class, TimeoutException.class})
    @Override
    protected Integration saveOrUpdate(EntitiesImportCtx ctx, Integration integration, EntityExportData<Integration> exportData, IdProvider idProvider) {
        // Too aggressive operation
        // integrationManagerService.validateIntegrationConfiguration(integration).get(20, TimeUnit.SECONDS);
        return integrationService.saveIntegration(integration);
    }

    @Override
    protected void onEntitySaved(User user, Integration savedIntegration, Integration oldIntegration) throws ThingsboardException {
        super.onEntitySaved(user, savedIntegration, oldIntegration);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedIntegration.getId(),
                oldIntegration == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}
