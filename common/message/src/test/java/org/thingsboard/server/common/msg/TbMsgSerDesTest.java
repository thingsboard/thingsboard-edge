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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TbMsgSerDesTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static EasyRandom easyRandom;

    @BeforeAll
    static void init() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(TbMsgCallback.class, () -> TbMsgCallback.EMPTY)
                .randomize(EntityId.class, () -> new DeviceId(UUID.randomUUID()));
        easyRandom = new EasyRandom(parameters);
    }

    @Test
    public void tbMsgJsonSerializationDeserialization() throws Exception {
        TbMsg tbMsg = easyRandom.nextObject(TbMsg.class);
        byte[] bytes = objectMapper.writeValueAsBytes(tbMsg);
        TbMsg deserializedTbMsg = objectMapper.readValue(bytes, TbMsg.class);
        assertNotNull(deserializedTbMsg);

        assertEquals(tbMsg.getQueueName(), deserializedTbMsg.getQueueName());
        assertEquals(tbMsg.getId(), deserializedTbMsg.getId());
        assertEquals(tbMsg.getTs(), deserializedTbMsg.getTs());
        assertEquals(tbMsg.getType(), deserializedTbMsg.getType());
        assertEquals(tbMsg.getInternalType(), deserializedTbMsg.getInternalType());
        assertEquals(tbMsg.getOriginator(), deserializedTbMsg.getOriginator());
        assertEquals(tbMsg.getCustomerId(), deserializedTbMsg.getCustomerId());
        assertEquals(tbMsg.getMetaData(), deserializedTbMsg.getMetaData());
        assertEquals(tbMsg.getDataType(), deserializedTbMsg.getDataType());
        assertEquals(tbMsg.getData(), deserializedTbMsg.getData());
        assertEquals(tbMsg.getRuleChainId(), deserializedTbMsg.getRuleChainId());
        assertEquals(tbMsg.getRuleNodeId(), deserializedTbMsg.getRuleNodeId());
        assertEquals(tbMsg.getCorrelationId(), deserializedTbMsg.getCorrelationId());
        assertEquals(tbMsg.getPartition(), deserializedTbMsg.getPartition());
    }

}
