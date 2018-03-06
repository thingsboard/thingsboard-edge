/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.converter.js;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.service.converter.AbstractDownlinkDataConverter;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.DownlinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.downlink.AttributeUpdate;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.downlink.RPCCall;

import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 04.12.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsConverterEvaluatorTest {

    final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void basicUplinkTest() throws ScriptException, NoSuchMethodException {
        JSUplinkEvaluator eval = createUplinkEvaluator("uplinkConverter.js");
        String result = eval.execute("ABC".getBytes(StandardCharsets.UTF_8), new UplinkMetaData("JSON", Collections.singletonMap("temperatureKeyName", "temperature")));
        Assert.assertEquals("{\"deviceName\":\"ABC\",\"telemetry\":{\"telemetryKeyName\":42}}", result);
    }

    @Test
    public void basicDownlinkTest() throws ScriptException, NoSuchMethodException, IOException {
        JSDownlinkEvaluator eval = createDownlinkEvaluator("downlinkConverter.js");

        DownLinkMsg downLinkMsg = new DownLinkMsg(new DeviceId(EntityId.NULL_UUID), "Sensor A", "temp-sensor");
        downLinkMsg.getUpdatedAttributes().put("temperature", new AttributeUpdate(System.currentTimeMillis(), "33"));
        downLinkMsg.getUpdatedAttributes().put("humidity", new AttributeUpdate(System.currentTimeMillis(), "78"));
        downLinkMsg.getDeletedAttributes().add("latitude");

        RPCCall rpcCall = new RPCCall();
        rpcCall.setId(UUID.randomUUID());
        rpcCall.setExpirationTime(System.currentTimeMillis() + 24 * 60 * 1000);
        rpcCall.setMethod("updateState");
        rpcCall.setParams("{\"status\": \"ACTIVE\"}");

        downLinkMsg.getRpcCalls().add(rpcCall);

        String downlinkPayload = mapper.writeValueAsString(downLinkMsg);

        String result = eval.execute(downlinkPayload, new DownLinkMetaData(Collections.singletonMap("topicPrefix", "sensor")));
        JsonElement element = new JsonParser().parse(result);
        Assert.assertTrue(element.isJsonObject());

        DownlinkData downlinkData = AbstractDownlinkDataConverter.parseDownlinkData(element.getAsJsonObject(), downLinkMsg);

        Assert.assertEquals("JSON", downlinkData.getContentType());
        Assert.assertEquals(1, downlinkData.getMetadata().size());
        Assert.assertTrue(downlinkData.getMetadata().containsKey("topic"));
        Assert.assertEquals("sensor/upload", downlinkData.getMetadata().get("topic"));

        JsonNode dataJson = mapper.readTree(downlinkData.getData());

        Assert.assertTrue(dataJson.has("temperature"));
        Assert.assertEquals("33", dataJson.get("temperature").asText());
    }

    private JSUplinkEvaluator createUplinkEvaluator(String scriptName) {
        InputStream src = JsConverterEvaluatorTest.class.getClassLoader().getResourceAsStream(scriptName);
        return new JSUplinkEvaluator(read(src));
    }

    private JSDownlinkEvaluator createDownlinkEvaluator(String scriptName) {
        InputStream src = JsConverterEvaluatorTest.class.getClassLoader().getResourceAsStream(scriptName);
        return new JSDownlinkEvaluator(read(src));
    }

    public static String read(InputStream input) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
