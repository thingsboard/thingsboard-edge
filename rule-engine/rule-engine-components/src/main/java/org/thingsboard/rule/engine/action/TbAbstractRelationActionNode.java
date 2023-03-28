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
package org.thingsboard.rule.engine.action;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
public abstract class TbAbstractRelationActionNode<C extends TbAbstractRelationActionNodeConfiguration> implements TbNode {

    protected C config;

    private LoadingCache<EntityKey, EntityContainer> entityIdCache;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadEntityNodeActionConfig(configuration);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.config.getEntityCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getEntityCacheExpiration(), TimeUnit.SECONDS);
        }
        entityIdCache = cacheBuilder.build(new EntityCacheLoader(ctx, createEntityIfNotExists()));
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String relationType = processPattern(msg, config.getRelationType());
        withCallback(processEntityRelationAction(ctx, msg, relationType),
                filterResult -> ctx.tellNext(filterResult.getMsg(), filterResult.isResult() ? SUCCESS : FAILURE), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
        if (entityIdCache != null) {
            entityIdCache.invalidateAll();
        }
    }

    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg, String relationType) {
        return Futures.transformAsync(getEntity(ctx, msg), entityContainer -> doProcessEntityRelationAction(ctx, msg, entityContainer, relationType), ctx.getDbCallbackExecutor());
    }

    protected abstract boolean createEntityIfNotExists();

    protected abstract ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType);

    protected abstract C loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    protected ListenableFuture<EntityContainer> getEntity(TbContext ctx, TbMsg msg) {
        String entityName = processPattern(msg, this.config.getEntityNamePattern());
        String type;
        if (this.config.getEntityTypePattern() != null) {
            type = processPattern(msg, this.config.getEntityTypePattern());
        } else {
            type = null;
        }
        EntityType entityType = EntityType.valueOf(this.config.getEntityType());
        EntityId ownerId = ctx.getPeContext().getOwner(ctx.getTenantId(), msg.getOriginator());
        EntityKey key = new EntityKey(entityName, type, entityType, ownerId);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityContainer entityContainer = entityIdCache.get(key);
            if (entityContainer.getEntityId() == null) {
                throw new RuntimeException("No entity found with type '" + key.getEntityType() + "' and name '" + key.getEntityName() + "'."
                        + DataConstants.ENTITY_CREATION_ON_EDGE_NOT_SUPPORTED_WARNING);
            }
            return entityContainer;
        });
    }

    protected SearchDirectionIds processSingleSearchDirection(TbMsg msg, EntityContainer entityContainer) {
        SearchDirectionIds searchDirectionIds = new SearchDirectionIds();
        if (EntitySearchDirection.FROM.name().equals(this.config.getDirection())) {
            searchDirectionIds.setFromId(EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString()));
            searchDirectionIds.setToId(msg.getOriginator());
            searchDirectionIds.setOriginatorDirectionFrom(false);
        } else {
            searchDirectionIds.setToId(EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString()));
            searchDirectionIds.setFromId(msg.getOriginator());
            searchDirectionIds.setOriginatorDirectionFrom(true);
        }
        return searchDirectionIds;
    }

    protected ListenableFuture<List<EntityRelation>> processListSearchDirection(TbContext ctx, TbMsg msg) {
        if (EntitySearchDirection.FROM.name().equals(this.config.getDirection())) {
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        } else {
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        }
    }

    protected String processPattern(TbMsg msg, String pattern) {
        return TbNodeUtils.processPattern(pattern, msg);
    }

    @Data
    @AllArgsConstructor
    private static class EntityKey {
        private String entityName;
        private String type;
        private EntityType entityType;
        private EntityId ownerId;
    }

    @Data
    protected static class SearchDirectionIds {
        private EntityId fromId;
        private EntityId toId;
        private boolean originatorDirectionFrom;
    }

    private static class EntityCacheLoader extends CacheLoader<EntityKey, EntityContainer> {

        private final TbContext ctx;
        private final boolean createIfNotExists;

        private EntityCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        @Override
        public EntityContainer load(EntityKey key) {
            return loadEntity(key);
        }

        private EntityContainer loadEntity(EntityKey entitykey) {
            EntityType type = entitykey.getEntityType();
            EntityContainer targetEntity = new EntityContainer();
            targetEntity.setEntityType(type);
            switch (type) {
                case DEVICE:
                    DeviceService deviceService = ctx.getDeviceService();
                    Device device = deviceService.findDeviceByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (device != null) {
                        targetEntity.setEntityId(device.getId());
                    } else if (createIfNotExists) {
                        Device newDevice = new Device();
                        newDevice.setName(entitykey.getEntityName());
                        newDevice.setType(entitykey.getType());
                        newDevice.setTenantId(ctx.getTenantId());
                        newDevice.setOwnerId(entitykey.getOwnerId());
                        Device savedDevice = deviceService.saveDevice(newDevice);
                        ctx.getClusterService().onDeviceUpdated(savedDevice, null);
                        ctx.enqueue(ctx.deviceCreatedMsg(savedDevice, ctx.getSelfId()),
                                () -> log.trace("Pushed Device Created message: {}", savedDevice),
                                throwable -> log.warn("Failed to push Device Created message: {}", savedDevice, throwable));
                        targetEntity.setEntityId(savedDevice.getId());
                    }
                    break;
                case ASSET:
                    AssetService assetService = ctx.getAssetService();
                    Asset asset = assetService.findAssetByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (asset != null) {
                        targetEntity.setEntityId(asset.getId());
                    }
                    // TODO: @voba assets are not created on the edge at the moment
                    // else if (createIfNotExists) {
                    //     Asset newAsset = new Asset();
                    //     newAsset.setName(entitykey.getEntityName());
                    //     newAsset.setType(entitykey.getType());
                    //     newAsset.setTenantId(ctx.getTenantId());
                    //     newAsset.setOwnerId(entitykey.getOwnerId());
                    //     Asset savedAsset = assetService.saveAsset(newAsset);
                    //     ctx.enqueue(ctx.assetCreatedMsg(savedAsset, ctx.getSelfId()),
                    //             () -> log.trace("Pushed Asset Created message: {}", savedAsset),
                    //             throwable -> log.warn("Failed to push Asset Created message: {}", savedAsset, throwable));
                    //     targetEntity.setEntityId(savedAsset.getId());
                    // }
                    break;
                case CUSTOMER:
                    CustomerService customerService = ctx.getCustomerService();
                    Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), entitykey.getEntityName());
                    if (customerOptional.isPresent()) {
                        targetEntity.setEntityId(customerOptional.get().getId());
                    }
                    // TODO: @voba customers are not created on the edge at the moment
                    // else if (createIfNotExists) {
                    //     Customer newCustomer = new Customer();
                    //     newCustomer.setTitle(entitykey.getEntityName());
                    //     newCustomer.setTenantId(ctx.getTenantId());
                    //     newCustomer.setOwnerId(entitykey.getOwnerId());
                    //     Customer savedCustomer = customerService.saveCustomer(newCustomer);
                    //     ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                    //             () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                    //             throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                    //     targetEntity.setEntityId(savedCustomer.getId());
                    // }
                    break;
                case TENANT:
                    targetEntity.setEntityId(ctx.getTenantId());
                    break;
                case ENTITY_VIEW:
                    EntityViewService entityViewService = ctx.getEntityViewService();
                    EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (entityView != null) {
                        targetEntity.setEntityId(entityView.getId());
                    }
                    break;
                case EDGE:
                    EdgeService edgeService = ctx.getEdgeService();
                    Edge edge = edgeService.findEdgeByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (edge != null) {
                        targetEntity.setEntityId(edge.getId());
                    }
                    break;
                case DASHBOARD:
                    DashboardService dashboardService = ctx.getDashboardService();
                    DashboardInfo dashboardInfo = dashboardService.findFirstDashboardInfoByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (dashboardInfo != null) {
                        targetEntity.setEntityId(dashboardInfo.getId());
                    }
                    break;
                case USER:
                    UserService userService = ctx.getUserService();
                    User user = userService.findUserByTenantIdAndEmail(ctx.getTenantId(), entitykey.getEntityName());
                    if (user != null) {
                        targetEntity.setEntityId(user.getId());
                    }
                    break;
                default:
                    return targetEntity;
            }
            return targetEntity;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class RelationContainer {

        private TbMsg msg;
        private boolean result;

    }


}
