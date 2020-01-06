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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ashvayka on 18.12.17.
 */
@Slf4j
public abstract class AbstractUplinkDataConverter extends AbstractDataConverter implements TBUplinkDataConverter {

    private static final String DEFAULT_DEVICE_TYPE = "default";

    @Override
    public void init(Converter configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<UplinkData> convertUplink(ConverterContext context, byte[] data, UplinkMetaData metadata) throws Exception {
        try {
            String rawResult = doConvertUplink(data, metadata);
            JsonElement element = new JsonParser().parse(rawResult);
            List<UplinkData> result = new ArrayList<>();
            if (element.isJsonArray()) {
                for (JsonElement uplinkJson : element.getAsJsonArray()) {
                    result.add(parseUplinkData(uplinkJson.getAsJsonObject()));
                }
            } else if (element.isJsonObject()) {
                result.add(parseUplinkData(element.getAsJsonObject()));
            }
            if (configuration.isDebugMode()) {
                persistUplinkDebug(context, metadata.getContentType(), data, rawResult, metadata);
            }
            return result;
        } catch (Exception e) {
            if (configuration.isDebugMode()) {
                persistUplinkDebug(context, metadata.getContentType(), data, metadata, e);
            }
            throw e;
        }
    }

    protected abstract String doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception;

    protected UplinkData parseUplinkData(JsonObject src) {
        boolean isAsset = getIsAssetAndVerify(src);

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(isAsset);
        if (isAsset) {
            builder.assetName(src.get("assetName").getAsString());
            builder.assetType(src.get("assetType").getAsString());
        } else {
            builder.deviceName(src.get("deviceName").getAsString());
            if (src.has("deviceType")) {
                builder.deviceType(src.get("deviceType").getAsString());
            } else {
                builder.deviceType(DEFAULT_DEVICE_TYPE);
            }
        }

        if (src.has("customerName")) {
            builder.customerName(src.get("customerName").getAsString());
        }
        if (src.has("groupName")) {
            builder.groupName(src.get("groupName").getAsString());
        }
        if (src.has("telemetry")) {
            builder.telemetry(parseTelemetry(src.get("telemetry")));
        }
        if (src.has("attributes")) {
            builder.attributesUpdate(parseAttributesUpdate(src.get("attributes")));
        }

        //TODO: add support of attribute requests and client-side RPC.
        return builder.build();
    }

    private boolean getIsAssetAndVerify(JsonObject src) {
        boolean isAsset;
        boolean isDeviceNamePresent = src.has("deviceName");
        boolean isAssetNamePresent = src.has("assetName");
        boolean isAssetTypePresent = src.has("assetType");

        if (!isDeviceNamePresent && !isAssetNamePresent) {
            throw new JsonParseException("Either 'deviceName' or 'assetName' should be present in the converter output!");
        }
        if (isDeviceNamePresent && isAssetNamePresent) {
            throw new JsonParseException("Both 'deviceName' and 'assetName' can't be present in the converter output!");
        }

        if (isDeviceNamePresent) {
            isAsset = false;
        } else {
            if (!isAssetTypePresent) {
                throw new JsonParseException("Asset type is not set!");
            }
            isAsset = true;
        }
        return isAsset;
    }

    private PostTelemetryMsg parseTelemetry(JsonElement src) {
        return JsonConverter.convertToTelemetryProto(src);
    }

    private PostAttributeMsg parseAttributesUpdate(JsonElement src) {
        return JsonConverter.convertToAttributesProto(src);
    }

    private void persistUplinkDebug(ConverterContext context, String inMessageType, byte[] inMessage, String outMessage, UplinkMetaData metadata) {
        try {
            persistDebug(context, getTypeUplink (inMessage), inMessageType, inMessage, "JSON", outMessage.getBytes(StandardCharsets.UTF_8), metadataToJson(metadata), null);
        } catch (JsonProcessingException e) {
            log.warn("Failed to persist uplink debug message");
        }
    }

    private void persistUplinkDebug(ConverterContext context, String inMessageType, byte[] inMessage, UplinkMetaData metadata, Exception e) {
        try {
            persistDebug(context, "Uplink", inMessageType, inMessage, null, null, metadataToJson(metadata), e);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to persist uplink debug message", ex);
        }
    }

    private String metadataToJson(UplinkMetaData metaData) throws JsonProcessingException {
        return mapper.writeValueAsString(metaData.getKvMap());
    }

    private String getTypeUplink (byte[] inMessage) throws JsonProcessingException {
        return (inMessage != null && inMessage.length >23 &&Arrays.equals(Arrays.copyOfRange(inMessage, 1, 23), mapper.writeValueAsBytes("DevEUI_downlink_Sent")))? "Downlink_Sent": "Uplink";
    }
}
