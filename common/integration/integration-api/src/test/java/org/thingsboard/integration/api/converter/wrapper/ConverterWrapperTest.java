/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConverterWrapperTest {

    @Test
    public void loriotConverterWrapperTest() throws Exception {
        ConverterWrapper wrapper = ConverterWrapperFactory.getWrapper(IntegrationType.LORIOT).get();

        ObjectNode payloadMsg = JacksonUtil.newObjectNode();
        payloadMsg.put("data", "2A3F");
        payloadMsg.put("rssi", "-130");
        payloadMsg.put("port", 80);
        payloadMsg.put("EUI", "BE7A123456789");

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, Map.of("integrationName", "Loriot integration"));

        Map<String, String> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("data", "\"2A3F\"");
        expectedKvMap.put("rssi", "\"-130\"");
        expectedKvMap.put("fPort", "80");
        expectedKvMap.put("eui", "\"BE7A123456789\"");

        TbPair<byte[], UplinkMetaData> result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals("2A3F".getBytes(), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.TEXT, result.getSecond().getContentType());

        ObjectNode decoded = JacksonUtil.newObjectNode();
        decoded.put("temperature", "42");
        decoded.put("humidity", "63");

        payloadMsg.set("decoded", decoded);
        expectedKvMap.put("decoded", JacksonUtil.toString(decoded));

        result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void chirpStackConverterWrapperTest() throws Exception {
        ConverterWrapper wrapper = ConverterWrapperFactory.getWrapper(IntegrationType.CHIRPSTACK).get();

        ObjectNode payloadMsg = JacksonUtil.newObjectNode();
        payloadMsg.put("data", "MkEzRg==");
        payloadMsg.put("fPort", 80);

        ObjectNode deviceInfoMsg = JacksonUtil.newObjectNode();
        deviceInfoMsg.put("deviceName", "Chirpstack");
        deviceInfoMsg.put("deviceProfileName", "default");
        deviceInfoMsg.put("devEui", "BE7A123456789");

        payloadMsg.set("deviceInfo", deviceInfoMsg);

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        Map<String, String> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("data", "\"MkEzRg==\"");
        expectedKvMap.put("fPort", "80");
        expectedKvMap.put("eui", "\"BE7A123456789\"");
        expectedKvMap.put("deviceName", "\"Chirpstack\"");
        expectedKvMap.put("deviceProfileName", "\"default\"");

        TbPair<byte[], UplinkMetaData> result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("MkEzRg=="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());
    }

}
