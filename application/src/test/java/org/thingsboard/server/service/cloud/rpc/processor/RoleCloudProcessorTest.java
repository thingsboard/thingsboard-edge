// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class RoleCloudProcessorTest {

    private RoleCloudProcessor processor;

    @Before
    public void setUp() {
        processor = new RoleCloudProcessor();
    }

    @After
    public void tearDown() {}

    @Test
    public void testReplaceWriteOperationsToReadIfRequired() throws JsonProcessingException {
        Role role = new Role();

        ArrayNode assetOperations = JacksonUtil.newArrayNode();
        assetOperations.add(Operation.READ.name());
        assetOperations.add(Operation.WRITE.name());

        ArrayNode deviceOperations = JacksonUtil.newArrayNode();
        deviceOperations.add(Operation.READ.name());

        ArrayNode allOperations = JacksonUtil.newArrayNode();
        allOperations.add(Operation.ALL.name());

        ObjectNode permissions = JacksonUtil.newObjectNode();
        permissions.set(Resource.ALL.name(), allOperations);
        permissions.set(Resource.DEVICE.name(), deviceOperations);
        permissions.set(Resource.ASSET.name(), assetOperations);
        role.setPermissions(permissions);
        role.setType(RoleType.GENERIC);

        Role updatedRole = processor.replaceWriteOperationsToReadIfRequired(role);

        CollectionType operationType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
        JavaType resourceType = JacksonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Resource.class);
        MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, resourceType, operationType);
        Map<Resource, List<Operation>> newPermissions = JacksonUtil.fromString(updatedRole.getPermissions().toString(), mapType);

        Assert.assertTrue(newPermissions.containsKey(Resource.ALL));
        Assert.assertFalse(newPermissions.get(Resource.ALL).contains(Operation.ALL));

        Assert.assertTrue(newPermissions.containsKey(Resource.DEVICE_GROUP));
        Assert.assertTrue(newPermissions.containsKey(Resource.DEVICE));
        Assert.assertTrue(newPermissions.containsKey(Resource.ALARM));
        Assert.assertTrue(newPermissions.containsKey(Resource.ASSET));
    }

    @Test
    public void testProfilePermissionIsPreserved() {
        ArrayNode allOperations = JacksonUtil.newArrayNode();
        allOperations.add(Operation.ALL.name());

        ObjectNode profilePermissions = JacksonUtil.newObjectNode();
        profilePermissions.set(Resource.PROFILE.name(), allOperations);
        Map<Resource, List<Operation>> newPermissions = replaceOperations(profilePermissions);
        Assert.assertEquals(List.of(Operation.ALL), newPermissions.get(Resource.PROFILE));

        ObjectNode allPermissions = JacksonUtil.newObjectNode();
        allPermissions.set(Resource.ALL.name(), allOperations);
        newPermissions = replaceOperations(allPermissions);
        Assert.assertEquals(List.of(Operation.ALL), newPermissions.get(Resource.PROFILE));
        Assert.assertTrue(newPermissions.getOrDefault(Resource.VERSION_CONTROL, List.of()).isEmpty());
    }

    private Map<Resource, List<Operation>> replaceOperations(ObjectNode permissions) {
        Role role = new Role();
        role.setPermissions(permissions);
        role.setType(RoleType.GENERIC);

        Role updatedRole = processor.replaceWriteOperationsToReadIfRequired(role);

        CollectionType operationType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
        JavaType resourceType = JacksonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Resource.class);
        MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, resourceType, operationType);
        return JacksonUtil.fromString(updatedRole.getPermissions().toString(), mapType);
    }

}