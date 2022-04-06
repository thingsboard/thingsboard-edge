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

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.GroupEntityExportData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class BaseGroupEntityExportService<I extends EntityId, E extends ExportableEntity<I> & GroupEntity<I>, D extends GroupEntityExportData<E>> extends BaseEntityExportService<I, E, D> {

    @Autowired
    private EntityGroupService entityGroupService;

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @Override
    protected final void setAdditionalExportData(SecurityUser user, E entity, D exportData, EntityExportSettings exportSettings) throws ThingsboardException {
        super.setAdditionalExportData(user, entity, exportData, exportSettings);

        if (exportSettings.isExportEntityGroupsInfo()) {
            List<EntityGroupId> entityGroupsIds = entityGroupService.findEntityGroupsForEntity(user.getTenantId(), entity.getId()).get().stream()
                    .filter(entityGroupId -> !entityGroupService.findEntityGroupById(user.getTenantId(), entityGroupId).isGroupAll())
                    .collect(Collectors.toList());
            for (EntityGroupId entityGroupId : entityGroupsIds) {
                exportableEntitiesService.checkPermission(user, entityGroupId, Operation.READ);
            }
            exportData.setEntityGroupsIds(entityGroupsIds);
        }
    }

}
