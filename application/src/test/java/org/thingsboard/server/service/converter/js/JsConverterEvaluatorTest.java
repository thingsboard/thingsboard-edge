/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.converter.js;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.integration.api.converter.JSDownlinkEvaluator;
import org.thingsboard.integration.api.converter.JSUplinkEvaluator;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.integration.api.converter.AbstractDownlinkDataConverter;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.service.script.TestNashornJsInvokeService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 04.12.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsConverterEvaluatorTest {

    final ObjectMapper mapper = new ObjectMapper();

    private TestNashornJsInvokeService jsSandboxService;
    private ConverterId converterId = new ConverterId(UUIDs.timeBased());

    @Before
    public void beforeTest() throws Exception {
        jsSandboxService = new TestNashornJsInvokeService(false, 1, 100, 3);
    }

    @After
    public void afterTest() throws Exception {
        jsSandboxService.stop();
    }

    @Test
    public void basicUplinkTest() throws Exception {
        JSUplinkEvaluator eval = createUplinkEvaluator("uplinkConverter.js");
        String result = eval.execute("ABC".getBytes(StandardCharsets.UTF_8), new UplinkMetaData("JSON", Collections.singletonMap("temperatureKeyName", "temperature")));
        Assert.assertEquals("{\"deviceName\":\"ABC\",\"telemetry\":{\"telemetryKeyName\":42}}", result);
        eval.destroy();
    }

    @Test
    public void basicDownlinkTest() throws Exception {
        JSDownlinkEvaluator eval = createDownlinkEvaluator("downlinkConverter.js");

        String rawJson = "{\"temperature\": 33, \"humidity\": 78}";

        TbMsgMetaData metaData = new TbMsgMetaData();
        IntegrationMetaData integrationMetaData = new IntegrationMetaData(Collections.singletonMap("topicPrefix", "sensor"));
        TbMsg msg = TbMsg.newMsg("USER", null, metaData, rawJson);

        JsonNode result = eval.execute(msg, integrationMetaData);
        Assert.assertTrue(result.isObject());
        DownlinkData downlinkData = AbstractDownlinkDataConverter.parseDownlinkData(result);

        Assert.assertEquals("JSON", downlinkData.getContentType());
        Assert.assertEquals(1, downlinkData.getMetadata().size());
        Assert.assertTrue(downlinkData.getMetadata().containsKey("topic"));
        Assert.assertEquals("sensor/upload", downlinkData.getMetadata().get("topic"));

        JsonNode dataJson = mapper.readTree(downlinkData.getData());

        Assert.assertTrue(dataJson.has("temperature"));
        Assert.assertEquals("33", dataJson.get("temperature").asText());

        Assert.assertTrue(dataJson.has("humidity"));
        Assert.assertEquals("78", dataJson.get("humidity").asText());

        Assert.assertTrue(dataJson.has("dewPoint"));
        Assert.assertEquals("28.65", dataJson.get("dewPoint").asText());

        eval.destroy();
    }

    private JSUplinkEvaluator createUplinkEvaluator(String scriptName) {
        InputStream src = JsConverterEvaluatorTest.class.getClassLoader().getResourceAsStream(scriptName);
        return new JSUplinkEvaluator(jsSandboxService, converterId, read(src));
    }

    private JSDownlinkEvaluator createDownlinkEvaluator(String scriptName) {
        InputStream src = JsConverterEvaluatorTest.class.getClassLoader().getResourceAsStream(scriptName);
        return new JSDownlinkEvaluator(jsSandboxService, converterId, read(src));
    }

    public static String read(InputStream input) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
