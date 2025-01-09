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
package org.thingsboard.server.dao.edge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheKey;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheValue;
import org.thingsboard.server.cache.edge.RelatedEdgesEvictEvent;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;

import java.util.List;

@Service
@Slf4j
public class BaseRelatedEdgesService extends AbstractCachedEntityService<RelatedEdgesCacheKey, RelatedEdgesCacheValue, RelatedEdgesEvictEvent> implements RelatedEdgesService {

    public static final int RELATED_EDGES_CACHE_ITEMS = 1000;
    public static final PageLink FIRST_PAGE = new PageLink(RELATED_EDGES_CACHE_ITEMS);

    @Autowired
    @Lazy
    private EdgeService edgeService;

    @TransactionalEventListener(classes = RelatedEdgesEvictEvent.class)
    @Override
    public void handleEvictEvent(RelatedEdgesEvictEvent event) {
        if (event.getEntityId() == null) {
            cache.evictByPrefix("{" + event.getTenantId() + "}");
        } else {
            cache.evict(new RelatedEdgesCacheKey(event.getTenantId(), event.getEntityId()));
        }
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndEntityId(tenantId, entityId, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndEntityId(tenantId, entityId, pageLink)), false).getPageData();
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndGroupEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndGroupEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndGroupEntityId(tenantId, entityId, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndGroupEntityId(tenantId, entityId, pageLink)), false).getPageData();
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(TenantId tenantId, EntityGroupId entityGroupId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndEntityGroupIds, tenantId [{}], entityGroupId [{}], groupType [{}], pageLink [{}]", tenantId, entityGroupId, groupType, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, List.of(entityGroupId), groupType, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityGroupId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, List.of(entityGroupId), groupType, pageLink)), false).getPageData();
    }

    @Override
    public void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId) {
        log.trace("Executing publishRelatedEdgeIdsEvictEvent, tenantId [{}], entityId [{}]", tenantId, entityId);
        publishEvictEvent(new RelatedEdgesEvictEvent(tenantId, entityId));
    }

    @Override
    public void publishEdgeIdsEvictEventByTenantId(TenantId tenantId) {
        log.trace("Executing publishEdgeIdsEvictEventByTenantId, tenantId [{}]", tenantId);
        publishEvictEvent(new RelatedEdgesEvictEvent(tenantId, null));
    }

}
