/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.edqs.repo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitiesByGroupNameFilterTest extends AbstractEDQTest {

    private UUID deviceId;
    private UUID deviceId2;
    private UUID deviceId3;

    private UUID groupAId;
    private UUID groupBId;

    @Before
    public void setUp() {
        deviceId = createDevice(customerId, "Lora-1");
        deviceId2 = createDevice(customerId, "Lora-2");
        deviceId3 = createDevice(customerId, "Lora-3");

        // add device and device 2 to Group A
        groupAId = createGroup(customerId.getId(), EntityType.DEVICE, "Group A");
        createRelation(EntityType.ENTITY_GROUP, groupAId, EntityType.DEVICE, deviceId, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
        createRelation(EntityType.ENTITY_GROUP, groupAId, EntityType.DEVICE, deviceId2, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");

        // add device and device 3 to Group B
        groupBId = createGroup(customerId.getId(), EntityType.DEVICE, "Group B");
        createRelation(EntityType.ENTITY_GROUP, groupBId, EntityType.DEVICE, deviceId3, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantEntitiesOfGroupA() {
        // get entity list
        var result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group A", null, null), false);

        Assert.assertEquals(2, result.getTotalElements());
        List<UUID> entityIds = result.getData().stream().map(queryResult -> queryResult.getEntityId().getId()).toList();
        assertThat(entityIds).containsOnly(deviceId, deviceId2);

        result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group B", null, null), false);
        Assert.assertEquals(1, result.getTotalElements());
    }

    @Test
    public void testFindCustomerEntitiesOfGroupA() {
        var result = repository.findEntityDataByQuery(tenantId, new CustomerId(UUID.randomUUID()), RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group A", null, null), false);
        Assert.assertEquals(0, result.getTotalElements());

        result = repository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group A", null, null), false);
        Assert.assertEquals(2, result.getTotalElements());
        List<UUID> entityIds = result.getData().stream().map(queryResult -> queryResult.getEntityId().getId()).toList();
        assertThat(entityIds).containsOnly(deviceId, deviceId2);
    }

    @Test
    public void testFindCustomerEntitiesOfGroupAWithGroupPermission() {
        MergedUserPermissions groupPermission = new MergedUserPermissions(
                Collections.emptyMap(), Map.of(new EntityGroupId(groupAId), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        var result = repository.findEntityDataByQuery(tenantId, customerId, groupPermission, getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group A", null, null),  false);
        Assert.assertEquals(2, result.getTotalElements());
        List<UUID> entityIds = result.getData().stream().map(queryResult -> queryResult.getEntityId().getId()).toList();
        assertThat(entityIds).containsOnly(deviceId, deviceId2);
    }

    @Test
    public void testFindGroupWithGenericAndGroupPermission() {
        CustomerId subCustomer = new CustomerId(UUID.randomUUID());
        createCustomer(subCustomer.getId(), customerId.getId(), "Subcustomer A");
        UUID subCustomerGroupId = createGroup(subCustomer.getId(), EntityType.DEVICE, "Group B");
        UUID deviceId4 = createDevice(subCustomer, "Lora-4");
        createRelation(EntityType.ENTITY_GROUP, subCustomerGroupId, EntityType.DEVICE, deviceId4, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");

        MergedUserPermissions groupPermission = new MergedUserPermissions(
                Map.of(Resource.ALL, Set.of(Operation.ALL)), Map.of(new EntityGroupId(groupBId), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        var result = repository.findEntityDataByQuery(tenantId, subCustomer, groupPermission,
                getEntitiesByGroupNameDataQuery(EntityType.DEVICE, "Group B", null, null), false);

        Assert.assertEquals(1, result.getTotalElements());
    }

    private static EntityDataQuery getEntitiesByGroupNameDataQuery(EntityType entityType, String groupName, EntityId ownerId, List<KeyFilter> keyFilters) {
        EntitiesByGroupNameFilter filter = new EntitiesByGroupNameFilter();
        filter.setGroupType(entityType);
        filter.setEntityGroupNameFilter(groupName);
        filter.setOwnerId(ownerId);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

        return new EntityDataQuery(filter, pageLink, entityFields, null, keyFilters);
    }

}
