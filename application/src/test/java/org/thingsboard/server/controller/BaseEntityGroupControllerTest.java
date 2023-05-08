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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseEntityGroupControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveEntityGroup() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        Assert.assertNotNull(savedEntityGroup);
        Assert.assertNotNull(savedEntityGroup.getId());
        Assert.assertTrue(savedEntityGroup.getCreatedTime() > 0);
        Assert.assertEquals(entityGroup.getName(), savedEntityGroup.getName());
        savedEntityGroup.setName("New Entity Group");
        doPost("/api/entityGroup", savedEntityGroup, EntityGroup.class);
        EntityGroup foundEntityGroup = doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString(), EntityGroup.class);
        Assert.assertEquals(savedEntityGroup.getName(), foundEntityGroup.getName());
    }

    @Test
    public void testSaveEntityGroupWithSameName() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        Assert.assertNotNull(savedEntityGroup);
        Assert.assertNotNull(savedEntityGroup.getId());
        EntityGroup entityGroup2 = new EntityGroup();
        entityGroup2.setName("Entity Group");
        entityGroup2.setType(EntityType.DEVICE);
        doPost("/api/entityGroup", entityGroup2).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity Group with such name, type and owner already exists!")));
    }

    @Test
    public void testFindEntityGroupById() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
        EntityGroup foundEntityGroup = doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString(), EntityGroup.class);
        Assert.assertNotNull(foundEntityGroup);
        Assert.assertEquals(savedEntityGroup, foundEntityGroup);
    }

    @Test
    public void testDeleteEntityGroup() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Entity Group");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        doDelete("/api/entityGroup/" + savedEntityGroup.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/entityGroup/" + savedEntityGroup.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    // @voba - merge comment
    // edge assign functionality only in CE/PE
    // @Test
    public void testFindEdgeEntityGroupsByTenantIdAndNameAndType() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<EntityGroupId> edgeEntityGroupIds = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setType(EntityType.DEVICE);
            entityGroup.setName("Scheduler Event " + i);
            EntityGroup savedEntityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/entityGroup/" + savedEntityGroup.getId().getId().toString() + "/DEVICE", EntityGroup.class);
            edgeEntityGroupIds.add(savedEntityGroup.getId());
        }

        List<EntityGroupId> loadedEdgeEntityGroupIds = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<EntityGroupInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/entityGroups/edge/" + savedEdge.getId().getId() + "/DEVICE?",
                    new TypeReference<>() {}, pageLink);
            loadedEdgeEntityGroupIds.addAll(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(edgeEntityGroupIds.size() == loadedEdgeEntityGroupIds.size() &&
                edgeEntityGroupIds.containsAll(loadedEdgeEntityGroupIds));

        for (EntityGroupId entityGroupId : loadedEdgeEntityGroupIds) {
            doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/entityGroup/" + entityGroupId.getId().toString() + "/DEVICE", EntityGroup.class);
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/entityGroups/edge/" + savedEdge.getId().getId() + "/DEVICE?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getTotalElements());
    }
}
