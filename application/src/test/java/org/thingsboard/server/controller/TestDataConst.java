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
package org.thingsboard.server.controller;

public class TestDataConst {
    public static final String TTI_JSON_PAYLOAD_EXAMPLE = "{\n" +
            "    \"end_device_ids\": {\n" +
            "        \"device_id\": \"eui-1000000000000001\",\n" +
            "        \"application_ids\": {\n" +
            "            \"application_id\": \"application-tts-name\"\n" +
            "        },\n" +
            "        \"dev_eui\": \"1000000000000001\",\n" +
            "        \"join_eui\": \"2000000000000001\",\n" +
            "        \"dev_addr\": \"20000001\"\n" +
            "    },\n" +
            "    \"correlation_ids\": [\"as:up:01H0S7ZJQ9MQPMVY49FT3SE07M\", \"gs:conn:01H03BQZ9342X3Y86DJ2P704E5\", \"gs:up:host:01H03BQZ99EGAM52KK1300GFKN\", \"gs:uplink:01H0S7ZJGS6D9TJSKJN8XNTMAV\", \"ns:uplink:01H0S7ZJGS9KKD4HTTPKFEMWCV\", \"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0S7ZJGSF3M38ZRZVTM38DEC\", \"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0S7ZJQ8R2EH5AA269AKM8DX\"],\n" +
            "    \"received_at\": \"2023-05-19T05:33:35.848446463Z\",\n" +
            "    \"uplink_message\": {\n" +
            "        \"session_key_id\": \"AYfqmb0pc/1uRZv9xUydgQ==\",\n" +
            "        \"f_port\": 85,\n" +
            "        \"f_cnt\": 10335,\n" +
            "        \"frm_payload\": \"AXVeAwABBAAB\",\n" +
            "        \"rx_metadata\": [{\n" +
            "            \"gateway_ids\": {\n" +
            "                \"gateway_id\": \"eui-6a7e111a10000000\",\n" +
            "                \"eui\": \"6A7E111A10000000\"\n" +
            "            },\n" +
            "            \"time\": \"2023-05-19T05:33:35.608982Z\",\n" +
            "            \"timestamp\": 3893546133,\n" +
            "            \"rssi\": -35,\n" +
            "            \"channel_rssi\": -35,\n" +
            "            \"snr\": 13.2,\n" +
            "            \"frequency_offset\": \"69\",\n" +
            "            \"uplink_token\": \"CiIKIAoUZXVpLTZhN2UxMTFhMTAwMDAwMDASCCThJP/+9k6eEJWZy8AOGgwIr5ScowYQvNbUsQIgiMy8y6jwpwE=\",\n" +
            "            \"channel_index\": 3,\n" +
            "            \"received_at\": \"2023-05-19T05:33:35.607383681Z\"\n" +
            "        }],\n" +
            "        \"settings\": {\n" +
            "            \"data_rate\": {\n" +
            "                \"lora\": {\n" +
            "                    \"bandwidth\": 125000,\n" +
            "                    \"spreading_factor\": 7,\n" +
            "                    \"coding_rate\": \"4/5\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"frequency\": \"867100000\",\n" +
            "            \"timestamp\": 3893546133,\n" +
            "            \"time\": \"2023-05-19T05:33:35.608982Z\"\n" +
            "        },\n" +
            "        \"received_at\": \"2023-05-19T05:33:35.641841782Z\",\n" +
            "        \"consumed_airtime\": \"0.056576s\",\n" +
            "        \"network_ids\": {\n" +
            "            \"net_id\": \"000013\",\n" +
            "            \"tenant_id\": \"ttn\",\n" +
            "            \"cluster_id\": \"eu1\",\n" +
            "            \"cluster_address\": \"eu1.cloud.thethings.network\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    public static final String TTN_JSON_PAYLOAD_EXAMPLE = "{\n" +
            "    \"end_device_ids\": {\n" +
            "        \"device_id\": \"eui-1000000000000001\",\n" +
            "        \"application_ids\": {\n" +
            "            \"application_id\": \"application-tts-name\"\n" +
            "        },\n" +
            "        \"dev_eui\": \"1000000000000001\",\n" +
            "        \"join_eui\": \"2000000000000001\",\n" +
            "        \"dev_addr\": \"20000001\"\n" +
            "    },\n" +
            "    \"correlation_ids\": [\"as:up:01H0S7ZJQ9MQPMVY49FT3SE07M\", \"gs:conn:01H03BQZ9342X3Y86DJ2P704E5\", \"gs:up:host:01H03BQZ99EGAM52KK1300GFKN\", \"gs:uplink:01H0S7ZJGS6D9TJSKJN8XNTMAV\", \"ns:uplink:01H0S7ZJGS9KKD4HTTPKFEMWCV\", \"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0S7ZJGSF3M38ZRZVTM38DEC\", \"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0S7ZJQ8R2EH5AA269AKM8DX\"],\n" +
            "    \"received_at\": \"2023-05-19T05:33:35.848446463Z\",\n" +
            "    \"uplink_message\": {\n" +
            "        \"session_key_id\": \"AYfqmb0pc/1uRZv9xUydgQ==\",\n" +
            "        \"f_port\": 85,\n" +
            "        \"f_cnt\": 10335,\n" +
            "        \"frm_payload\": \"AXVeAwABBAAB\",\n" +
            "        \"rx_metadata\": [{\n" +
            "            \"gateway_ids\": {\n" +
            "                \"gateway_id\": \"eui-6a7e111a10000000\",\n" +
            "                \"eui\": \"6A7E111A10000000\"\n" +
            "            },\n" +
            "            \"time\": \"2023-05-19T05:33:35.608982Z\",\n" +
            "            \"timestamp\": 3893546133,\n" +
            "            \"rssi\": -35,\n" +
            "            \"channel_rssi\": -35,\n" +
            "            \"snr\": 13.2,\n" +
            "            \"frequency_offset\": \"69\",\n" +
            "            \"uplink_token\": \"CiIKIAoUZXVpLTZhN2UxMTFhMTAwMDAwMDASCCThJP/+9k6eEJWZy8AOGgwIr5ScowYQvNbUsQIgiMy8y6jwpwE=\",\n" +
            "            \"channel_index\": 3,\n" +
            "            \"received_at\": \"2023-05-19T05:33:35.607383681Z\"\n" +
            "        }],\n" +
            "        \"settings\": {\n" +
            "            \"data_rate\": {\n" +
            "                \"lora\": {\n" +
            "                    \"bandwidth\": 125000,\n" +
            "                    \"spreading_factor\": 7,\n" +
            "                    \"coding_rate\": \"4/5\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"frequency\": \"867100000\",\n" +
            "            \"timestamp\": 3893546133,\n" +
            "            \"time\": \"2023-05-19T05:33:35.608982Z\"\n" +
            "        },\n" +
            "        \"received_at\": \"2023-05-19T05:33:35.641841782Z\",\n" +
            "        \"consumed_airtime\": \"0.056576s\",\n" +
            "        \"network_ids\": {\n" +
            "            \"net_id\": \"000013\",\n" +
            "            \"tenant_id\": \"ttn\",\n" +
            "            \"cluster_id\": \"eu1\",\n" +
            "            \"cluster_address\": \"eu1.cloud.thethings.network\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
    public static final String CHIRDSTACK_JSON_PAYLOAD_EXAMPLE = "{\n" +
            "    \"deduplicationId\": \"57433366-50a6-4dc2-8145-2df1bbc70d9e\",\n" +
            "    \"time\": \"2023-05-22T07:47:05.404859+00:00\",\n" +
            "    \"deviceInfo\": {\n" +
            "        \"tenantId\": \"52f14cd4-c6f1-4fbd-8f87-4025e1d49242\",\n" +
            "        \"tenantName\": \"ChirpStack\",\n" +
            "        \"applicationId\": \"ca739e26-7b67-4f14-b69e-d568c22a5a75\",\n" +
            "        \"applicationName\": \"Chirpstack application\",\n" +
            "        \"deviceProfileId\": \"605d08d4-65f5-4d2c-8a5a-3d2457662f79\",\n" +
            "        \"deviceProfileName\": \"Chirpstack default device profile\",\n" +
            "        \"deviceName\": \"Device name\",\n" +
            "        \"devEui\": \"1000000000000001\",\n" +
            "        \"tags\": {}\n" +
            "    },\n" +
            "    \"devAddr\": \"20000001\",\n" +
            "    \"adr\": true,\n" +
            "    \"dr\": 5,\n" +
            "    \"fCnt\": 4,\n" +
            "    \"fPort\": 85,\n" +
            "    \"confirmed\": false,\n" +
            "    \"data\": \"AXVdAwABBAAA\",\n" +
            "    \"rxInfo\": [{\n" +
            "        \"gatewayId\": \"6a7e111a10000000\",\n" +
            "        \"uplinkId\": 24022,\n" +
            "        \"time\": \"2023-05-22T07:47:05.404859+00:00\",\n" +
            "        \"rssi\": -35,\n" +
            "        \"snr\": 11.5,\n" +
            "        \"channel\": 2,\n" +
            "        \"rfChain\": 1,\n" +
            "        \"location\": {},\n" +
            "        \"context\": \"EFwMtA==\",\n" +
            "        \"metadata\": {\n" +
            "            \"region_common_name\": \"EU868\",\n" +
            "            \"region_config_id\": \"eu868\"\n" +
            "        },\n" +
            "        \"crcStatus\": \"CRC_OK\"\n" +
            "    }],\n" +
            "    \"txInfo\": {\n" +
            "        \"frequency\": 868500000,\n" +
            "        \"modulation\": {\n" +
            "            \"lora\": {\n" +
            "                \"bandwidth\": 125000,\n" +
            "                \"spreadingFactor\": 7,\n" +
            "                \"codeRate\": \"CR_4_5\"\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
    public static final String LORIOT_PAYLOAD_EXAMPLE = "{\n" +
            "    \"cmd\": \"rx\",\n" +
            "    \"seqno\": 3040,\n" +
            "    \"EUI\": \"1000000000000001\",\n" +
            "    \"ts\": 1684478801936,\n" +
            "    \"fcnt\": 2,\n" +
            "    \"port\": 85,\n" +
            "    \"freq\": 867500000,\n" +
            "    \"rssi\": -21,\n" +
            "    \"snr\": 10,\n" +
            "    \"toa\": 206,\n" +
            "    \"dr\": \"SF9 BW125 4/5\",\n" +
            "    \"ack\": false,\n" +
            "    \"bat\": 94,\n" +
            "    \"offline\": false,\n" +
            "    \"data\": \"01755e030001040001\"\n" +
            "}";

    public static final String AWS_IOT_PAYLOAD_EXAMPLE = "{\n" +
            "    \"device_id\": \"3G7H1j-9zF\",\n" +
            "    \"timestamp\": \"2023-06-10T12:00:00Z\",\n" +
            "    \"sensor_data\": {\n" +
            "        \"temperature\": 25.3,\n" +
            "        \"humidity\": 62.8,\n" +
            "        \"pressure\": 1012.5\n" +
            "    },\n" +
            "    \"location\": {\n" +
            "        \"latitude\": 37.7749,\n" +
            "        \"longitude\": -122.4194\n" +
            "    },\n" +
            "    \"status\": \"active\",\n" +
            "    \"power_status\": \"on\",\n" +
            "    \"vibration\": {\n" +
            "        \"x\": 0.02,\n" +
            "        \"y\": 0.03,\n" +
            "        \"z\": 0.01\n" +
            "    },\n" +
            "    \"fault_codes\": [100, 204, 301],\n" +
            "    \"battery_level\": 78.5\n" +
            "}";
    public static final String AZURE_PAYLOAD_EXAMPLE = "{\n" +
            "    \"deviceId\": \"8F4A2C6D\",\n" +
            "    \"deviceType\": \"Packing machine\",\n" +
            "    \"temperature\": 25.5,\n" +
            "    \"pressure\": 1013.25,\n" +
            "    \"vibration\": {\n" +
            "        \"x\": 0.02,\n" +
            "        \"y\": 0.03,\n" +
            "        \"z\": 0.015\n" +
            "    },\n" +
            "    \"location\": {\n" +
            "        \"latitude\": 37.7749,\n" +
            "        \"longitude\": -122.4194,\n" +
            "        \"altitude\": 10\n" +
            "    },\n" +
            "    \"timestamp\": \"2023-06-09T10:30:00Z\",\n" +
            "    \"status\": \"ALARM\",\n" +
            "    \"alarms\": [{\n" +
            "            \"type\": \"temperature\",\n" +
            "            \"severity\": \"high\",\n" +
            "            \"message\": \"Temperature exceeds threshold.\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"vibration\",\n" +
            "            \"severity\": \"critical\",\n" +
            "            \"message\": \"Excessive vibration detected.\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"metadata\": {\n" +
            "        \"version\": 1,\n" +
            "        \"batteryLevel\": 100,\n" +
            "        \"batteryStatus\": \"Charging\",\n" +
            "        \"manufacturer\": \"Example corporation\"\n" +
            "    }\n" +
            "}";
    public static final String SIGFOX_PAYLOAD_EXAMPLE = "{\n" +
            "    \"device\": \"2203961\",\n" +
            "    \"time\": \"1686298419\",\n" +
            "    \"data\": \"2502af2102462a\",\n" +
            "    \"seqNumber\": \"570\",\n" +
            "    \"deviceTypeId\": \"630ceaea10d051194ec0246e\",\n" +
            "    \"ack\": \"false\",\n" +
            "    \"customData#int1\": \"37\",\n" +
            "    \"customData#int2\": \"2\"\n" +
            "}";

    public static final String TTI_JSON_DECODED = "{\"deviceName\":\"eui-1000000000000001\",\"deviceType\":\"application-tts-name\",\"groupName\":\"IAQ devices\",\"attributes\":{\"devEui\":\"1000000000000001\",\"fPort\":85,\"correlation_ids\":[\"as:up:01H0S7ZJQ9MQPMVY49FT3SE07M\",\"gs:conn:01H03BQZ9342X3Y86DJ2P704E5\",\"gs:up:host:01H03BQZ99EGAM52KK1300GFKN\",\"gs:uplink:01H0S7ZJGS6D9TJSKJN8XNTMAV\",\"ns:uplink:01H0S7ZJGS9KKD4HTTPKFEMWCV\",\"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0S7ZJGSF3M38ZRZVTM38DEC\",\"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0S7ZJQ8R2EH5AA269AKM8DX\"],\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\",\"frequency\":\"867100000\",\"net_id\":\"000013\",\"tenant_id\":\"ttn\",\"cluster_id\":\"eu1\",\"cluster_address\":\"eu1.cloud.thethings.network\",\"device_id\":\"eui-1000000000000001\",\"application_id\":\"application-tts-name\",\"join_eui\":\"2000000000000001\",\"dev_addr\":\"20000001\"},\"telemetry\":{\"ts\":1684474415641,\"values\":{\"HEX_bytes\":\"01755E030001040001\",\"session_key_id\":\"AYfqmb0pc/1uRZv9xUydgQ==\",\"f_cnt\":10335,\"frm_payload\":\"AXVeAwABBAAB\",\"eui\":\"6A7E111A10000000\",\"rssi\":-35,\"channel_rssi\":-35,\"snr\":13.2,\"frequency_offset\":\"69\",\"channel_index\":3,\"consumed_airtime\":\"0.056576s\"}}}";
    public static final String TTN_JSON_DECODED = "{\"deviceName\":\"eui-1000000000000001\",\"deviceType\":\"application-tts-name\",\"groupName\":\"IAQ devices\",\"attributes\":{\"devEui\":\"1000000000000001\",\"fPort\":85,\"correlation_ids\":[\"as:up:01H0S7ZJQ9MQPMVY49FT3SE07M\",\"gs:conn:01H03BQZ9342X3Y86DJ2P704E5\",\"gs:up:host:01H03BQZ99EGAM52KK1300GFKN\",\"gs:uplink:01H0S7ZJGS6D9TJSKJN8XNTMAV\",\"ns:uplink:01H0S7ZJGS9KKD4HTTPKFEMWCV\",\"rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01H0S7ZJGSF3M38ZRZVTM38DEC\",\"rpc:/ttn.lorawan.v3.NsAs/HandleUplink:01H0S7ZJQ8R2EH5AA269AKM8DX\"],\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\",\"frequency\":\"867100000\",\"net_id\":\"000013\",\"tenant_id\":\"ttn\",\"cluster_id\":\"eu1\",\"cluster_address\":\"eu1.cloud.thethings.network\",\"device_id\":\"eui-1000000000000001\",\"application_id\":\"application-tts-name\",\"join_eui\":\"2000000000000001\",\"dev_addr\":\"20000001\"},\"telemetry\":{\"ts\":1684474415641,\"values\":{\"HEX_bytes\":\"01755E030001040001\",\"session_key_id\":\"AYfqmb0pc/1uRZv9xUydgQ==\",\"f_cnt\":10335,\"frm_payload\":\"AXVeAwABBAAB\",\"eui\":\"6A7E111A10000000\",\"rssi\":-35,\"channel_rssi\":-35,\"snr\":13.2,\"frequency_offset\":\"69\",\"channel_index\":3,\"consumed_airtime\":\"0.056576s\"}}}";

    public static final String CHIRPSTACK_JSON_DECODED = "{\"deviceName\":\"Device name\",\"deviceType\":\"Chirpstack default device profile\",\"groupName\":\"IAQ devices\",\"attributes\":{\"deduplicationId\":\"57433366-50a6-4dc2-8145-2df1bbc70d9e\",\"tenantId\":\"52f14cd4-c6f1-4fbd-8f87-4025e1d49242\",\"tenantName\":\"ChirpStack\",\"applicationId\":\"ca739e26-7b67-4f14-b69e-d568c22a5a75\",\"applicationName\":\"Chirpstack application\",\"deviceProfileId\":\"605d08d4-65f5-4d2c-8a5a-3d2457662f79\",\"deviceProfileName\":\"Chirpstack default device profile\",\"devEui\":\"1000000000000001\",\"devAddr\":\"20000001\",\"fPort\":85,\"frequency\":868500000,\"bandwidth\":125000,\"spreadingFactor\":7,\"codeRate\":\"CR_4_5\"},\"telemetry\":{\"ts\":1684741625404,\"values\":{\"HEX_bytes\":\"01755D030001040000\",\"dr\":5,\"fCnt\":4,\"confirmed\":false,\"gatewayId\":\"6a7e111a10000000\",\"uplinkId\":24022,\"rssi\":-35,\"snr\":11.5,\"channel\":2,\"rfChain\":1,\"context\":\"EFwMtA==\",\"crcStatus\":\"CRC_OK\"}}}";

    public static final String LORIOT_JSON_DECODED = "[{\"deviceName\":\"1000000000000001\",\"deviceType\":\"LoraDevices\",\"groupName\":\"IAQ devices\",\"attributes\":{\"fPort\":85,\"dataRange\":\"SF9 BW125 4/5\",\"freq\":867500000,\"offline\":false},\"telemetry\":{\"ts\":1684478801936,\"values\":{\"HEX_bytes\":\"01755E030001040001\",\"seqno\":3040,\"fcnt\":2,\"rssi\":-21,\"snr\":10,\"toa\":206,\"ack\":false,\"bat\":94}}}]";

    public static final String AWS_IOT_JSON_DECODED = "{\"deviceName\":\"Production 1 - 3G7H1j-9zF\",\"deviceType\":\"default\",\"groupName\":\"Production\",\"attributes\":{\"deviceId\":\"3G7H1j-9zF\"},\"telemetry\":{\"ts\":1686398400000,\"values\":{\"temperature\":25.3,\"humidity\":62.8,\"pressure\":1012.5,\"latitude\":37.7749,\"longitude\":-122.4194,\"status\":\"active\",\"power_status\":\"on\",\"x\":0.02,\"y\":0.03,\"z\":0.01,\"fault_codes.0\":100,\"fault_codes.1\":204,\"fault_codes.2\":301,\"battery_level\":78.5}}}";

    public static final String AZURE_JSON_DECODED = "{\"deviceName\":\"8F4A2C6D\",\"deviceType\":\"Packing machine\",\"groupName\":\"Control room\",\"attributes\":{\"version\":1,\"manufacturer\":\"Example corporation\"},\"telemetry\":{\"ts\":1686306600000,\"values\":{\"receivedAlarms\":[{\"type\":\"temperature\",\"severity\":\"high\",\"message\":\"Temperature exceeds threshold.\"},{\"type\":\"vibration\",\"severity\":\"critical\",\"message\":\"Excessive vibration detected.\"}],\"temperature\":25.5,\"pressure\":1013.25,\"x\":0.02,\"y\":0.03,\"z\":0.015,\"status\":\"ALARM\",\"batteryLevel\":100,\"batteryStatus\":\"Charging\"}}}";

    public static final String SIGFOX_JSON_DECODED = "{\"deviceName\":\"Sigfox-2203961\",\"deviceType\":\"Sigfox device\",\"groupName\":\"Control room devices\",\"attributes\":{\"sigfoxId\":\"2203961\",\"deviceTypeId\":\"630ceaea10d051194ec0246e\",\"autoCalibration\":\"on\",\"zeroPointAdjusted\":false,\"transmitPower\":\"full\",\"powerControl\":\"off\",\"fwVersion\":2},\"telemetry\":{\"ts\":\"1686298419000\",\"values\":{\"temperature\":28.7,\"humidity\":33,\"co2\":582,\"co2Baseline\":420,\"customData1\":\"37\",\"customData2\":\"2\"}}}";
}
