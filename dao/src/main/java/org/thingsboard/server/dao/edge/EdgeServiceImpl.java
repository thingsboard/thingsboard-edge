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
package org.thingsboard.server.dao.edge;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.extractConstraintViolationException;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class EdgeServiceImpl extends AbstractCachedEntityService<EdgeCacheKey, Edge, EdgeCacheEvictEvent> implements EdgeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataValidator<Edge> edgeValidator;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    @TransactionalEventListener(classes = EdgeCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EdgeCacheEvictEvent event) {
        List<EdgeCacheKey> keys = new ArrayList<>(2);
        keys.add(new EdgeCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new EdgeCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public Edge findEdgeById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findById(tenantId, edgeId.getId());
    }

    @Override
    public ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findByIdAsync(tenantId, edgeId.getId());
    }

    @Override
    public Edge findEdgeByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new EdgeCacheKey(tenantId, name),
                () -> edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    @Override
    public Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public Edge saveEdge(Edge edge) {
        log.trace("Executing saveEdge [{}]", edge);
        Edge oldEdge = edgeValidator.validate(edge, Edge::getTenantId);
        EdgeCacheEvictEvent evictEvent = new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), oldEdge != null ? oldEdge.getName() : null);
        try {
            var savedEdge = edgeDao.save(edge.getTenantId(), edge);
            publishEvictEvent(evictEvent);
            entityGroupService.addEntityToEntityGroupAll(savedEdge.getTenantId(), savedEdge.getOwnerId(), savedEdge.getId());
            return savedEdge;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && e.getConstraintName().equalsIgnoreCase("edge_name_unq_key")) {
                throw new DataValidationException("Edge with such name already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    @Transactional
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());

        deleteEntityRelations(tenantId, edgeId);

        edgeDao.removeById(tenantId, edgeId.getId());

        publishEvictEvent(new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), null));
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdAndIdsAsync, tenantId [{}], edgeIds [{}]", tenantId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void deleteEdgesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEdgesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], edgeIds [{}]", tenantId, customerId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void deleteEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEdgesRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query) {
        log.trace("[{}] Executing findEdgesByQuery [{}]", tenantId, query);
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Edge>> edges = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Edge>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.EDGE) {
                    futures.add(findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        edges = Futures.transform(edges, new Function<List<Edge>, List<Edge>>() {
            @Nullable
            @Override
            public List<Edge> apply(@Nullable List<Edge> edgeList) {
                return edgeList == null ?
                        Collections.emptyList() :
                        edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return edges;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEdgeTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEdgeTypes = edgeDao.findTenantEdgeTypesAsync(tenantId.getId());
        return Futures.transform(tenantEdgeTypes,
                edgeTypes -> {
                    edgeTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return edgeTypes;
                }, MoreExecutors.directExecutor());
    }

    @Override
    public void renameDeviceEdgeAllGroup(TenantId tenantId, Edge edge, String oldEdgeName) {
        log.trace("Executing renameDeviceEdgeAllGroup tenantId [{}], edge [{}], previousEdgeName [{}]", tenantId, edge, oldEdgeName);
        ListenableFuture<EntityGroup> deviceEdgeAllGroupFuture = entityGroupService.findOrCreateEdgeAllGroup(tenantId, edge, oldEdgeName, EntityType.DEVICE);
        Futures.addCallback(deviceEdgeAllGroupFuture, new FutureCallback<EntityGroup>() {
            @Override
            public void onSuccess(@Nullable EntityGroup deviceEdgeAllGroup) {
                if (deviceEdgeAllGroup != null) {
                    String newEntityGroupName = String.format(EntityGroup.GROUP_EDGE_ALL_NAME_PATTERN, edge.getName());
                    deviceEdgeAllGroup.setName(newEntityGroupName);
                    entityGroupService.saveEntityGroup(tenantId, deviceEdgeAllGroup.getOwnerId(), deviceEdgeAllGroup);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to find edge all group [{}]", tenantId, edge, t);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignDefaultRuleChainsToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, pageLink);
            if (pageData.getData().size() > 0) {
                for (RuleChain ruleChain : pageData.getData()) {
                    ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), edgeId);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public void assignTenantAdministratorsAndUsersGroupToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignTenantAdministratorsAndUsersGroupToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(tenantId);
        entityGroupService.assignEntityGroupToEdge(tenantId, admins.getId(), edgeId, admins.getType());
        EntityGroup users = entityGroupService.findOrCreateTenantUsersGroup(tenantId);
        entityGroupService.assignEntityGroupToEdge(tenantId, users.getId(), edgeId, users.getType());
    }

    @Override
    public void assignCustomerAdministratorsAndUsersGroupToEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, CustomerId parentCustomerId) {
        log.trace("Executing assignCustomerAdministratorsAndUsersGroupToEdge, tenantId [{}], edgeId [{}], customerId [{}]", tenantId, edgeId, customerId);
        EntityGroup customerAdmins = entityGroupService.findOrCreateCustomerAdminsGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerAdmins.getId(), edgeId, customerAdmins.getType());
        EntityGroup customerUsers = entityGroupService.findOrCreateCustomerUsersGroup(tenantId, customerId, parentCustomerId);
        entityGroupService.assignEntityGroupToEdge(tenantId, customerUsers.getId(), edgeId, customerUsers.getType());
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityIds(TenantId tenantId, List<EntityId> entityIds, EntityType entityType, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndEntityIds, tenantId [{}], entityIds [{}], entityType [{}], pageLink [{}]", tenantId, entityIds, entityType, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgeIdsByTenantIdAndEntityIds(tenantId.getId(),
                entityIds.stream().map(EntityId::getId).collect(Collectors.toList()),
                entityType,
                pageLink);
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(TenantId tenantId, List<EntityGroupId> entityGroupIds, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndEntityGroupIds, tenantId [{}], entityGroupIds [{}]", tenantId, entityGroupIds);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateIds(entityGroupIds, "Incorrect entityGroupIds " + entityGroupIds);
        validatePageLink(pageLink);
        List<UUID> entityGroupUuids = entityGroupIds.stream().map(UUIDBased::getId).collect(Collectors.toList());
        return edgeDao.findEdgeIdsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupUuids, groupType, pageLink);
    }

    private PaginatedRemover<TenantId, Edge> tenantEdgesRemover =
            new PaginatedRemover<TenantId, Edge>() {

                @Override
                protected PageData<Edge> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return edgeDao.findEdgesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Edge entity) {
                    deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Edge> customerEdgesRemover = new PaginatedRemover<CustomerId, Edge>() {

        @Override
        protected PageData<Edge> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Edge entity) {
            deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
        }
    };

    @Override
    public List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    @Override
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        return findRelatedEdgeIdsByEntityId(tenantId, entityId, null, pageLink);
    }

    @Override
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}] [{}]", tenantId, entityId, pageLink);
        switch (entityId.getEntityType()) {
            case TENANT:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case OTA_PACKAGE:
                return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
            case CUSTOMER:
                return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink));
            case EDGE:
                List<EdgeId> edgeIds = Collections.singletonList(new EdgeId(entityId.getId()));
                return new PageData<>(edgeIds, 1, 1, false);
            case USER:
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
                List<EntityGroupId> entityGroupsForEntity = null;
                try {
                    entityGroupsForEntity = entityGroupService.findEntityGroupsForEntity(tenantId, entityId).get();
                } catch (Exception e) {
                    log.error("[{}] Can't find entity group for entity {} {}", tenantId, entityId, e);
                }
                if (CollectionUtils.isEmpty(entityGroupsForEntity)) {
                    return createEmptyEdgeIdPageData();
                }
                return findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, entityGroupsForEntity, entityId.getEntityType(), pageLink);
            case ENTITY_GROUP:
                EntityGroupId entityGroupId = new EntityGroupId(entityId.getId());
                if (groupType == null) {
                    groupType = entityGroupService.findEntityGroupById(tenantId, entityGroupId).getType();
                }
                return findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, Collections.singletonList(entityGroupId), groupType, pageLink);
            case RULE_CHAIN:
            case SCHEDULER_EVENT:
            case INTEGRATION:
                return convertToEdgeIds(findEdgesByTenantIdAndEntityId(tenantId, entityId, pageLink));
            case CONVERTER:
                List<Integration> integrationsByConverterId =
                        integrationService.findIntegrationsByConverterId(tenantId, new ConverterId(entityId.getId()));
                List<EntityId> integrationIds = integrationsByConverterId.stream().map(Integration::getId).collect(Collectors.toList());
                return findEdgeIdsByTenantIdAndEntityIds(tenantId, integrationIds, EntityType.INTEGRATION, pageLink);
            default:
                log.warn("[{}] Unsupported entity type {}", tenantId, entityId.getEntityType());
                return createEmptyEdgeIdPageData();
        }
    }

    @Override
    public PageData<Edge> findEdgesByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findEdgesByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, "Incorrect entityGroupId " + groupId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findEdgesByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByEntityGroupIdsAndType, groupIds [{}], type [{}], pageLink [{}]", groupIds, type, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByEntityGroupIdsAndType(toUUIDs(groupIds), type, pageLink);
    }

    private PageData<EdgeId> createEmptyEdgeIdPageData() {
        return new PageData<>(new ArrayList<>(), 0, 0, false);
    }

    private PageData<EdgeId> convertToEdgeIds(PageData<Edge> pageData) {
        if (pageData == null) {
            return createEmptyEdgeIdPageData();
        }
        List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName) {
        List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).collect(Collectors.toList());
        ObjectNode result = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleNode> ruleNodes =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getNodes();
            if (ruleNodes != null && !ruleNodes.isEmpty()) {
                List<RuleChainId> connectedRuleChains =
                        ruleNodes.stream()
                                .filter(rn -> rn.getType().equals(tbRuleChainInputNodeClassName))
                                .map(rn -> new RuleChainId(UUID.fromString(rn.getConfiguration().get("ruleChainId").asText())))
                                .collect(Collectors.toList());
                List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = JacksonUtil.OBJECT_MAPPER.createArrayNode();
                    for (String missingRuleChain : missingRuleChains) {
                        array.add(missingRuleChain);
                    }
                    result.set(edgeRuleChain.getName(), array);
                }
            }
        }
        return result.toString();
    }

    private List<RuleChain> findEdgeRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return result;
    }

    @Override
    public String findEdgeMissingAttributes(TenantId tenantId, EdgeId edgeId, List<IntegrationId> integrationIds) throws Exception {
        ObjectNode result = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        for (IntegrationId integrationId : integrationIds) {
            Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
            Set<String> attributesKeys = EdgeUtils.getAttributeKeysFromConfiguration(integration.getConfiguration().toString());
            if (attributesKeys.isEmpty()) {
                return result.toString();
            }
            ArrayNode array = addMissingEdgeAttributes(tenantId, edgeId, attributesKeys);
            if (array != null) {
                result.set(integration.getName(), array);
            }
        }
        return result.toString();
    }

    @Override
    public String findAllRelatedEdgesMissingAttributes(TenantId tenantId, IntegrationId integrationId) throws Exception {
        Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
        Set<String> attributesKeys = EdgeUtils.getAttributeKeysFromConfiguration(integration.getConfiguration().toString());
        ObjectNode result = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EdgeId> pageData;
        do {
            pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, integrationId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (EdgeId relatedEdgeId : pageData.getData()) {
                    ArrayNode array = addMissingEdgeAttributes(tenantId, relatedEdgeId, attributesKeys);
                    if (array != null) {
                        Edge edgeById = findEdgeById(tenantId, relatedEdgeId);
                        result.set(edgeById.getName(), array);
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return result.toString();
    }

    private ArrayNode addMissingEdgeAttributes(TenantId tenantId, EdgeId edgeId, Set<String> attributesKeys) throws ExecutionException, InterruptedException {
        List<AttributeKvEntry> edgeAttributes =
                attributesService.find(tenantId, edgeId, DataConstants.SERVER_SCOPE, attributesKeys).get();
        List<String> edgeAttributeKeys =
                edgeAttributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
        List<String> missingAttributeKeys = attributesKeys.stream()
                .filter(element -> !edgeAttributeKeys.contains(element))
                .collect(Collectors.toList());
        if (missingAttributeKeys.isEmpty()) {
            return null;
        }

        ArrayNode array = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        for (String missingAttributeKey : missingAttributeKeys) {
            array.add(missingAttributeKey);
        }
        return array;
    }

}
