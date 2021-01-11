/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.RoleType;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    Role saveRole(TenantId tenantId, Role role);

    Role findRoleById(TenantId tenantId, RoleId roleId);

    ListenableFuture<List<Role>> findRolesByIdsAsync(TenantId tenantId, List<RoleId> roleIds);

    Optional<Role> findRoleByTenantIdAndName(TenantId tenantId, String name);

    Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(TenantId tenantId, CustomerId customerId, String name);

    PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndType(TenantId tenantId, PageLink pageLink, RoleType type);

    ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId);

    void deleteRole(TenantId tenantId, RoleId roleId);

    void deleteRolesByTenantId(TenantId tenantId);

    void deleteRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    Role findOrCreateRole(TenantId tenantId, CustomerId customerId, RoleType type,
                          String name, Object permissions, String description);

    Role findOrCreateTenantUserRole();

    Role findOrCreateTenantAdminRole();

    Role findOrCreateCustomerUserRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateCustomerAdminRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreatePublicUsersEntityGroupRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreatePublicUserRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateReadOnlyEntityGroupRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateWriteEntityGroupRole(TenantId tenantId, CustomerId customerId);

    PageData<Role> findRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, RoleType type, PageLink pageLink);
}
