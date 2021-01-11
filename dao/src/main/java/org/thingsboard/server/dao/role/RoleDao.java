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
package org.thingsboard.server.dao.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleDao extends Dao<Role> {

    Role save(TenantId tenantId, Role role);

    PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndType(UUID tenantId, RoleType type, PageLink pageLink);

    Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name);

    Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name);

    PageData<Role> findRolesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, RoleType type, PageLink pageLink);

    /**
     * Find roles by tenantId and role Ids.
     *
     * @param tenantId the tenantId
     * @param roleIds the role Ids
     * @return the list of role objects
     */
    ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds);

}
