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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

public class ThingParkConverterWrapper extends AbstractConverterWrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("time", "/DevEUI_uplink/Time")
                .put("eui", "/DevEUI_uplink/DevEUI")
                .put("fPort", "/DevEUI_uplink/FPort")
                .put("fCnt", "/DevEUI_uplink/FCntUp")
                .put("lostUplinksAs", "/DevEUI_uplink/LostUplinksAS")
                .put("adr", "/DevEUI_uplink/ADRbit")
                .put("mType", "/DevEUI_uplink/MType")
                .put("fCntDn", "/DevEUI_uplink/FCntDn")
                .put("data", "/DevEUI_uplink/payload_hex")
                .put("micHex", "/DevEUI_uplink/mic_hex")
                .put("lrcid", "/DevEUI_uplink/Lrcid")
                .put("rssi", "/DevEUI_uplink/LrrRSSI")
                .put("snr", "/DevEUI_uplink/LrrSNR")
                .put("esp", "/DevEUI_uplink/LrrESP")
                .put("spreadingFactor", "/DevEUI_uplink/SpFact")
                .put("bandwidth", "/DevEUI_uplink/SubBand")
                .put("channel", "/DevEUI_uplink/Channel")
                .put("lrrId", "/DevEUI_uplink/Lrrid")
                .put("late", "/DevEUI_uplink/Late")
                .put("latitude", "/DevEUI_uplink/LrrLAT")
                .put("longitude", "/DevEUI_uplink/LrrLON")
                .put("lrr", "/DevEUI_uplink/Lrrs/Lrr")
                .put("devLrrCnt", "/DevEUI_uplink/DevLrrCnt")
                .put("customerId", "/DevEUI_uplink/CustomerID")
                .put("customerData", "/DevEUI_uplink/CustomerData")
                .put("baseStationData", "/DevEUI_uplink/BaseStationData")
                .put("modelCfg", "/DevEUI_uplink/ModelCfg")
                .put("driverCfg", "/DevEUI_uplink/DriverCfg")
                .put("instantPer", "/DevEUI_uplink/InstantPER")
                .put("meanPer", "/DevEUI_uplink/MeanPER")
                .put("devAddr", "/DevEUI_uplink/DevAddr")
                .put("ackRequested", "/DevEUI_uplink/AckRequested")
                .put("rawMacCommands", "/DevEUI_uplink/rawMacCommands")
                .put("txPower", "/DevEUI_uplink/TxPower")
                .put("nbTrans", "/DevEUI_uplink/NbTrans")
                .put("frequency", "/DevEUI_uplink/Frequency")
                .put("dynamicClass", "/DevEUI_uplink/DynamicClass")
                .put("payloadEncryption", "/DevEUI_uplink/PayloadEncryption")
                .put("decoded", "/DevEUI_uplink/payload")
                .put("points", "/DevEUI_uplink/points")
                .put("downlinkUrl", "/DevEUI_uplink/downlinkUrl")
                .build();
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws DecoderException {
        var uplink = payloadJson.get("DevEUI_uplink");
        if (uplink.has("payload")) {
            var decoded = uplink.get("payload");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else {
            var data = uplink.get("payload_hex").textValue();
            return TbPair.of(Hex.decodeHex(data.toCharArray()), ContentType.BINARY);
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
