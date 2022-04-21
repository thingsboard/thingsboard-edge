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
package org.thingsboard.server.service.sync.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.exporting.data.GroupEntityExportData;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseGroupEntityImportService<DashboardId, Dashboard, GroupEntityExportData<Dashboard>> {

    private final DashboardService dashboardService;

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Override
    protected void setOwner(TenantId tenantId, Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(idProvider.getInternalId(dashboard.getCustomerId()));
    }

    @Override
    protected Dashboard findExistingEntity(TenantId tenantId, Dashboard dashboard, EntityImportSettings importSettings) {
        Dashboard existingDashboard = super.findExistingEntity(tenantId, dashboard, importSettings);
        if (existingDashboard == null && importSettings.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(tenantId, dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @Override
    protected Dashboard prepareAndSave(TenantId tenantId, Dashboard dashboard, GroupEntityExportData<Dashboard> exportData, IdProvider idProvider) {
        Optional.ofNullable(dashboard.getConfiguration())
                .flatMap(configuration -> Optional.ofNullable(configuration.get("entityAliases")))
                .filter(JsonNode::isObject)
                .ifPresent(entityAliases -> entityAliases.forEach(entityAlias -> {
                    Optional.ofNullable(entityAlias.get("filter"))
                            .filter(JsonNode::isObject)
                            .ifPresent(filter -> {
                                EntityFilter entityFilter = JacksonUtil.treeToValue(filter, EntityFilter.class);
                                EntityType entityType = DefaultEntityQueryRepository.resolveEntityType(entityFilter);

                                String filterJson = filter.toString();
                                String newFilterJson = UUID_PATTERN.matcher(filterJson).replaceAll(matchResult -> {
                                    String uuid = matchResult.group();
                                    EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, uuid);
                                    return idProvider.getInternalId(entityId).toString();
                                });
                                ((ObjectNode) entityAlias).set("filter", JacksonUtil.toJsonNode(newFilterJson));
                            });
                }));

        return dashboardService.saveDashboard(dashboard);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
