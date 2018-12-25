/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public interface EntityViewService {

    EntityView saveEntityView(EntityView entityView);

    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId);

    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId);

    void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    TextPageData<EntityView> findEntityViewByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, TextPageLink pageLink, String type);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(TenantId tenantId, List<EntityViewId> entityViewIds);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, TextPageLink pageLink, String type);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    void deleteEntityViewsByTenantId(TenantId tenantId);

    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);

    ShortEntityView findGroupEntityView(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    ListenableFuture<TimePageData<ShortEntityView>> findEntityViewsByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink);

}
