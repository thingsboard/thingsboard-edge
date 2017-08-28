/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Override
    public void updateData(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.3.0":
            case "1.3.1":
                log.info("Updating data from version {} to 1.3.1EE ...", fromVersion);

                tenantsGroupAllUpdater.updateEntities(null);

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
