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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_6;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;


public class RpcLwm2mIntegrationDiscoverWriteAttributesTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     *  <PROPERTIES> Class Attributes
     * - dim              (0-65535)      Integer: Multiple-Instance Resource; R, Number of instances existing for a Multiple-Instance Resource
     *  <NOTIFICATION> Class Attributes
     * - pmin             (def = 0(sec)) Integer: Object; Object Instance; Resource; Resource Instance; RW, Readable Resource
     * - pmax             (def = -- )    Integer: Object; Object Instance; Resource; Resource Instance; RW, Readable Resource
     * - Greater Than  gt (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     * - Less Than     lt (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     * - Step          st (def = -- )    Float:   Resource; Resource Instance; RW, Numerical&Readable Resource
     */


    /**
     * <PROPERTIES> Class Attributes
     * Object Version   ver   Object
     * Provide the  version of the associated Object.
     * "ver" only for objectId
     * <PROPERTIES> Class Attributes
     * Dimension 	dim Integer [0:255]
     * Number of instances existing for a Multiple-Instance Resource
     * <ObjectID>3</ObjectID>
     * 			<Item ID="6">
     * 				<Name>Available Power Sources</Name>
     *         <Operations>R</Operations>
     *         <MultipleInstances>Multiple</MultipleInstances>
     * 				<Type>Integer</Type>
     * 				<RangeEnumeration>0..7</RangeEnumeration>
     * WriteAttributes  implemented:	Discover {"id":"3/0/6"} ->  'dim' = 3
     */
    @Test
    public void testReadDIM_3_0_6_Only_R() throws Exception {
        String path = objectIdVer_3;
        String actualResult = sendDiscover(path);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3>;ver=1.2";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/6>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/7>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/8>;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expected = "</3/0/11>;dim=1";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * WriteAttributes {"id":"/3/0/14","attributes":{"pmax":100, "pmin":10}}
     * {"result":"CHANGED"}
     * result changed:
     *
     */
    @Test
    public void testWriteAttributesResourceWithParametersById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        String expectedValue = "{\"pmax\":100, \"pmin\":10}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        // result changed
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "</3/0/6>;pmax=100;pmin=10;dim=3";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    @Test
    public void testWriteAttributesResourceVerWithParametersById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_3;
        String expectedValue = "{\"ver\":1.3}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Attribute ver is of class PROPERTIES but only NOTIFICATION attribute can be used in WRITE ATTRIBUTE request.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    @Test
    public void testWriteAttributesObjectInstanceResourcePeriodLtGt_Return_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3;
        String expectedValue = "{\"pmax\":65, \"pmin\":5}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        String expectedValueStr = "gt=50;lt=42.2;st=0.5";
        JsonUtils.parse("{" + expectedValueStr + "}").toString();
        expectedValue = JsonUtils.parse("{" + expectedValueStr + "}").toString();
        actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        // ObjectId
        expectedPath = objectIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String actualValue = rpcActualResult.get("value").asText();
        String expected = "</3>;ver=1.2,</3/0>;pmax=65;pmin=5";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/6>;dim=3";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/7>;" + expectedValueStr + ";dim=3";
        assertTrue(actualValue.contains(expected));
        // ObjectInstanceId
        expectedPath = objectInstanceIdVer_3;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        actualValue = rpcActualResult.get("value").asText();
        expected = "</3/0>;pmax=65;pmin=5";
        assertTrue(actualValue.contains(expected));
        expected = "</3/0/7>;" + expectedValueStr + ";dim=3";
        assertTrue(actualValue.contains(expected));
        // ResourceId
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_6;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/6>;dim=3,</3/0/6/0>,</3/0/6/1>,</3/0/6/2>";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
        expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_7;
        actualResult = sendDiscover(expectedPath);
        rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        expected = "</3/0/7>;" + expectedValueStr;
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    private String sendRPCExecuteWithValueById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"WriteAttributes\", \"params\": {\"id\": \"" + path + "\", \"attributes\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }

    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Discover\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }
}
