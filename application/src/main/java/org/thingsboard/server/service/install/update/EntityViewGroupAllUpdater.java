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
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;

class EntityViewGroupAllUpdater extends EntityGroupAllPaginatedUpdater<EntityViewId, EntityView> {

    private final EntityViewService entityViewService;

    public EntityViewGroupAllUpdater(EntityViewService entityViewService, CustomerService customerService,
                                     EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                (tenantId, pageLink) -> entityViewService.findEntityViewByTenantId(tenantId, pageLink),
                (tenantId, entityViewIds) -> entityViewService.findEntityViewsByTenantIdAndIdsAsync(tenantId, entityViewIds),
                entityId -> new EntityViewId(entityId.getId()),
                entityView -> entityView.getId());
        this.entityViewService = entityViewService;
    }


    @Override
    protected void unassignFromCustomer(EntityView entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        entityViewService.saveEntityView(entity);
    }

    @Override
    protected String getName() {
        return "Entity views group all updater";
    }

}
