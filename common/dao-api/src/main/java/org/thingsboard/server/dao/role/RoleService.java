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
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface RoleService extends EntityDaoService {

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
