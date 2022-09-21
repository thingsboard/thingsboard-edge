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
package org.thingsboard.rule.engine.util;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Optional;

public class EntitiesByNameAndTypeLoader {

    public static EntityId findEntityId(TbContext ctx, EntityType entityType, String entityName) {
        EntityId targetEntity = null;
        switch (entityType) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                if (device != null) {
                    targetEntity = device.getId();
                }
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                if (asset != null) {
                    targetEntity = asset.getId();
                }
                break;
            case CUSTOMER:
                Optional<Customer> customerOptional = ctx.getCustomerService().findCustomerByTenantIdAndTitle(ctx.getTenantId(), entityName);
                if (customerOptional.isPresent()) {
                    targetEntity = customerOptional.get().getId();
                }
                break;
            case TENANT:
                targetEntity = ctx.getTenantId();
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                if (entityView != null) {
                    targetEntity = entityView.getId();
                }
                break;
            case EDGE:
                Edge edge = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                if (edge != null) {
                    targetEntity = edge.getId();
                }
                break;
            case DASHBOARD:
                PageData<DashboardInfo> dashboardInfoTextPageData = ctx.getDashboardService().findDashboardsByTenantId(ctx.getTenantId(), new PageLink(200, 0, entityName));
                Optional<DashboardInfo> currentDashboardInfo = dashboardInfoTextPageData.getData().stream()
                        .filter(dashboardInfo -> dashboardInfo.getTitle().equals(entityName))
                        .findFirst();
                if (currentDashboardInfo.isPresent()) {
                    targetEntity = currentDashboardInfo.get().getId();
                }
                break;
            case USER:
                User user = ctx.getUserService().findUserByEmail(ctx.getTenantId(), entityName);
                if (user != null) {
                    targetEntity = user.getId();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
        if (targetEntity == null) {
            throw new IllegalStateException("Failed to found " + entityType.name() + "  entity by name: '" + entityName + "'!");
        }
        return targetEntity;
    }

}
