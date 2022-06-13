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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.GroupEntityExportData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.utils.RegexUtils;

import java.util.ArrayList;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseGroupEntityImportService<DashboardId, Dashboard, GroupEntityExportData<Dashboard>> {

    private final DashboardService dashboardService;

    @Override
    protected void setOwner(TenantId tenantId, Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(idProvider.getInternalId(dashboard.getCustomerId()));
    }

    @Override
    protected Dashboard findExistingEntity(EntitiesImportCtx ctx, Dashboard dashboard) {
        Dashboard existingDashboard = super.findExistingEntity(ctx, dashboard);
        if (existingDashboard == null && ctx.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(ctx.getTenantId(), dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @Override
    protected Dashboard prepareAndSave(EntitiesImportCtx ctx, Dashboard dashboard, Dashboard old, GroupEntityExportData<Dashboard> exportData, IdProvider idProvider) {
        JsonNode configuration = dashboard.getConfiguration();
        JsonNode entityAliases = configuration.get("entityAliases");
        if (entityAliases != null && entityAliases.isObject()) {
            for (JsonNode entityAlias : entityAliases) {
                ArrayList<String> fields = Lists.newArrayList(entityAlias.fieldNames());
                for (String field : fields) {
                    if (field.equals("id")) continue;
                    JsonNode oldFieldValue = entityAlias.get(field);
                    JsonNode newFieldValue = JacksonUtil.toJsonNode(RegexUtils.replace(oldFieldValue.toString(), RegexUtils.UUID_PATTERN, uuid -> {
                        return idProvider.getInternalIdByUuid(UUID.fromString(uuid))
                                .map(entityId -> entityId.getId().toString()).orElse(uuid);
                    }));
                    ((ObjectNode) entityAlias).set(field, newFieldValue);
                }
            }
        }

        return dashboardService.saveDashboard(dashboard);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
