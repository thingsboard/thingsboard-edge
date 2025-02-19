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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.binary.Hex;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

public class ThingParkConverterWrapper extends AbstractConverterWrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("time", "/DevEUI_uplink/Time")
                .put("eui", "DevEUI_uplink/DevEUI")
                .put("fPort", "/DevEUI_uplink/FPort")
                .put("fCntUp", "/DevEUI_uplink/FCntUp")
                .put("lostUplinksAs", "/DevEUI_uplink/LostUplinksAS")
                .put("aDrBit", "/DevEUI_uplink/ADRbit")
                .put("mType", "/DevEUI_uplink/MType")
                .put("fCntDn", "/DevEUI_uplink/FCntDn")
                .put("payloadHex", "/DevEUI_uplink/payload_hex")
                .put("micHex", "/DevEUI_uplink/mic_hex")
                .put("lrcid", "/DevEUI_uplink/Lrcid")
                .put("lrrRssi", "/DevEUI_uplink/LrrRSSI")
                .put("lrrSnr", "/DevEUI_uplink/LrrSNR")
                .put("lrrEsp", "/DevEUI_uplink/LrrESP")
                .put("spFact", "/DevEUI_uplink/SpFact")
                .put("subBand", "/DevEUI_uplink/SubBand")
                .put("channel", "/DevEUI_uplink/Channel")
                .put("lrrId", "/DevEUI_uplink/Lrrid")
                .put("late", "/DevEUI_uplink/Late")
                .put("lrrLat", "/DevEUI_uplink/LrrLAT")
                .put("lrrLon", "/DevEUI_uplink/LrrLON")
                .put("lrr", "/DevEUI_uplink/Lrrs/Lrr")
                .put("devLrrCnt", "/DevLrrCnt")
                .put("customerId", "/CustomerID")
                .put("customerData", "/CustomerData")
                .put("baseStationData", "/BaseStationData")
                .put("modelCfg", "/ModelCfg")
                .put("driverCfg", "/DriverCfg")
                .put("instantPer", "/InstantPER")
                .put("meanPer", "/MeanPER")
                .put("devAddr", "/DevAddr")
                .put("txPower", "/TxPower")
                .put("nbTrans", "/NbTrans")
                .put("frequency", "/Frequency")
                .put("dynamicClass", "/DynamicClass")
                .build();
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws Exception {
        var data = payloadJson.get("payload_hex").textValue();
        return TbPair.of(Hex.decodeHex(data.toCharArray()), ContentType.BINARY);
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
