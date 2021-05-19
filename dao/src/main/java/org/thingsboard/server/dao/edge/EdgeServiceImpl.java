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
package org.thingsboard.server.dao.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.thingsboard.server.common.data.CacheConstants.EDGE_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class EdgeServiceImpl extends AbstractEntityService implements EdgeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final int DEFAULT_LIMIT = 100;

    private RestTemplate restTemplate;

    private static final String EDGE_LICENSE_SERVER_ENDPOINT = "https://license.thingsboard.io";

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;

    @PostConstruct
    public void init() {
        if (edgesEnabled) {
            initRestTemplate();
        }
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

    @Cacheable(cacheNames = EDGE_CACHE, key = "{#tenantId, #name}")
    @Override
    public Edge findEdgeByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<Edge> edgeOpt = edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name);
        return edgeOpt.orElse(null);
    }

    @Override
    public Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @CacheEvict(cacheNames = EDGE_CACHE, key = "{#edge.tenantId, #edge.name}")
    @Override
    public Edge saveEdge(Edge edge) {
        log.trace("Executing saveEdge [{}]", edge);
        edgeValidator.validate(edge, Edge::getTenantId);
        Edge savedEdge;
        try {
            savedEdge = edgeDao.save(edge.getTenantId(), edge);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && e.getConstraintName().equalsIgnoreCase("edge_name_unq_key")) {
                throw new DataValidationException("Edge with such name already exists!");
            } else {
                throw t;
            }
        }
        if (edge.getId() == null) {
            entityGroupService.addEntityToEntityGroupAll(savedEdge.getTenantId(), savedEdge.getOwnerId(), savedEdge.getId());
        }
        return savedEdge;
    }

    @Override
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());

        deleteEntityRelations(tenantId, edgeId);

        removeEdgeFromCacheByName(edge.getTenantId(), edge.getName());

        edgeDao.removeById(tenantId, edgeId.getId());
    }

    private void removeEdgeFromCacheByName(TenantId tenantId, String name) {
        Cache cache = cacheManager.getCache(EDGE_CACHE);
        cache.evict(Arrays.asList(tenantId, name));
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
                return edgeList == null ? Collections.emptyList() : edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList());
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

    class EdgeViewFunction implements BiFunction<Edge, List<EntityField>, ShortEntityView> {

        @Override
        public ShortEntityView apply(Edge edge, List<EntityField> entityFields) {
            ShortEntityView shortEntityView = new ShortEntityView(edge.getId());
            shortEntityView.put(EntityField.NAME.name().toLowerCase(), edge.getName());
            for (EntityField field : entityFields) {
                String key = field.name().toLowerCase();
                switch (field) {
                    case TYPE:
                        shortEntityView.put(key, edge.getType());
                        break;
                }
            }
            return shortEntityView;
        }
    }

    @Override
    public void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignDefaultRuleChainsToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
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
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndRuleChainId(TenantId tenantId, RuleChainId ruleChainId) {
        log.trace("Executing findEdgesByTenantIdAndRuleChainId, tenantId [{}], ruleChainId [{}]", tenantId, ruleChainId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(ruleChainId, "Incorrect ruleChainId " + ruleChainId);
        return edgeDao.findEdgesByTenantIdAndRuleChainId(tenantId.getId(), ruleChainId.getId());
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndSchedulerEventId(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findEdgesByTenantIdAndSchedulerEventId, tenantId [{}], schedulerEventId [{}]", tenantId, schedulerEventId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(schedulerEventId, "Incorrect schedulerEventId " + schedulerEventId);
        return edgeDao.findEdgesByTenantIdAndSchedulerEventId(tenantId.getId(), schedulerEventId.getId());
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, EntityType groupType) {
        log.trace("Executing findEdgesByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(entityGroupId, "Incorrect schedulerEventId " + entityGroupId);
        return edgeDao.findEdgesByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), groupType);
    }

    private DataValidator<Edge> edgeValidator =
            new DataValidator<Edge>() {

                @Override
                protected void validateCreate(TenantId tenantId, Edge edge) {
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Edge edge) {
                    Edge old = edgeDao.findById(edge.getTenantId(), edge.getId().getId());
                    if (!old.getName().equals(edge.getName())) {
                        removeEdgeFromCacheByName(tenantId, old.getName());
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Edge edge) {
                    if (StringUtils.isEmpty(edge.getType())) {
                        throw new DataValidationException("Edge type should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getName())) {
                        throw new DataValidationException("Edge name should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getSecret())) {
                        throw new DataValidationException("Edge secret should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getRoutingKey())) {
                        throw new DataValidationException("Edge routing key should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getEdgeLicenseKey())) {
                        throw new DataValidationException("Edge license key should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getCloudEndpoint())) {
                        throw new DataValidationException("Cloud endpoint should be specified!");
                    }
                    if (edge.getTenantId() == null) {
                        throw new DataValidationException("Edge should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(edge.getTenantId(), edge.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Edge is referencing to non-existent tenant!");
                        }
                    }
                    if (edge.getCustomerId() == null) {
                        edge.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!edge.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(edge.getTenantId(), edge.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign edge to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(edge.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign edge to customer from different tenant!");
                        }
                    }
                }
            };

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

    @Override
    public ListenableFuture<List<EdgeId>> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId) {
        return findRelatedEdgeIdsByEntityId(tenantId, entityId, null);
    }

    @Override
    public ListenableFuture<List<EdgeId>> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, String groupTypeStr) {
        // TODO: voba - rewrite 'find' to use native SQL queries instead of fetching relations

        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}]", tenantId, entityId);
        if (EntityType.TENANT.equals(entityId.getEntityType()) ||
                EntityType.CUSTOMER.equals(entityId.getEntityType()) ||
                EntityType.DEVICE_PROFILE.equals(entityId.getEntityType())) {
            List<EdgeId> result = new ArrayList<>();
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<Edge> pageData;
            do {
                if (EntityType.TENANT.equals(entityId.getEntityType()) ||
                        EntityType.DEVICE_PROFILE.equals(entityId.getEntityType())) {
                    pageData = findEdgesByTenantId(tenantId, pageLink);
                } else {
                    pageData = findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink);
                }
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    for (Edge edge : pageData.getData()) {
                        result.add(edge.getId());
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
            return Futures.immediateFuture(result);
        } else {
            switch (entityId.getEntityType()) {
                case USER:
                case DEVICE:
                case ASSET:
                case ENTITY_VIEW:
                case DASHBOARD:
                    ListenableFuture<List<EntityGroupId>> entityGroupsForEntity = entityGroupService.findEntityGroupsForEntity(tenantId, entityId);
                    ListenableFuture<List<List<Edge>>> mergedFuture = Futures.transformAsync(entityGroupsForEntity, entityGroupIds -> {
                        List<ListenableFuture<List<Edge>>> futures = new ArrayList<>();
                        if (entityGroupIds != null && !entityGroupIds.isEmpty()) {
                            for (EntityGroupId entityGroupId : entityGroupIds) {
                                futures.add(findEdgesByTenantIdAndEntityGroupId(tenantId, entityGroupId, entityId.getEntityType()));
                            }
                        }
                        return Futures.successfulAsList(futures);
                    }, MoreExecutors.directExecutor());
                    return Futures.transform(mergedFuture, listOfEdges -> {
                        Set<EdgeId> result = new HashSet<>();
                        if (listOfEdges != null && !listOfEdges.isEmpty()) {
                            for (List<Edge> edges : listOfEdges) {
                                if (edges != null && !edges.isEmpty()) {
                                    for (Edge edge : edges) {
                                        result.add(edge.getId());
                                    }
                                }
                            }
                        }
                        return new ArrayList<>(result);
                    }, MoreExecutors.directExecutor());
                case RULE_CHAIN:
                    return convertToEdgeIds(findEdgesByTenantIdAndRuleChainId(tenantId, new RuleChainId(entityId.getId())));
                case SCHEDULER_EVENT:
                    return convertToEdgeIds(findEdgesByTenantIdAndSchedulerEventId(tenantId, new SchedulerEventId(entityId.getId())));
                case ENTITY_GROUP:
                    EntityGroupId entityGroupId = new EntityGroupId(entityId.getId());
                    EntityType groupType;
                    if (groupTypeStr != null) {
                        groupType = EntityType.valueOf(groupTypeStr);
                    } else {
                        groupType = entityGroupService.findEntityGroupById(tenantId, entityGroupId).getType();
                    }
                    return convertToEdgeIds(findEdgesByTenantIdAndEntityGroupId(tenantId, entityGroupId, groupType));
                default:
                    return Futures.immediateFuture(Collections.emptyList());
            }
        }
    }

    private ListenableFuture<List<EdgeId>> convertToEdgeIds(ListenableFuture<List<Edge>> future) {
        return Futures.transform(future, edges -> {
            if (edges != null && !edges.isEmpty()) {
                return edges.stream().map(IdBased::getId).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Object checkInstance(Object request) {
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/checkInstance", request, Object.class, new Object[0]);
    }

    @Override
    public Object activateInstance(String edgeLicenseSecret, String releaseDate) {
        Map<String, String> params = new HashMap<>();
        params.put("licenseSecret", edgeLicenseSecret);
        params.put("releaseDate", releaseDate);
        return this.restTemplate.postForEntity(EDGE_LICENSE_SERVER_ENDPOINT + "/api/license/activateInstance?licenseSecret={licenseSecret}&releaseDate={releaseDate}", (Object) null, Object.class, params);
    }

    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).collect(Collectors.toList());
        ObjectNode result = mapper.createObjectNode();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleChainConnectionInfo> connectionInfos =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getRuleChainConnections();
            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                List<RuleChainId> connectedRuleChains =
                        connectionInfos.stream().map(RuleChainConnectionInfo::getTargetRuleChainId).collect(Collectors.toList());
                List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = mapper.createArrayNode();
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
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
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

    private void initRestTemplate() {
        boolean jdkHttpClientEnabled = isNotEmpty(System.getProperty("tb.proxy.jdk")) && System.getProperty("tb.proxy.jdk").equalsIgnoreCase("true");
        boolean systemProxyEnabled = isNotEmpty(System.getProperty("tb.proxy.system")) && System.getProperty("tb.proxy.system").equalsIgnoreCase("true");
        boolean proxyEnabled = isNotEmpty(System.getProperty("tb.proxy.host")) && isNotEmpty(System.getProperty("tb.proxy.port"));
        if (jdkHttpClientEnabled) {
            log.warn("Going to use plain JDK Http Client!");
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                factory.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")))));
            }

            this.restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        } else {
            CloseableHttpClient httpClient;
            HttpComponentsClientHttpRequestFactory requestFactory;
            if (systemProxyEnabled) {
                log.warn("Going to use System Proxy Server!");
                httpClient = HttpClients.createSystem();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            } else if (proxyEnabled) {
                log.warn("Going to use Proxy Server: [{}:{}]", System.getProperty("tb.proxy.host"), System.getProperty("tb.proxy.port"));
                httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).setProxy(new HttpHost(System.getProperty("tb.proxy.host"), Integer.parseInt(System.getProperty("tb.proxy.port")), "https")).build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            } else {
                httpClient = HttpClients.custom().setSSLHostnameVerifier(new DefaultHostnameVerifier()).build();
                requestFactory = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);
                this.restTemplate = new RestTemplate(requestFactory);
            }
        }

    }

}
