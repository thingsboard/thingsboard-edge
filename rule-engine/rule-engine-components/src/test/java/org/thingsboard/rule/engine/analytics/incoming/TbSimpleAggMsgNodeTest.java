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
package org.thingsboard.rule.engine.analytics.incoming;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class TbSimpleAggMsgNodeTest {

    TbSimpleAggMsgNode node;
    TbSimpleAggMsgNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @Before
    public void setUp() throws TbNodeException {
        ctx = mock(TbContext.class);
        callback = mock(TbMsgCallback.class);

        config = new TbSimpleAggMsgNodeConfiguration().defaultConfiguration();
        config.setTimeZoneId("UTC");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        node = spy(new TbSimpleAggMsgNode());

        doNothing().when(node).scheduleReportTickMsg(any(), any());

        node.init(ctx, nodeConfiguration);
    }

    @Test
    public void givenNullValue_whenOnMsg_thenThrowException() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metaData = new TbMsgMetaData();

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("temperature", null);

        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", deviceId, metaData, jsonObject.toString(), callback);

        Assert.assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctx, msg));
    }

    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var node = new TbSimpleAggMsgNode();
        String oldConfigStr = "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5,\"queueName\":null,\"mathFunction\":\"AVG\"," +
                "\"aggIntervalType\":\"HOUR\",\"timeZoneId\":\"UTC\",\"aggIntervalTimeUnit\":\"HOURS\"," +
                "\"aggIntervalValue\":1,\"autoCreateIntervals\":false,\"intervalPersistencePolicy\":\"ON_EACH_CHECK_AFTER_INTERVAL_END\"," +
                "\"intervalCheckTimeUnit\":\"MINUTES\",\"intervalCheckValue\":1,\"inputValueKey\":\"temperature\"," +
                "\"outputValueKey\":\"avgHourlyTemperature\",\"statePersistencePolicy\":\"ON_EACH_CHANGE\"," +
                "\"statePersistenceTimeUnit\":\"MINUTES\",\"statePersistenceValue\":1}";
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, JacksonUtil.toJsonNode(oldConfigStr));
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(config, JacksonUtil.treeToValue(upgrade.getSecond(), config.getClass()));
    }

    @Test
    public void givenNewConfigWithOldVersion_whenUpgrade_thenShouldReturnFalseResultWithTheSameConfig() throws Exception {
        var node = new TbSimpleAggMsgNode();
        JsonNode expectedConfig = JacksonUtil.valueToTree(config);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, expectedConfig);
        Assertions.assertFalse(upgrade.getFirst());
        Assertions.assertEquals(config, JacksonUtil.treeToValue(upgrade.getSecond(), config.getClass()));
    }

}