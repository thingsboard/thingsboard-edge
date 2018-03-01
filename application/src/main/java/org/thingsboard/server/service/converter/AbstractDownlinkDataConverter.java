/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.service.integration.ConverterContext;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by ashvayka on 18.12.17.
 */
@Slf4j
public abstract class AbstractDownlinkDataConverter extends AbstractDataConverter implements TBDownlinkDataConverter {

    private Map<String, Set<String>> fetchAttributesMap = new HashMap<>();

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        JsonNode fetchAttributes = configuration.getConfiguration().get("fetchAttributes");
        for (String scope : DataConstants.allScopes()) {
            fetchAttributesMap.put(scope, parseAttributeKeys(scope, fetchAttributes));
        }
    }

    private Set<String> parseAttributeKeys(String scope, JsonNode fetchAttributes) {
        Set<String> attributeKeysList = new HashSet<>();
        JsonNode attributeKeys = fetchAttributes != null ? fetchAttributes.get(scope) : null;
        if (attributeKeys != null && attributeKeys.isArray()) {
            ArrayNode attributeKeysArray = (ArrayNode)attributeKeys;
            for (JsonNode keyJson : attributeKeysArray) {
                attributeKeysList.add(keyJson.asText());
            }
        }
        return attributeKeysList;
    }

    @Override
    public List<DownlinkData> convertDownLink(ConverterContext context, List<DownLinkMsg> downLinkMsgs, DownLinkMetaData metadata) throws Exception {
        List<String> rawPayloads = new ArrayList<>();
        for (DownLinkMsg downLinkMsg : downLinkMsgs) {
            fetchAttributes(context, downLinkMsg);
            String payload = mapper.writeValueAsString(downLinkMsg);
            rawPayloads.add(payload);
        }
        try {
            List<DownlinkData> result = new ArrayList<>();
            List<String> rawResults = new ArrayList<>();
            for (int i=0;i<downLinkMsgs.size();i++) {
                DownLinkMsg downLinkMsg = downLinkMsgs.get(i);
                String payload = rawPayloads.get(i);
                String rawResult = doConvertDownlink(payload, metadata);
                rawResults.add(rawResult);
                JsonElement element = new JsonParser().parse(rawResult);
                List<DownlinkData> downLinkResult = new ArrayList<>();
                if (element.isJsonArray()) {
                    for (JsonElement downlinkJson : element.getAsJsonArray()) {
                        result.add(parseDownlinkData(downlinkJson.getAsJsonObject(), downLinkMsg));
                    }
                } else if (element.isJsonObject()) {
                    result.add(parseDownlinkData(element.getAsJsonObject(), downLinkMsg));
                }
                result.addAll(downLinkResult);
            }
            if (configuration.isDebugMode()) {
                persistDownlinkDebug(context, rawPayloads, rawResults, metadata);
            }
            return result;
        } catch (Exception e) {
            if (configuration.isDebugMode()) {
                persistDownlinkDebug(context, rawPayloads, metadata, e);
            }
            throw e;
        }
    }

    private void fetchAttributes(ConverterContext context, DownLinkMsg downLinkMsg) throws Exception {
            DeviceId deviceId = downLinkMsg.getDeviceId();
            for (Map.Entry<String, Set<String>> attrScopeEntry : fetchAttributesMap.entrySet()) {
                if (!attrScopeEntry.getValue().isEmpty()) {
                    List<AttributeKvEntry> attributes = context.getAttributesService().find(deviceId, attrScopeEntry.getKey(), attrScopeEntry.getValue()).get();
                    for (AttributeKvEntry attribute : attributes) {
                        downLinkMsg.addCurrentAttribute(attrScopeEntry.getKey(), attribute.getKey(), attribute.getValueAsString());
                    }
                }
            }
    }

    protected abstract String doConvertDownlink(String payload, DownLinkMetaData metadata) throws Exception;

    public static DownlinkData parseDownlinkData(JsonObject src, DownLinkMsg sourceMsg) {
        if (!src.has("contentType")) {
            throw new JsonParseException("Downlink content type is not set!");
        } else if (!src.has("data")) {
            throw new JsonParseException("Downlink data is not set!");
        }
        String contentType = src.get("contentType").getAsString();
        String strData = src.get("data").getAsString();
        byte[] data;
        switch (contentType) {
            case "JSON":
            case "TEXT":
                data = strData.getBytes(StandardCharsets.UTF_8);
                break;
            case "BINARY":
                data = Base64Utils.decodeFromString(strData);
                break;
            default:
                throw new JsonParseException("Unknown downlink content type " + contentType);
        }
        Map<String, String> metadata = new HashMap<>();
        if (src.has("metadata")) {
            JsonElement metadataElement = src.get("metadata");
            if (!metadataElement.isJsonObject()) {
                throw new JsonParseException("Invalid downlink metadata format!");
            }
            JsonObject metadataObject = metadataElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> metadataEntry : metadataObject.entrySet()) {
                JsonElement metadataValue = metadataEntry.getValue();
                if (!metadataValue.isJsonPrimitive()) {
                    throw new JsonParseException("Invalid downlink metadata value format!");
                }
                metadata.put(metadataEntry.getKey(), metadataValue.getAsString());
            }
        }
        DownlinkData.DownlinkDataBuilder builder = DownlinkData.builder();
        builder.deviceId(sourceMsg.getDeviceId());
        builder.deviceName(sourceMsg.getDeviceName());
        builder.deviceType(sourceMsg.getDeviceType());
        builder.contentType(contentType);
        builder.data(data);
        builder.metadata(metadata);
        return builder.build();
    }

    private void persistDownlinkDebug(ConverterContext context, List<String> rawPayloads,
                                      List<String> rawResults, DownLinkMetaData metadata) {
        try {
            persistDebug(context, "Downlink", "JSON", stringListToJson(rawPayloads), "JSON", stringListToJson(rawResults), metadataToJson(metadata), null);
        } catch (IOException e) {
            log.warn("Failed to persist downlink debug message");
        }
    }

    private void persistDownlinkDebug(ConverterContext context, List<String> rawPayloads, DownLinkMetaData metadata, Exception e) {
        try {
            persistDebug(context, "Downlink", "JSON", stringListToJson(rawPayloads), null, null, metadataToJson(metadata), e);
        } catch (IOException ex) {
            log.warn("Failed to persist downlink debug message", ex);
        }
    }

    private byte[] stringListToJson(List<String> jsons) throws IOException {
        ArrayNode jsonArray = mapper.createArrayNode();
        for (String json : jsons) {
            jsonArray.add(mapper.readTree(json));
        }
        return mapper.writeValueAsString(jsonArray).getBytes(StandardCharsets.UTF_8);
    }

    private String metadataToJson(DownLinkMetaData metaData) throws JsonProcessingException {
        return mapper.writeValueAsString(metaData.getKvMap());
    }
}
