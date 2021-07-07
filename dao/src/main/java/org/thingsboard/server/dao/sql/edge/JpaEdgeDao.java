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
package org.thingsboard.server.dao.sql.edge;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JpaEdgeDao extends JpaAbstractSearchTextDao<EdgeEntity, Edge> implements EdgeDao {

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<EdgeEntity> getEntityClass() {
        return EdgeEntity.class;
    }

    @Override
    protected CrudRepository<EdgeEntity, UUID> getCrudRepository() {
        return edgeRepository;
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(edgeRepository.findEdgesByTenantIdAndIdIn(tenantId, edgeIds)));
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> edgeIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                edgeRepository.findEdgesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, edgeIds)));
    }

    @Override
    public Optional<Edge> findEdgeByTenantIdAndName(UUID tenantId, String name) {
        Edge edge = DaoUtil.getData(edgeRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(edge);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                edgeRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEdgeTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEdgeTypesToDto(tenantId, edgeRepository.findTenantEdgeTypes(tenantId)));
    }

    @Override
    public Optional<Edge> findByRoutingKey(UUID tenantId, String routingKey) {
        Edge edge = DaoUtil.getData(edgeRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(edge);
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

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndDashboardId(UUID tenantId, UUID dashboardId) {
        log.debug("Try to find edges by tenantId [{}], dashboardId [{}]", tenantId, dashboardId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(new TenantId(tenantId), new DashboardId(dashboardId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        return transformFromRelationToEdge(tenantId, relations);
    }

    @Override
    public void cleanupEvents(long ttl) {
        log.info("Going to cleanup old edge events using ttl: {}s", ttl);
        try (Connection connection = dataSource.getConnection();
        PreparedStatement stmt = connection.prepareStatement("call cleanup_edge_events_by_ttl(?,?)")) {
            stmt.setLong(1, ttl);
            stmt.setLong(2, 0);
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total edge events removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during edge events TTL task execution ", e);
        }
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

    private List<EntitySubtype> convertTenantEdgeTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.EDGE, type));
            }
        }
        return list;
    }

}
