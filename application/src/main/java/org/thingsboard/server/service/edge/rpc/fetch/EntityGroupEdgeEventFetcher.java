/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.edge.rpc.EdgeEventUtils;

import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Slf4j
public class EntityGroupEdgeEventFetcher extends BasePageableEdgeEventFetcher {

    private final EntityGroupService entityGroupService;

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        List<EdgeEvent> result = new ArrayList<>();
        result.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.DEVICE));
        result.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.ASSET));
        // TODO: entity view must be in sync with assets/devices
        result.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.ENTITY_VIEW));
        result.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.DASHBOARD));
        result.addAll(getEntityGroupsEdgeEvents(tenantId, edge.getId(), EntityType.USER));
        // @voba - returns PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    private List<EdgeEvent> getEntityGroupsEdgeEvents(TenantId tenantId, EdgeId edgeId, EntityType entityGroupType) {
        try {
            List<EntityGroup> list = entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, entityGroupType).get();
            List<EdgeEvent> result = new ArrayList<>();
            if (list != null && !list.isEmpty()) {
                for (EntityGroup entityGroup : list) {
                    if (!entityGroup.isEdgeGroupAll()) {
                        result.add(EdgeEventUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP,
                                EdgeEventActionType.ADDED, entityGroup.getId(), null, null));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Exception during loading edge entity groups(s) on sync!", e);
            throw new RuntimeException(e);
        }
    }

}




