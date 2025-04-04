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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;

public class ThingsStackConverterUnwrapper extends AbstractConverterUnwrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("deviceId", "/end_device_ids/device_id")
                .put("applicationId", "/end_device_ids/application_ids/application_id")
                .put("eui", "/end_device_ids/dev_eui")
                .put("joinEui", "/end_device_ids/join_eui")
                .put("devAddr", "/end_device_ids/dev_addr")
                .put("correlationIds", "/correlation_ids")
                .put("receivedAt", "/received_at")
                .put("sessionKeyId", "/uplink_message/session_key_id")
                .put("fPort", "/uplink_message/f_port")
                .put("fCnt", "/uplink_message/f_cnt")
                .put("data", "/uplink_message/frm_payload")
                .put("decoded", "/uplink_message/decoded_payload")
                .put("rxMetadata", "/uplink_message/rx_metadata")
                .put("bandwidth", "/uplink_message/settings/data_rate/lora/bandwidth")
                .put("spreadingFactor", "/uplink_message/settings/data_rate/lora/spreading_factor")
                .put("dataRateIndex", "/uplink_message/settings/data_rate_index")
                .put("codeRate", "/uplink_message/settings/coding_rate")
                .put("frequency", "/uplink_message/settings/frequency")
                .put("timestamp", "/uplink_message/settings/timestamp")
                .put("time", "/uplink_message/settings/time")
                .put("consumedAirtime", "/uplink_message/consumed_airtime")
                .put("latitude", "/uplink_message/locations/user/latitude")
                .put("longitude", "/uplink_message/locations/user/longitude")
                .put("altitude", "/uplink_message/locations/user/altitude")
                .put("source", "/uplink_message/locations/user/source")
                .put("brandId", "/uplink_message/version_ids/brand_id")
                .put("modelId", "/uplink_message/version_ids/model_id")
                .put("hardwareVersion", "/uplink_message/version_ids/hardware_version")
                .put("firmwareVersion", "/uplink_message/version_ids/firmware_version")
                .put("bandId", "/uplink_message/version_ids/band_id")
                .put("netId", "/uplink_message/network_ids/net_id")
                .put("tenantId", "/uplink_message/network_ids/tenant_id")
                .put("clusterId", "/uplink_message/network_ids/cluster_id")
                .put("attributes", "/uplink_message/attributes")
                .put("uplinkMessageReceivedAt", "/uplink_message/received_at")
                .put("simulated", "/simulated")
                .put("rssi", "")
                .put("snr", "")
                .build();
    }

    @Override
    protected String getGatewayInfoPath() {
        return "/uplink_message/rx_metadata";
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        var uplink = payloadJson.get("uplink_message");
        if (uplink.has("decoded_payload")) {
            var decoded = uplink.get("decoded_payload");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else {
            var data = uplink.get("frm_payload").textValue();
            return TbPair.of(Base64.getDecoder().decode(data), ContentType.BINARY);
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
