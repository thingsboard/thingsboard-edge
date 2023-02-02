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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;


public class RpcLwm2mIntegrationDeleteTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * if there is such an instance
     * Delete {"id":"/3303/12"}
     * {"result":"DELETE"}
     */
    @Test
    public void testDeleteObjectInstanceIsSuchByIdKey_Result_DELETED() throws Exception {
        String expectedPath = objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.DELETED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * if there is no such instance
     * Delete {"id":"/19/12"}
     * {"result":"NOT_FOUND"}
     */
    @Test
    public void testDeleteObjectInstanceIsNotSuchByIdKey_Result_NOT_FOUND() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * delete object
     * Delete {"id":"/19_1.1"}
     * {"result":"BAD_REQUEST","error":"Invalid path /19 : Only object instances can be delete"}
     */
    @Test
    public void testDeleteObjectByIdKey_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_19;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Invalid path " + pathIdVerToObjectId((String) expectedPath) + " : Only object instances can be delete";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }


    /**
     * delete resource
     * Delete {"id":"/3/0/7"}
     * {"result":"METHOD_NOT_ALLOWED"}
     */
    @Test
    public void testDeleteResourceByIdKey_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + RESOURCE_ID_7;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }


    private String sendRPCDeleteById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Delete\", \"params\": {\"id\": \"" + path  + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
