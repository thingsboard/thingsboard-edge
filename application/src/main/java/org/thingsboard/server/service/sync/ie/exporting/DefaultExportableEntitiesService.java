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
package org.thingsboard.server.service.sync.ie.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();
    private final Map<EntityType, BiConsumer<TenantId, EntityId>> removers = new HashMap<>();

    private final OwnersCacheService ownersCacheService;
    private final AccessControlService accessControlService;


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            entity = exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        E entity = findEntityById(id);

        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }
        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityById(I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }

        return dao.findById(TenantId.SYS_TENANT_ID, id.getId());
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            try {
                entity = exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> PageData<E> findEntitiesByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        ExportableEntityDao<I, E> dao = getExportableEntityDao(entityType);
        if (dao != null) {
            return dao.findByTenantId(tenantId.getId(), pageLink);
        } else {
            return new PageData<>();
        }
    }

    @Override
    public <I extends EntityId> I getExternalIdByInternal(I internalId) {
        ExportableEntityDao<I, ?> dao = getExportableEntityDao(internalId.getEntityType());
        if (dao != null) {
            return dao.getExternalIdByInternal(internalId);
        } else {
            return null;
        }
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> PageData<I> findEntityIdsByTenantIdAndCustomerId(TenantId tenantId, EntityId ownerId, EntityType entityType, PageLink pageLink) {
        Dao<E> dao = getDao(entityType);
        if (dao instanceof ExportableCustomerEntityDao) {
            ExportableCustomerEntityDao<E, I> exportableEntityDao = (ExportableCustomerEntityDao<E, I>) dao;
            return exportableEntityDao.findIdsByTenantIdAndCustomerId(tenantId.getId(), ownerId != null ? ownerId.getId() : null, pageLink);
        }
        return new PageData<>();
    }

    private boolean belongsToTenant(HasId<? extends EntityId> entity, TenantId tenantId) {
        Set<EntityId> owners;
        if (entity instanceof HasOwnerId) {
            owners = ownersCacheService.getOwners(tenantId, entity.getId(), (HasOwnerId) entity);
        } else {
            owners = Set.of(((HasTenantId) entity).getTenantId());
        }
        return owners.contains(tenantId);
    }


    @Override
    public <I extends EntityId> void removeById(TenantId tenantId, I id) {
        EntityType entityType = id.getEntityType();
        BiConsumer<TenantId, EntityId> entityRemover = removers.get(entityType);
        if (entityRemover == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }
        entityRemover.accept(tenantId, id);
    }

    private <I extends EntityId, E extends ExportableEntity<I>> ExportableEntityDao<I, E> getExportableEntityDao(EntityType entityType) {
        Dao<E> dao = getDao(entityType);
        if (dao instanceof ExportableEntityDao) {
            return (ExportableEntityDao<I, E>) dao;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    @Autowired
    private void setDaos(Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

    @Autowired
    private void setRemovers(CustomerService customerService, AssetService assetService, RuleChainService ruleChainService,
                             DashboardService dashboardService, DeviceProfileService deviceProfileService,
                             AssetProfileService assetProfileService, DeviceService deviceService, WidgetsBundleService widgetsBundleService,
                             EntityGroupService entityGroupService, ConverterService converterService,
                             IntegrationService integrationService, RoleService roleService) {
        removers.put(EntityType.CUSTOMER, (tenantId, entityId) -> {
            customerService.deleteCustomer(tenantId, (CustomerId) entityId);
        });
        removers.put(EntityType.ASSET, (tenantId, entityId) -> {
            assetService.deleteAsset(tenantId, (AssetId) entityId);
        });
        removers.put(EntityType.RULE_CHAIN, (tenantId, entityId) -> {
            ruleChainService.deleteRuleChainById(tenantId, (RuleChainId) entityId);
        });
        removers.put(EntityType.DASHBOARD, (tenantId, entityId) -> {
            dashboardService.deleteDashboard(tenantId, (DashboardId) entityId);
        });
        removers.put(EntityType.DEVICE_PROFILE, (tenantId, entityId) -> {
            deviceProfileService.deleteDeviceProfile(tenantId, (DeviceProfileId) entityId);
        });
        removers.put(EntityType.ASSET_PROFILE, (tenantId, entityId) -> {
            assetProfileService.deleteAssetProfile(tenantId, (AssetProfileId) entityId);
        });
        removers.put(EntityType.DEVICE, (tenantId, entityId) -> {
            deviceService.deleteDevice(tenantId, (DeviceId) entityId);
        });
        removers.put(EntityType.WIDGETS_BUNDLE, (tenantId, entityId) -> {
            widgetsBundleService.deleteWidgetsBundle(tenantId, (WidgetsBundleId) entityId);
        });
        removers.put(EntityType.ENTITY_GROUP, (tenantId, entityId) -> {
            entityGroupService.deleteEntityGroup(tenantId, (EntityGroupId) entityId);
        });
        removers.put(EntityType.CONVERTER, (tenantId, entityId) -> {
            converterService.deleteConverter(tenantId, (ConverterId) entityId);
        });
        removers.put(EntityType.INTEGRATION, (tenantId, entityId) -> {
            integrationService.deleteIntegration(tenantId, (IntegrationId) entityId);
        });
        removers.put(EntityType.ROLE, (tenantId, entityId) -> {
            roleService.deleteRole(tenantId, (RoleId) entityId);
        });
    }

}
