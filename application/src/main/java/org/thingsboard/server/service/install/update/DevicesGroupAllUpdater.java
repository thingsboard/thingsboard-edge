/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;

class DevicesGroupAllUpdater extends EntityGroupAllPaginatedUpdater<DeviceId, Device> {

    private final DeviceService deviceService;

    public DevicesGroupAllUpdater(DeviceService deviceService, CustomerService customerService,
                                  EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                deviceService::findDevicesByTenantId,
                deviceService::findDevicesByTenantIdAndIdsAsync,
                entityId -> new DeviceId(entityId.getId()),
                device -> device.getId());
        this.deviceService = deviceService;
    }

    @Override
    protected void unassignFromCustomer(Device entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        deviceService.saveDevice(entity);
    }

    @Override
    protected String getName() {
        return "Devices group all updater";
    }
}
