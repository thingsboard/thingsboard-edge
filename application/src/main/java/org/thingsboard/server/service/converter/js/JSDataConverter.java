/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.server.service.converter.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.extensions.core.filter.NashornJsFilterEvaluator;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 02.12.17.
 */
public class JSDataConverter implements ThingsboardDataConverter {


    private static final String JS_WRAPPER_PREFIX = "function decodeInternal(bytes, metadata) {\n" +
            "    var payload = [];\n" +
            "    for (var i = 0; i < bytes.length; i++) {\n" +
            "        payload.push(bytes[i]);\n" +
            "    }\n" +
            "    return JSON.stringify(Decoder(payload, metadata));\n" +
            "}\n" +
            "\n" +
            "function Decoder(payload, metadata) {\n";

    private static final String JS_WRAPPER_SUFIX = "\n}";


    private final AtomicInteger telemetryIdSeq = new AtomicInteger();
    private final AtomicInteger attrRequestIdSeq = new AtomicInteger();

    protected NashornJsConverterEvaluator uplinkEvaluator;

    @Override
    public void init(Converter configuration) {
        uplinkEvaluator = new NashornJsConverterEvaluator(
                JS_WRAPPER_PREFIX
                        + configuration.getConfiguration().get("decoder").asText()
                        + JS_WRAPPER_SUFIX);
    }

    @Override
    public void update(Converter configuration) {
        destroy();
        init(configuration);
    }

    @Override
    public void destroy() {
        if (uplinkEvaluator != null) {
            uplinkEvaluator.destroy();
        }
    }

    @Override
    public List<UplinkData> convertUplink(byte[] data, UplinkMetaData metadata) throws Exception {
        List<UplinkData> result = new ArrayList<>();
        String src = applyJsFunction(data, metadata);
        JsonElement element = new JsonParser().parse(src);
        if (element.isJsonArray()) {
            for (JsonElement uplinkJson : element.getAsJsonArray()) {
                result.add(parseUplinkData(uplinkJson.getAsJsonObject()));
            }
        } else if (element.isJsonObject()) {
            result.add(parseUplinkData(element.getAsJsonObject()));
        }

        return result;
    }

    private UplinkData parseUplinkData(JsonObject src) {
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

    private String applyJsFunction(byte[] data, UplinkMetaData metadata) throws Exception {
        return uplinkEvaluator.execute(data, metadata);
    }
}
