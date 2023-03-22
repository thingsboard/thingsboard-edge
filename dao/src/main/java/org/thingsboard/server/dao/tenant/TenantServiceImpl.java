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
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("TenantDaoService")
@Slf4j
public class TenantServiceImpl extends AbstractCachedEntityService<TenantId, Tenant, TenantEvictEvent> implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Lazy
    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private BlobEntityService blobEntityService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    @Autowired
    private RpcService rpcService;

    @Autowired
    private DataValidator<Tenant> tenantValidator;

    @Lazy
    @Autowired
    private QueueService queueService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    @Autowired
    private NotificationRequestService notificationRequestService;

    @Autowired
    private NotificationRuleService notificationRuleService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private NotificationTargetService notificationTargetService;

    @Autowired
    protected TbTransactionalCache<TenantId, Boolean> existsTenantCache;

    @Autowired
    private EntityViewService entityViewService;

    @TransactionalEventListener(classes = TenantEvictEvent.class)
    @Override
    public void handleEvictEvent(TenantEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
        if (event.isInvalidateExists()) {
            existsTenantCache.evict(tenantId);
        }
    }

    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);

        return cache.getAndPutInTransaction(tenantId, () -> tenantDao.findById(tenantId, tenantId.getId()), true);
    }

    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId) {
        log.trace("Executing findTenantInfoById [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findTenantInfoById(tenantId, tenantId.getId());
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId) {
        log.trace("Executing findTenantByIdAsync [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    @Override
    public ListenableFuture<List<Tenant>> findTenantsByIdsAsync(TenantId callerId, List<TenantId> tenantIds) {
        log.trace("Executing findTenantsByIdsAsync, callerId [{}], tenantIds [{}]", callerId, tenantIds);
        validateIds(tenantIds, "Incorrect tenantIds " + tenantIds);
        return tenantDao.findTenantsByIdsAsync(callerId.getId(), toUUIDs(tenantIds));
    }

    @Override
    @Transactional
    public Tenant saveTenant(Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        if (tenant.getTenantProfileId() == null) {
            TenantProfile tenantProfile = this.tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);
            tenant.setTenantProfileId(tenantProfile.getId());
        }
        tenantValidator.validate(tenant, Tenant::getId);
        boolean create = tenant.getId() == null;
        Tenant savedTenant = tenantDao.save(tenant.getId(), tenant);
        publishEvictEvent(new TenantEvictEvent(savedTenant.getId(), create));
        if (tenant.getId() == null) {
            deviceProfileService.createDefaultDeviceProfile(savedTenant.getId());
            assetProfileService.createDefaultAssetProfile(savedTenant.getId());

            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.CUSTOMER);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ASSET);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DEVICE);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.ENTITY_VIEW);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.EDGE);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.DASHBOARD);
            entityGroupService.createEntityGroupAll(savedTenant.getId(), savedTenant.getId(), EntityType.USER);

            entityGroupService.findOrCreateTenantUsersGroup(savedTenant.getId());
            entityGroupService.findOrCreateTenantAdminsGroup(savedTenant.getId());
            apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
            try {
                notificationSettingsService.createDefaultNotificationConfigs(savedTenant.getId());
            } catch (Throwable e) {
                log.error("Failed to create default notification configs for tenant {}", savedTenant.getId(), e);
            }
        }
        return savedTenant;
    }

    /**
     * We intentionally leave this method without "Transactional" annotation due to complexity of the method.
     * Ideally we should delete related entites without "paginatedRemover" logic. But in such a case we can't clear cache and send events.
     * We will create separate task to make "deleteTenant" transactional.
     */
    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        whiteLabelingService.deleteDomainWhiteLabelingByEntityId(tenantId, tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        assetProfileService.deleteAssetProfilesByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        deviceProfileService.deleteDeviceProfilesByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        edgeService.deleteEdgesByTenantId(tenantId);
        userService.deleteTenantAdmins(tenantId);
        integrationService.deleteIntegrationsByTenantId(tenantId);
        converterService.deleteConvertersByTenantId(tenantId);
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        schedulerEventService.deleteSchedulerEventsByTenantId(tenantId);
        blobEntityService.deleteBlobEntitiesByTenantId(tenantId);
        deleteEntityGroups(tenantId, tenantId);
        deleteEntityRelations(tenantId, tenantId);
        groupPermissionService.deleteGroupPermissionsByTenantId(tenantId);
        roleService.deleteRolesByTenantId(tenantId);
        apiUsageStateService.deleteApiUsageStateByTenantId(tenantId);
        resourceService.deleteResourcesByTenantId(tenantId);
        otaPackageService.deleteOtaPackagesByTenantId(tenantId);
        rpcService.deleteAllRpcByTenantId(tenantId);
        queueService.deleteQueuesByTenantId(tenantId);
        notificationRequestService.deleteNotificationRequestsByTenantId(tenantId);
        notificationRuleService.deleteNotificationRulesByTenantId(tenantId);
        notificationTemplateService.deleteNotificationTemplatesByTenantId(tenantId);
        notificationTargetService.deleteNotificationTargetsByTenantId(tenantId);
        adminSettingsService.deleteAdminSettingsByTenantId(tenantId);
        tenantDao.removeById(tenantId, tenantId.getId());
        publishEvictEvent(new TenantEvictEvent(tenantId, true));
        deleteEntityRelations(tenantId, tenantId);
    }

    @Override
    public PageData<Tenant> findTenants(PageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenants(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public PageData<TenantInfo> findTenantInfos(PageLink pageLink) {
        log.trace("Executing findTenantInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantInfos(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantsByTenantProfileId [{}]", tenantProfileId);
        return tenantDao.findTenantIdsByTenantProfileId(tenantProfileId);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
    }

    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        log.trace("Executing findTenantsIds");
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantsIds(pageLink);
    }

    @Override
    public boolean tenantExists(TenantId tenantId) {
        return existsTenantCache.getAndPutInTransaction(tenantId, () -> tenantDao.existsById(tenantId, tenantId.getId()), false);
    }

    private PaginatedRemover<TenantId, Tenant> tenantsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Tenant> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return tenantDao.findTenants(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Tenant entity) {
            deleteTenant(TenantId.fromUUID(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findTenantById(new TenantId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
