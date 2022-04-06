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
package org.thingsboard.server.service.sync.exporting.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;

import java.util.List;

public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Autowired @Lazy
    protected ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;

    @Override
    public final D getExportData(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException("Entity not found");
        }
        exportableEntitiesService.checkPermission(user, entity, getEntityType(), Operation.READ);

        exportData.setEntity(entity);
        setRelatedEntities(user.getTenantId(), entity, exportData);
        setAdditionalExportData(user, entity, exportData, exportSettings);

        return exportData;
    }

    protected void setRelatedEntities(TenantId tenantId, E mainEntity, D exportData) {}

    protected void setAdditionalExportData(SecurityUser user, E entity, D exportData, EntityExportSettings exportSettings) throws ThingsboardException {
        if (exportSettings.isExportInboundRelations()) {
            List<EntityRelation> inboundRelations = relationService.findByTo(user.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
            if (inboundRelations != null) {
                for (EntityRelation relation : inboundRelations) {
                    exportableEntitiesService.checkPermission(user, relation.getFrom(), Operation.READ);
                }
            }
            exportData.setInboundRelations(inboundRelations);
        }
        if (exportSettings.isExportOutboundRelations()) {
            List<EntityRelation> outboundRelations = relationService.findByFrom(user.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
            if (outboundRelations != null) {
                for (EntityRelation relation : outboundRelations) {
                    exportableEntitiesService.checkPermission(user, relation.getTo(), Operation.READ);
                }
            }
            exportData.setOutboundRelations(outboundRelations);
        }
    }

    protected abstract D newExportData();

}
