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
package org.thingsboard.integration.api.converter;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.wrapper.ConverterWrapper;
import org.thingsboard.integration.api.converter.wrapper.ConverterWrapperFactory;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class DedicatedScriptUplinkDataConverter extends AbstractUplinkDataConverter {

    public static final String TELEMETRY = "telemetry";
    public static final String VALUES = "values";
    public static final String TS = "ts";
    public static final String ATTRIBUTES = "attributes";
    public static final String DEFAULT_PROFILE = "default";

    private ScriptUplinkEvaluator evaluator;
    private DedicatedConverterConfig config;
    private ConverterWrapper converterWrapper;
    private Gson gson;

    public DedicatedScriptUplinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
        this.gson = new Gson();
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        this.config = JacksonUtil.treeToValue(configuration.getConfiguration(), DedicatedConverterConfig.class);
        ScriptInvokeService scriptInvokeService = getScriptInvokeService(configuration);
        String functionField = ScriptLanguage.JS.equals(scriptInvokeService.getLanguage()) ? "function" : "tbelFunction";
        String function = configuration.getConfiguration().get(functionField).asText();
        this.evaluator = new ScriptUplinkEvaluator(configuration.getTenantId(), scriptInvokeService, configuration.getId(), function);
        IntegrationType integrationType = configuration.getIntegrationType();
        this.converterWrapper = ConverterWrapperFactory
                .getWrapper(integrationType)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported integrationType: " + integrationType));
    }

    @Override
    public void update(Converter configuration) {
        destroy();
        init(configuration);
    }

    @Override
    public void destroy() {
        if (this.evaluator != null) {
            this.evaluator.destroy();
        }
    }

    @Override
    public ListenableFuture<String> doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception {
        return evaluator.execute(data, metadata);
    }

    @Override
    public ListenableFuture<List<UplinkData>> convertUplink(ConverterContext context, byte[] data, UplinkMetaData metadata, ExecutorService callBackExecutorService) throws Exception {
        TbPair<byte[], UplinkMetaData> wrappedPair = converterWrapper.wrap(data, metadata);
        return super.convertUplink(context, wrappedPair.getFirst(), wrappedPair.getSecond(), callBackExecutorService);
    }

    @Override
    protected UplinkData parseUplinkData(JsonObject src, UplinkMetaData metadata) {
        Map<String, String> kvMap = new HashMap<>(metadata.getKvMap());

        JsonElement telemetry;
        if (src.has(TELEMETRY)) {
            telemetry = src.get(TELEMETRY);

            if (telemetry.isJsonArray()) {
                JsonObject telemetryFromMetadata = addKvs(new JsonObject(), kvMap, config.getTelemetry());
                if (!telemetryFromMetadata.isEmpty()) {
                    telemetry.getAsJsonArray().add(telemetryFromMetadata);
                }
            } else if (telemetry.isJsonObject()) {
                var obj = telemetry.getAsJsonObject();
                if (obj.has(VALUES)) {
                    var values = obj.get(VALUES).getAsJsonObject();
                    addKvs(values, kvMap, config.getTelemetry());
                } else {
                    addKvs(obj, kvMap, config.getTelemetry());
                }
            }
        } else {
            telemetry = addKvs(new JsonObject(), kvMap, config.getTelemetry());
        }

        JsonObject attributes;
        if (src.has(ATTRIBUTES)) {
            attributes = src.get(ATTRIBUTES).getAsJsonObject();
        } else {
            attributes = new JsonObject();
        }

        addKvs(attributes, kvMap, config.getAttributes());

        boolean isAsset = config.getType() == EntityType.ASSET;

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(isAsset);
        String entityName = processTemplate(config.getName(), kvMap);
        String profile = processTemplate(config.getProfile(), kvMap);
        if (profile == null) {
            profile = DEFAULT_PROFILE;
        }
        String label = processTemplate(config.getLabel(), kvMap);
        String customer = processTemplate(config.getCustomer(), kvMap);
        String group = processTemplate(config.getGroup(), kvMap);

        if (isAsset) {
            builder.assetName(entityName);
            builder.assetType(profile);
            if (label != null) {
                builder.assetLabel(label);
            }
        } else {
            builder.deviceName(entityName);
            builder.deviceType(profile);
            if (label != null) {
                builder.deviceLabel(label);
            }
        }

        if (customer != null) {
            builder.customerName(customer);
        }
        if (group != null) {
            builder.groupName(group);
        }

        Map<String, String> currentOnValueTelemetryUpdate = this.currentUpdateOnlyTelemetryPerEntity.getOrDefault(entityName, new ConcurrentHashMap<>());
        Map<String, String> currentOnValueAttributesUpdate = this.currentUpdateOnlyAttributesPerEntity.getOrDefault(entityName, new ConcurrentHashMap<>());

        TransportProtos.PostTelemetryMsg parsedTelemetry = parseTelemetry(telemetry);
        if (!this.updateOnlyKeys.isEmpty()) {
            parsedTelemetry = filterTelemetryOnKeyValueUpdateAndUpdateMap(parsedTelemetry, currentOnValueTelemetryUpdate);
        }
        builder.telemetry(parsedTelemetry);

        TransportProtos.PostAttributeMsg parsedAttributes = parseAttributesUpdate(attributes);
        if (!this.updateOnlyKeys.isEmpty()) {
            parsedAttributes = filterAttributeOnKeyValueUpdateAndUpdateMap(parsedAttributes, currentOnValueAttributesUpdate);
        }
        builder.attributesUpdate(parsedAttributes);

        if (!currentOnValueTelemetryUpdate.isEmpty()) {
            this.currentUpdateOnlyTelemetryPerEntity.put(entityName, currentOnValueTelemetryUpdate);
        }

        if (!currentOnValueAttributesUpdate.isEmpty()) {
            this.currentUpdateOnlyAttributesPerEntity.put(entityName, currentOnValueAttributesUpdate);
        }

        return builder.build();
    }

    private JsonObject addKvs(JsonObject kvsObj, Map<String, String> kvMap, Set<String> keys) {
        if (CollectionsUtil.isNotEmpty(keys) && !kvMap.isEmpty()) {
            kvMap.entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()) && !kvsObj.has(e.getKey()))
                    .forEach(e -> kvsObj.add(e.getKey(), gson.fromJson(e.getValue(), JsonElement.class)));
        }
        return kvsObj;
    }

    private String processTemplate(String template, Map<String, String> data) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, String> kv : data.entrySet()) {
            result = processVar(result, kv.getKey(), kv.getValue());
        }
        return result;
    }

    private String processVar(String pattern, String key, String val) {
        return pattern.replace(formatVarTemplate(key), val);
    }

    private static String formatVarTemplate(String key) {
        return "${" + key + '}';
    }

}
