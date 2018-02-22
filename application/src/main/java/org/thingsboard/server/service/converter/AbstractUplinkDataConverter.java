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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.service.integration.ConverterContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 18.12.17.
 */
@Slf4j
public abstract class AbstractUplinkDataConverter extends AbstractDataConverter implements TBUplinkDataConverter {

    private final AtomicInteger telemetryIdSeq = new AtomicInteger();
    private final AtomicInteger attrRequestIdSeq = new AtomicInteger();


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
        if (!src.has("deviceName")) {
            throw new JsonParseException("Device name is not set!");
        } else if (!src.has("deviceType")) {
            throw new JsonParseException("Device type is not set!");
        }
        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.deviceName(src.get("deviceName").getAsString());
        builder.deviceType(src.get("deviceType").getAsString());
        if (src.has("telemetry")) {
            builder.telemetry(parseTelemetry(src.get("telemetry")));
        }
        if (src.has("attributes")) {
            builder.attributesUpdate(parseAttributesUpdate(src.get("attributes")));
        }

        //TODO: add support of attribute requests and client-side RPC.

        return builder.build();
    }

    private TelemetryUploadRequest parseTelemetry(JsonElement src) {
        return JsonConverter.convertToTelemetry(src, telemetryIdSeq.getAndIncrement());
    }

    private UpdateAttributesRequest parseAttributesUpdate(JsonElement src) {
        return JsonConverter.convertToAttributes(src, attrRequestIdSeq.getAndIncrement());
    }

    private void persistUplinkDebug(ConverterContext context, String inMessageType, byte[] inMessage,
                                    String outMessage, UplinkMetaData metadata) {
        try {
            persistDebug(context, "Uplink", inMessageType, inMessage, "JSON", outMessage.getBytes(StandardCharsets.UTF_8), metadataToJson(metadata), null);
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

    private void persistDebug(ConverterContext context, String type, String inMessageType, byte[] inMessage,
                              String outMessageType, byte[] outMessage, String metadata, Exception exception) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(DataConstants.DEBUG_CONVERTER);

        ObjectNode node = mapper.createObjectNode()
                .put("server", context.getDiscoveryService().getCurrentServer().getServerAddress().toString())
                .put("type", type)
                .put("inMessageType", inMessageType)
                .put("in", convertToString(inMessageType, inMessage))
                .put("outMessageType", outMessageType)
                .put("out", convertToString(outMessageType, outMessage))
                .put("metadata", metadata);

        if (exception != null) {
            node = node.put("error", toString(exception));
        }

        event.setBody(node);
        context.getEventService().save(event);
    }

    private String convertToString(String messageType, byte[] message) {
        if (message == null) {
            return null;
        }
        switch (messageType) {
            case "JSON":
            case "TEXT":
                return new String(message, StandardCharsets.UTF_8);
            case "BINARY":
                return Base64Utils.encodeToString(message);
            default:
                throw new RuntimeException("Message type: " + messageType + " is not supported!");
        }
    }

}
