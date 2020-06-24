/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.model.nosql.EdgeEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraEdgeDao extends CassandraAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<EdgeEntity> getColumnFamilyClass() {
        return EdgeEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EDGE_COLUMN_FAMILY_NAME;
    }


    @Override
    public List<Edge> findEdgesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return null;
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return null;
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        return null;
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return null;
    }

    @Override
    public List<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return null;
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        return null;
    }

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        return Optional.empty();
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        return null;
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        return Optional.empty();
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(UUID tenantId, UUID ruleChainId) {
        log.debug("Try to find edges by tenantId [{}], ruleChainId [{}]", tenantId, ruleChainId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new RuleChainId(ruleChainId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndSchedulerEventId(UUID tenantId, UUID schedulerEventId) {
        log.debug("Try to find edges by tenantId [{}], schedulerEventId [{}]", tenantId, schedulerEventId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new SchedulerEventId(schedulerEventId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, EntityType groupType) {
        log.debug("Try to find edges by tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new EntityGroupId(entityGroupId), relationType, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    private ListenableFuture<List<Edge>> transformFromRelationToEdge(UUID tenantId, ListenableFuture<List<EntityRelation>> relations) {
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<Edge>> edgeFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                edgeFutures.add(findByIdAsync(new TenantId(tenantId), relation.getFrom().getId()));
            }
            return Futures.successfulAsList(edgeFutures);
        }, MoreExecutors.directExecutor());
    }
}
