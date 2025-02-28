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
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.file.Files;
import java.nio.file.Path;
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

        assertArrayEquals(Hex.decodeHex("2A3F"), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

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

        ObjectNode decoded = JacksonUtil.newObjectNode();
        decoded.put("temperature", "42");
        decoded.put("humidity", "63");

        payloadMsg.set("object", decoded);
        expectedKvMap.put("decoded", JacksonUtil.toString(decoded));

        result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void thingsStackConverterWrapperTest() throws Exception {
        ConverterWrapper wrapper = ConverterWrapperFactory.getWrapper(IntegrationType.TTN).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("ThingsStackPayload.json"), ObjectNode.class);

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        Map<String, String> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("deviceId", "\"03022\"");
        expectedKvMap.put("applicationId", "\"test-decentlab\"");
        expectedKvMap.put("eui", "\"70B3D57BA0000BCE\"");
        expectedKvMap.put("joinEui", "\"70B3D57ED00006B2\"");
        expectedKvMap.put("devAddr", "\"27000020\"");
        expectedKvMap.put("correlationIds", "[\"as:up:01E0CY8V864TP36Q130RSQQJBY\"]");
        expectedKvMap.put("receivedAt", "\"2020-02-06T09:46:05.447941836Z\"");
        expectedKvMap.put("sessionKeyId", "\"AXAWYbtxgUllLtJWdZrW0Q==\"");
        expectedKvMap.put("fPort", "1");
        expectedKvMap.put("fCnt", "101");
        expectedKvMap.put("data", "\"AgI7AAMANwJxDGA=\"");
        expectedKvMap.put("rxMetadata", "[]");
        expectedKvMap.put("bandwidth", "125000");
        expectedKvMap.put("spreadingFactor", "11");
        expectedKvMap.put("dataRateIndex", "1");
        expectedKvMap.put("codingRate", "\"4/5\"");
        expectedKvMap.put("frequency", "\"867700000\"");
        expectedKvMap.put("timestamp", "436812492");
        expectedKvMap.put("time", "\"2020-02-06T09:46:05Z\"");
        expectedKvMap.put("uplinkMessageReceivedAt", "\"2020-02-06T09:46:05.234172599Z\"");

        TbPair<byte[], UplinkMetaData> result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("AgI7AAMANwJxDGA="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        ObjectNode decoded = JacksonUtil.newObjectNode();
        decoded.put("temperature", "42");
        decoded.put("humidity", "63");

        ((ObjectNode) payloadMsg.get("uplink_message")).set("decoded_payload", decoded);
        expectedKvMap.put("decoded", JacksonUtil.toString(decoded));

        result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void thingsParkConverterWrapperTest() throws Exception {
        ConverterWrapper wrapper = ConverterWrapperFactory.getWrapper(IntegrationType.THINGPARK).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("ThingsParkPayload.json"), ObjectNode.class);

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        Map<String, String> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("time", "\"2024-11-28T21:08:22.138+00:00\"");
        expectedKvMap.put("eui", "\"70B3D57BA000156B\"");
        expectedKvMap.put("fPort", "1");
        expectedKvMap.put("fCntUp", "26");
        expectedKvMap.put("lostUplinksAs", "0");
        expectedKvMap.put("adrBit", "1");
        expectedKvMap.put("mType", "2");
        expectedKvMap.put("fCntDn", "2");
        expectedKvMap.put("data", "\"02023b0003003702710c60\"");
        expectedKvMap.put("micHex", "\"e7214986\"");
        expectedKvMap.put("lrcid", "\"00000211\"");
        expectedKvMap.put("rssi", "-114.0");
        expectedKvMap.put("snr", "4.75");
        expectedKvMap.put("esp", "-115.2547");
        expectedKvMap.put("spreadingFactor", "9");
        expectedKvMap.put("bandwidth", "\"G0\"");
        expectedKvMap.put("channel", "\"LC1\"");
        expectedKvMap.put("lrrId", "\"100019D4\"");
        expectedKvMap.put("late", "0");
        expectedKvMap.put("latitude", "32.516357");
        expectedKvMap.put("longitude", "-106.824348");
        expectedKvMap.put("lrr", "[]");
        expectedKvMap.put("devLrrCnt", "1");
        expectedKvMap.put("customerId", "\"100045194\"");
        expectedKvMap.put("customerData", "{}");
        expectedKvMap.put("baseStationData", "{\"doms\":[],\"name\":\"iStation US #6_CDRRC_Summerford\"}");
        expectedKvMap.put("modelCfg", "\"0\"");
        expectedKvMap.put("driverCfg", "{}");
        expectedKvMap.put("instantPer", "0.0");
        expectedKvMap.put("meanPer", "0.037037");
        expectedKvMap.put("devAddr", "\"00FDA112\"");
        expectedKvMap.put("txPower", "18.0");
        expectedKvMap.put("nbTrans", "2");
        expectedKvMap.put("frequency", "902.5");
        expectedKvMap.put("dynamicClass", "\"A\"");

        TbPair<byte[], UplinkMetaData> result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("AgI7AAMANwJxDGA="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        ObjectNode decoded = JacksonUtil.newObjectNode();
        decoded.put("temperature", "42");
        decoded.put("humidity", "63");

        ((ObjectNode) payloadMsg.get("DevEUI_uplink")).set("payload", decoded);
        expectedKvMap.put("decoded", JacksonUtil.toString(decoded));

        result = wrapper.wrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    private String readPayloadFromFile(String fileName) throws Exception {
        var uri = this.getClass().getClassLoader().getResource(fileName).toURI();
        return Files.readString(Path.of(uri));

    }

}
