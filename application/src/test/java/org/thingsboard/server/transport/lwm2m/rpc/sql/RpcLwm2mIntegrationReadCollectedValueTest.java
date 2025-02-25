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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import java.util.concurrent.atomic.AtomicReference;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_TS_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_TS_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_VALUE_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3303_12_5700_VALUE_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3303_12_5700;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS;

@Slf4j
public class RpcLwm2mIntegrationReadCollectedValueTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Read {"id":"/3303/12/5700"}
     * Trigger a Send operation from the client with multiple values for the same resource as a payload
     * acked "[{"bn":"/3303/12/5700","bt":1724".. 116 bytes]
     * 2 values for the resource /3303/12/5700 should be stored with:
     * - timestamps1 =  Instance.now() + RESOURCE_ID_VALUE_3303_12_5700_1
     * - timestamps2 =  (timestamps1 + 3 sec) + RESOURCE_ID_VALUE_3303_12_5700_2
     * @throws Exception
     */
    @Test
    public void testReadSingleResource_sendFromClient_CollectedValue() throws Exception {
        // init test
        int cntValues = 2;
        int resourceId = 5700;
        String expectedIdVer = objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12 + "/" + resourceId;
        sendRPCById(expectedIdVer);

        // verify time start/end send CollectedValue;
        await().atMost(40, SECONDS).until(() -> RESOURCE_ID_3303_12_5700_TS_0 > 0
                                                    && RESOURCE_ID_3303_12_5700_TS_1 > 0);

        // verify result read: verify count value: 1-2: send CollectedValue;
        AtomicReference<ObjectNode> actualValues = new AtomicReference<>();
        await().atMost(40, SECONDS).until(() -> {
            actualValues.set(doGetAsync(
                    "/api/plugins/telemetry/DEVICE/" + lwM2MTestClient.getDeviceIdStr() + "/values/timeseries?keys="
                            + RESOURCE_ID_NAME_3303_12_5700
                            + "&startTs=" + (RESOURCE_ID_3303_12_5700_TS_0 - RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS)
                            + "&endTs=" + (RESOURCE_ID_3303_12_5700_TS_1 + RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS)
                            + "&interval=0&limit=100&useStrictDataTypes=false",
                    ObjectNode.class));
            return actualValues.get() != null && actualValues.get().size() > 0
                    && actualValues.get().get(RESOURCE_ID_NAME_3303_12_5700).size() >= cntValues && verifyTs(actualValues);
        });
    }

    private boolean verifyTs(AtomicReference<ObjectNode> actualValues) {
        String expectedVal_0 = String.valueOf(RESOURCE_ID_3303_12_5700_VALUE_0);
        String expectedVal_1 = String.valueOf(RESOURCE_ID_3303_12_5700_VALUE_1);
        ArrayNode actual = (ArrayNode) actualValues.get().get(RESOURCE_ID_NAME_3303_12_5700);
        long actualTS0 = 0;
        long actualTS1 = 0;
        for (JsonNode tsNode : actual) {
            if (tsNode.get("value").asText().equals(expectedVal_0)) {
                actualTS0 = tsNode.get("ts").asLong();
            } else if (tsNode.get("value").asText().equals(expectedVal_1)) {
                actualTS1 = tsNode.get("ts").asLong();
            }
        }
        return actualTS0 >= RESOURCE_ID_3303_12_5700_TS_0
                && actualTS1 <= RESOURCE_ID_3303_12_5700_TS_1
                && (actualTS1 - actualTS0) >= RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS;
    }

    private String sendRPCById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + lwM2MTestClient.getDeviceIdStr(), setRpcRequest, String.class, status().isOk());
    }
}
