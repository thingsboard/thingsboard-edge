/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.data.UplinkContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 18.12.17.
 */
@Slf4j
public abstract class AbstractUplinkDataConverter extends AbstractDataConverter implements TBUplinkDataConverter {

    private static final String DEFAULT_DEVICE_TYPE = "default";

    public AbstractUplinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService) {
        super(jsInvokeService, tbelInvokeService);
    }

    @Override
    public void init(Converter configuration) {
        this.configuration = configuration;
    }

    @Override
    public ListenableFuture<List<UplinkData>> convertUplink(ConverterContext context, byte[] data, UplinkMetaData metadata,
                                                            ExecutorService callBackExecutorService) throws Exception {
        long startTime = System.currentTimeMillis();
        ListenableFuture<String> convertFuture = doConvertUplink(data, metadata);
        ListenableFuture<List<UplinkData>> result = Futures.transform(convertFuture, rawResult -> {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Uplink conversion took {} ms.", configuration.getId(), configuration.getName(), System.currentTimeMillis() - startTime);
            }
            JsonElement element = new JsonParser().parse(rawResult);
            List<UplinkData> resultList = new ArrayList<>();
            if (element.isJsonArray()) {
                for (JsonElement uplinkJson : element.getAsJsonArray()) {
                    resultList.add(parseUplinkData(uplinkJson.getAsJsonObject()));
                }
            } else if (element.isJsonObject()) {
                resultList.add(parseUplinkData(element.getAsJsonObject()));
            }
            if (configuration.isDebugMode()) {
                persistUplinkDebug(context, metadata.getContentType(), data, rawResult, metadata);
            }
            return resultList;
        }, callBackExecutorService);
        DonAsynchron.withCallback(result, r -> {
        }, t -> {
            if (t instanceof Exception) {
                if (configuration.isDebugMode()) {
                    persistUplinkDebug(context, metadata.getContentType(), data, metadata, (Exception) t);
                }
            } else {
                log.warn("[{}][{}] Unhandled exception: ", configuration.getId(), configuration.getName(), t);
            }
        }, callBackExecutorService);
        return result;
    }

    protected abstract ListenableFuture<String> doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception;

    protected UplinkData parseUplinkData(JsonObject src) {
        boolean isAsset = getIsAssetAndVerify(src);

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(isAsset);
        if (isAsset) {
            builder.assetName(src.get("assetName").getAsString());
            builder.assetType(src.get("assetType").getAsString());
            if (src.has("assetLabel")) {
                builder.assetLabel(src.get("assetLabel").getAsString());
            }
        } else {
            builder.deviceName(src.get("deviceName").getAsString());
            if (src.has("deviceType")) {
                builder.deviceType(src.get("deviceType").getAsString());
            } else {
                builder.deviceType(DEFAULT_DEVICE_TYPE);
            }
            if (src.has("deviceLabel")) {
                builder.deviceLabel(src.get("deviceLabel").getAsString());
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

    private void persistUplinkDebug(ConverterContext context, UplinkContentType inMessageType, byte[] inMessage, String outMessage, UplinkMetaData metadata) {
        try {
            persistDebug(context, getTypeUplink(inMessage), inMessageType.name(), inMessage, "JSON", outMessage.getBytes(StandardCharsets.UTF_8), metadataToJson(metadata), null);
        } catch (JsonProcessingException e) {
            log.warn("Failed to persist uplink debug message");
        }
    }

    private void persistUplinkDebug(ConverterContext context, UplinkContentType inMessageType, byte[] inMessage, UplinkMetaData metadata, Exception e) {
        try {
            persistDebug(context, "Uplink", inMessageType.name(), inMessage, null, null, metadataToJson(metadata), e);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to persist uplink debug message", ex);
        }
    }

    private String metadataToJson(UplinkMetaData metaData) throws JsonProcessingException {
        return mapper.writeValueAsString(metaData.getKvMap());
    }

    private String getTypeUplink(byte[] inMessage) throws JsonProcessingException {
        return (inMessage != null && inMessage.length > 23 && Arrays.equals(Arrays.copyOfRange(inMessage, 1, 23), mapper.writeValueAsBytes("DevEUI_downlink_Sent"))) ? "Downlink_Sent" : "Uplink";
    }
}
