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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RoleClientTest extends AbstractContainerTest {

    @Test
    public void testRoles() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoles(RoleType.GENERIC, new PageLink(100)).getTotalElements() == 2);

        PageData<Role> genericPageData = edgeRestClient.getRoles(RoleType.GENERIC, new PageLink(100));

        List<EntityId> genericIds = genericPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoles(RoleType.GROUP, new PageLink(100)).getTotalElements() == 1);
        PageData<Role> groupPageData = edgeRestClient.getRoles(RoleType.GROUP, new PageLink(100));
        List<EntityId> groupIds = groupPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        genericIds.addAll(groupIds);
        assertEntitiesByIdsAndType(genericIds, EntityType.ROLE);
    }

    @Test
    public void testTenantRole() {
        // create role
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Generic Edge Role");
        Role savedRole = cloudRestClient.saveRole(role);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoleById(savedRole.getId()).isPresent());
        assertEntitiesByIdsAndType(Collections.singletonList(savedRole.getId()), EntityType.ROLE);

        // update role
        savedRole.setName("Generic Edge Role Updated");
        cloudRestClient.saveRole(savedRole);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Generic Edge Role Updated".equals(edgeRestClient.getRoleById(savedRole.getId()).get().getName()));

        // delete role
        cloudRestClient.deleteRole(savedRole.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoleById(savedRole.getId()).isEmpty());
    }

    @Test
    public void testCustomerRole() {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer A", null);

        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Customer Generic Edge Role");
        role.setOwnerId(savedCustomer.getId());
        role.setCustomerId(savedCustomer.getId());
        Role savedRole = cloudRestClient.saveRole(role);

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoleById(savedRole.getId()).isPresent());
        assertEntitiesByIdsAndType(Collections.singletonList(savedRole.getId()), EntityType.ROLE);

        // update role
        savedRole.setName("Customer Generic Edge Role Updated");
        cloudRestClient.saveRole(savedRole);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Customer Generic Edge Role Updated".equals(edgeRestClient.getRoleById(savedRole.getId()).get().getName()));

        // delete role
        cloudRestClient.deleteRole(savedRole.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRoleById(savedRole.getId()).isEmpty());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // delete customers
        cloudRestClient.deleteCustomer(savedCustomer.getId());
    }

}

