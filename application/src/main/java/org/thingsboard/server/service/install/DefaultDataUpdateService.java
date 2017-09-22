/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Override
    public void updateData(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.3.1":
                log.info("Updating data from version 1.3.1 to 1.3.1PE ...");

                tenantsGroupAllUpdater.updateEntities(null);

                AdminSettings mailTemplateSettings = adminSettingsService.findAdminSettingsByKey("mailTemplates");
                if (mailTemplateSettings == null) {
                    systemDataLoaderService.loadMailTemplates();
                }

                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsGroupAllUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected List<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink).getData();
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE};
                        for (EntityType groupType : entityGroupTypes) {
                            Optional<EntityGroup> entityGroupOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(tenant.getId(), groupType, EntityGroup.GROUP_ALL_NAME).get();
                            if (!entityGroupOptional.isPresent()) {
                                entityGroupService.createEntityGroupAll(tenant.getId(), groupType);
                                switch (groupType) {
                                    case USER:
                                        usersGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case CUSTOMER:
                                        customersGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case ASSET:
                                        assetsGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case DEVICE:
                                        devicesGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
    };

    private PaginatedUpdater<TenantId, User> usersGroupAllUpdater = new PaginatedUpdater<TenantId, User>() {
        @Override
        protected List<User> findEntities(TenantId id, TextPageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink).getData();
        }
        @Override
        protected void updateEntity(User entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Customer> customersGroupAllUpdater = new PaginatedUpdater<TenantId, Customer>() {
        @Override
        protected List<Customer> findEntities(TenantId id, TextPageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink).getData();
        }
        @Override
        protected void updateEntity(Customer entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Asset> assetsGroupAllUpdater = new PaginatedUpdater<TenantId, Asset>() {
        @Override
        protected List<Asset> findEntities(TenantId id, TextPageLink pageLink) {
            return assetService.findAssetsByTenantId(id, pageLink).getData();
        }
        @Override
        protected void updateEntity(Asset entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Device> devicesGroupAllUpdater = new PaginatedUpdater<TenantId, Device>() {
        @Override
        protected List<Device> findEntities(TenantId id, TextPageLink pageLink) {
            return deviceService.findDevicesByTenantId(id, pageLink).getData();
        }
        @Override
        protected void updateEntity(Device entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    public abstract class PaginatedUpdater<I, D extends IdBased<?>> {

        private static final int DEFAULT_LIMIT = 100;

        public void updateEntities(I id) {
            TextPageLink pageLink = new TextPageLink(DEFAULT_LIMIT);
            boolean hasNext = true;
            while (hasNext) {
                List<D> entities = findEntities(id, pageLink);
                for (D entity : entities) {
                    updateEntity(entity);
                }
                hasNext = entities.size() == pageLink.getLimit();
                if (hasNext) {
                    int index = entities.size() - 1;
                    UUID idOffset = entities.get(index).getUuidId();
                    pageLink.setIdOffset(idOffset);
                }
            }
        }

        protected abstract List<D> findEntities(I id, TextPageLink pageLink);

        protected abstract void updateEntity(D entity);

    }

}
