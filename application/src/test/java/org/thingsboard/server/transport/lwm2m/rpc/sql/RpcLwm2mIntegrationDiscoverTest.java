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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;


public class RpcLwm2mIntegrationDiscoverTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * DiscoverAll
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverAll_Return_CONTENT_LinksAllObjectsAllInstancesOfClient() throws Exception {
        String setRpcRequest = "{\"method\":\"DiscoverAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        JsonNode rpcActualValue = JacksonUtil.toJsonNode(rpcActualResult.get("value").asText());
        Set actualObjects = ConcurrentHashMap.newKeySet();
        Set actualInstances = ConcurrentHashMap.newKeySet();
        rpcActualValue.forEach(node -> {
            if (!node.get("uriReference").asText().equals("/")) {
                LwM2mPath path = new LwM2mPath(node.get("uriReference").asText());
                actualObjects.add("/" + path.getObjectId());
                if (path.isObjectInstance()) {
                    actualInstances.add("/" + path.getObjectId() + "/" + path.getObjectInstanceId());
                }
            }
        });
        assertEquals(expectedInstances, actualInstances);
        assertEquals(expectedObjects, actualObjects);
    }

    /**
     * Discover {"id":"/3"}
     *
     * @throws Exception
     */
    @Test
    public void testDiscoverObject_Return_CONTENT_LinksInstancesAndResourcesOnLyExpectedObject() {
        expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendDiscover((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String[] actualValues = rpcActualResult.get("value").asText().split(",");
                assertTrue(actualValues.length > 0);
                assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectId)).collect(Collectors.toList()).size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Discover {"id":"3/0"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/3>,</3/0/4>,</3/0/5>,</3/0/6>,</3/0/7>,</3/0/8>,</3/0/9>,</3/0/10>,</3/0/11>,</3/0/12>,</3/0/13>,</3/0/14>,</3/0/15>,</3/0/16>,</3/0/1
     * 7>,</3/0/18>,</3/0/19>,</3/0/20>,</3/0/21>,</3/0/22>"}
     * If WriteAttributes implemented and WriteAttributes saved
     * Discover {"id":"19/0"}
     * {"result":"CONTENT","value":"[</19/0>;dim=2;pmin=10;pmax=60;gt=50;lt=42.2,</19/0/0>;pmax=120, </19/0/1>, </19/0/2>, </19/0/3>, </19/0/4>, </19/0/5>;lt=45]"}
     */
    @Test
    public void testDiscoverInstance_Return_CONTENT_LinksResourcesOnLyExpectedInstance() throws Exception {
        String expected = (String) expectedObjectIdVerInstances.stream().findAny().get();
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedObjectInstanceId = pathIdVerToObjectId(expected);
        String[] actualValues = rpcActualResult.get("value").asText().split(",");
        assertTrue(actualValues.length > 0);
        assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectInstanceId)).collect(Collectors.toList()).size());
    }

    /**
     * Discover {"id":"3/0/14"}
     * If WriteAttributes implemented:
     * {"result":"CONTENT","value":"</3/0/14>;pmax=100, "pmin":10, "ver"=1.0"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</3/0/14>"}
     * Discover {"id":"19_1.1/0/0"}
     * If WriteAttributes implemented:
     * {"result":"CONTENT","value":"</19/0/0>;pmax=100, "pmin":10, "ver"=1.1"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</19/0/0>"}
     */
    @Test
    public void testDiscoverResource_Return_CONTENT_LinksResourceOnLyExpectedResource() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().findFirst().get();
        String expectedObjectInstanceId = pathIdVerToObjectId(expectedInstance);
        LwM2mPath expectedPath = new LwM2mPath(expectedObjectInstanceId);
        int expectedResource = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String expected = expectedInstance + "/" + expectedResource;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedResourceId = "<" + expectedObjectInstanceId + "/" + expectedResource + ">";
        String actualValue = rpcActualResult.get("value").asText();
        assertEquals(expectedResourceId, actualValue );

    }

    /**
     * Discover {"id":"2/0"}
     *{"result":"NOT_FOUND"}
     */
    @Test
    public void testDiscoverObjectInstanceAbsentInObject_Return_NOT_FOUND() throws Exception {
        String expected = objectIdVer_2 + "/" + OBJECT_INSTANCE_ID_0;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }
    /**
     * Discover {"id":"2/0/2"}
     * {"result":"NOT_FOUND"}
     */
    @Test
    public void testDiscoverResourceAbsentInObject_Return_NOT_FOUND() throws Exception {
         String expected = objectIdVer_2 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Discover\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
