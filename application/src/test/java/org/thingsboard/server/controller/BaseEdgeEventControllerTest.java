/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class BaseEdgeEventControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
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
    public void testGetEdgeEvents() throws Exception {
        Thread.sleep(500);
        Edge edge = constructEdge("TestEdge", "default");
        edge = doPost("/api/edge", edge, Edge.class);

        EntityGroup deviceEntityGroup = constructEntityGroup("TestDevice", EntityType.DEVICE);
        EntityGroup savedDeviceEntityGroup = doPost("/api/entityGroup", deviceEntityGroup, EntityGroup.class);
        Device device = constructDevice("TestDevice", "default");
        Device savedDevice =
                doPost("/api/device?entityGroupId=" + savedDeviceEntityGroup.getId().getId().toString(), device, Device.class);
        Thread.sleep(1000);

        doPost("/api/edge/" + edge.getId().toString() + "/entityGroup/" + savedDeviceEntityGroup.getId().toString() + "/DEVICE", EntityGroup.class);
        Thread.sleep(500);

        Device device2 = constructDevice("TestDevice2", "default");
        doPost("/api/device?entityGroupId=" + savedDeviceEntityGroup.getId().getId().toString(), device2, Device.class);
        EntityGroup assetEntityGroup = constructEntityGroup("TestDevice", EntityType.ASSET);
        EntityGroup savedAssetEntityGroup = doPost("/api/entityGroup", assetEntityGroup, EntityGroup.class);
        Asset asset = constructAsset("TestAsset", "default");
        Asset savedAsset = doPost("/api/asset?entityGroupId=" + savedAssetEntityGroup.getId().getId().toString(), asset, Asset.class);
        Thread.sleep(500);

        doPost("/api/edge/" + edge.getId().toString() + "/entityGroup/" + savedAssetEntityGroup.getId().toString()+ "/ASSET", EntityGroup.class);
        Thread.sleep(1000);

        Asset asset2 = constructAsset("TestAsset2", "default");
        doPost("/api/asset?entityGroupId=" + savedAssetEntityGroup.getId().getId().toString(), asset2, Asset.class);
        EntityRelation relation = new EntityRelation(savedAsset.getId(), savedDevice.getId(), EntityRelation.CONTAINS_TYPE);
        doPost("/api/relation", relation);
        Thread.sleep(500);

        List<EdgeEvent> edgeEvents = doGetTypedWithTimePageLink("/api/edge/" + edge.getId().toString() + "/events?",
                new TypeReference<PageData<EdgeEvent>>() {
                }, new TimePageLink(10)).getData();

        Assert.assertFalse(edgeEvents.isEmpty());

        Assert.assertEquals(6, edgeEvents.size());

        Assert.assertEquals(EdgeEventType.RULE_CHAIN, edgeEvents.get(0).getType());
        Assert.assertEquals(EdgeEventActionType.UPDATED, edgeEvents.get(0).getAction());

        Assert.assertEquals(EdgeEventType.ENTITY_GROUP, edgeEvents.get(1).getType());
        Assert.assertEquals(EdgeEventActionType.ASSIGNED_TO_EDGE, edgeEvents.get(1).getAction());

        Assert.assertEquals(EdgeEventType.DEVICE, edgeEvents.get(2).getType());
        Assert.assertEquals(EdgeEventActionType.ADDED_TO_ENTITY_GROUP, edgeEvents.get(2).getAction());
        Assert.assertEquals(savedDeviceEntityGroup.getUuidId(), edgeEvents.get(2).getEntityGroupId());

        Assert.assertEquals(EdgeEventType.ENTITY_GROUP, edgeEvents.get(3).getType());
        Assert.assertEquals(EdgeEventActionType.ASSIGNED_TO_EDGE, edgeEvents.get(3).getAction());

        Assert.assertEquals(EdgeEventType.ASSET, edgeEvents.get(4).getType());
        Assert.assertEquals(EdgeEventActionType.ADDED_TO_ENTITY_GROUP, edgeEvents.get(4).getAction());
        Assert.assertEquals(savedAssetEntityGroup.getUuidId(), edgeEvents.get(4).getEntityGroupId());

        Assert.assertEquals(EdgeEventType.RELATION, edgeEvents.get(5).getType());
        Assert.assertEquals(EdgeEventActionType.RELATION_ADD_OR_UPDATE, edgeEvents.get(5).getAction());
    }

    private EntityGroup constructEntityGroup(String name, EntityType type) {
        EntityGroup result = new EntityGroup();
        result.setName(name);
        result.setType(type);
        return result;
    }

    private Device constructDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return device;
    }

    private Asset constructAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return asset;
    }
}
