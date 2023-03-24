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
package org.thingsboard.server.dao.entityview;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;


/**
 * Created by Victor Basanets on 8/28/2017.
 */
@Service("EntityViewDaoService")
@Slf4j
public class EntityViewServiceImpl extends AbstractCachedEntityService<EntityViewCacheKey, EntityViewCacheValue, EntityViewEvictEvent> implements EntityViewService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ENTITY_VIEW_ID = "Incorrect entityViewId ";

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private EntityViewInfoDao entityViewInfoDao;

    @Autowired
    private DataValidator<EntityView> entityViewValidator;

    @Autowired
    protected JpaExecutorService service;

    @TransactionalEventListener(classes = EntityViewEvictEvent.class)
    @Override
    public void handleEvictEvent(EntityViewEvictEvent event) {
        List<EntityViewCacheKey> keys = new ArrayList<>(5);
        keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getNewName()));
        keys.add(EntityViewCacheKey.byId(event.getId()));
        keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getNewEntityId()));
        if (event.getOldEntityId() != null && !event.getOldEntityId().equals(event.getNewEntityId())) {
            keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getOldEntityId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public EntityView saveEntityView(EntityView entityView) {
        log.trace("Executing save entity view [{}]", entityView);
        EntityView old = entityViewValidator.validate(entityView, EntityView::getTenantId);
        try {
            EntityView saved = entityViewDao.save(entityView.getTenantId(), entityView);
            publishEvictEvent(new EntityViewEvictEvent(saved.getTenantId(), saved.getId(), saved.getEntityId(), old != null ? old.getEntityId() : null, saved.getName(), old != null ? old.getName() : null));
            if (entityView.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(saved.getTenantId(), saved.getOwnerId(), saved.getId());
            }
            return saved;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "entity_view_external_id_unq_key", "Entity View with such external id already exists!");
            throw t;
        }
    }

    @Override
    public EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byId(entityViewId),
                () -> entityViewDao.findById(tenantId, entityViewId.getId())
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);
    }

    @Override
    public EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEntityViewByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byName(tenantId, name),
                () -> entityViewDao.findEntityViewByTenantIdAndName(tenantId.getId(), name).orElse(null)
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);

    }

    @Override
    public PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type) {
        log.trace("Executing findEntityViewByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(TenantId tenantId, List<EntityViewId> entityViewIds) {
        log.trace("Executing findEntityViewsByTenantIdAndIdsAsync, tenantId [{}], entityViewIds [{}]", tenantId, entityViewIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(entityViewIds, "Incorrect entityViewIds " + entityViewIds);
        return entityViewDao.findEntityViewsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(entityViewIds));
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,
                                                                       PageLink pageLink) {
        log.trace("Executing findEntityViewByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type) {
        log.trace("Executing findEntityViewsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}]," +
                " pageLink [{}], type [{}]", tenantId, customerId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId.getId(),
                customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<EntityView>> entityViews = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<EntityView>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ENTITY_VIEW) {
                    futures.add(findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        entityViews = Futures.transform(entityViews, new Function<List<EntityView>, List<EntityView>>() {
            @Nullable
            @Override
            public List<EntityView> apply(@Nullable List<EntityView> entityViewList) {
                return entityViewList == null ? Collections.emptyList() : entityViewList.stream().filter(entityView -> query.getEntityViewTypes().contains(entityView.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return entityViews;
    }

    @Override
    public ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findByIdAsync(tenantId, entityViewId.getId());
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityIdAsync, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        return service.submit(() -> cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityId, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        return cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true);
    }

    @Override
    @Transactional
    public void deleteEntityView(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing deleteEntityView [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        deleteEntityRelations(tenantId, entityViewId);
        EntityView entityView = entityViewDao.findById(tenantId, entityViewId.getId());
        entityViewDao.removeById(tenantId, entityViewId.getId());
        publishEvictEvent(new EntityViewEvictEvent(entityView.getTenantId(), entityView.getId(), entityView.getEntityId(), null, entityView.getName(), null));
    }

    @Override
    public void deleteEntityViewsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityViewsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEntityViewRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteEntityViewsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEntityViewsRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEntityViewTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEntityViewTypes = entityViewDao.findTenantEntityViewTypesAsync(tenantId.getId());
        return Futures.transform(tenantEntityViewTypes,
                entityViewTypes -> {
                    entityViewTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return entityViewTypes;
                }, MoreExecutors.directExecutor());
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findEntityViewsByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, "Incorrect entityGroupId " + groupId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findEntityViewsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewsByEntityGroupIdsAndType, groupIds [{}], type [{}], pageLink [{}]", groupIds, type, pageLink);
        validateIds(groupIds, "Incorrect groupIds " + groupIds);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByEntityGroupIdsAndType(toUUIDs(groupIds), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findTenantEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantEntityViewInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewInfoDao.findTenantEntityViewsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findTenantEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findTenantEntityViewInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewInfoDao.findTenantEntityViewsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers, tenantId [{}], customerId [{}], type [{}], pageLink [{}]",
                tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    private PaginatedRemover<TenantId, EntityView> tenantEntityViewRemover = new PaginatedRemover<TenantId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EntityView entity) {
            deleteEntityView(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };

    private PaginatedRemover<CustomerId, EntityView> customerEntityViewsRemover = new PaginatedRemover<CustomerId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EntityView entity) {
            deleteEntityView(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEntityViewById(tenantId, new EntityViewId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}
