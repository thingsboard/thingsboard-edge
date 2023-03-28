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

        ArrayNode assetOperations = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        assetOperations.add(Operation.READ.name());
        assetOperations.add(Operation.WRITE.name());

        ArrayNode deviceOperations = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        deviceOperations.add(Operation.READ.name());

        ArrayNode allOperations = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        allOperations.add(Operation.ALL.name());

        ObjectNode permissions = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        permissions.set(Resource.ALL.name(), allOperations);
        permissions.set(Resource.DEVICE.name(), deviceOperations);
        permissions.set(Resource.ASSET.name(), assetOperations);
        role.setPermissions(permissions);
        role.setType(RoleType.GENERIC);

        Role updatedRole = processor.replaceWriteOperationsToReadIfRequired(role);

        CollectionType operationType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
        JavaType resourceType = JacksonUtil.OBJECT_MAPPER.getTypeFactory().constructType(Resource.class);
        MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, resourceType, operationType);
        Map<Resource, List<Operation>> newPermissions = JacksonUtil.OBJECT_MAPPER.readValue(updatedRole.getPermissions().toString(), mapType);

        Assert.assertTrue(newPermissions.containsKey(Resource.ALL));
        Assert.assertFalse(newPermissions.get(Resource.ALL).contains(Operation.ALL));

        Assert.assertTrue(newPermissions.containsKey(Resource.DEVICE_GROUP));
        Assert.assertTrue(newPermissions.containsKey(Resource.DEVICE));
        Assert.assertTrue(newPermissions.containsKey(Resource.ALARM));
        Assert.assertTrue(newPermissions.containsKey(Resource.ASSET));
    }

}