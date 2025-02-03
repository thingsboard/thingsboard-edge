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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class DedicatedScriptUplinkDataConverter extends AbstractUplinkDataConverter {

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
        this.converterWrapper = ConverterWrapperFactory.getWrapper(configuration.getIntegrationType());
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
        TbPair<byte[], UplinkMetaData> wrappedPair = converterWrapper.wrap(config, data, metadata);
        return super.convertUplink(context, wrappedPair.getFirst(), wrappedPair.getSecond(), callBackExecutorService);
    }

    @Override
    protected UplinkData parseUplinkData(JsonObject src, UplinkMetaData metadata) {
        Map<String, String> kvMap = new HashMap<>(metadata.getKvMap());

        JsonObject telemetry = new JsonObject();
        JsonObject tsValues = new JsonObject();
        telemetry.add("values", tsValues);

        if (CollectionsUtil.isNotEmpty(config.getTelemetry())) {
            kvMap.entrySet().stream()
                    .filter((e -> config.getTelemetry().contains(e.getKey())))
                    .forEach(e -> tsValues.add(e.getKey(), gson.fromJson(e.getValue(), JsonElement.class)));
        }

        JsonObject attributes = new JsonObject();

        if (CollectionsUtil.isNotEmpty(config.getAttributes())) {
            kvMap.entrySet().stream()
                    .filter((e -> config.getAttributes().contains(e.getKey())))
                    .forEach(e -> attributes.add(e.getKey(), gson.fromJson(e.getValue(), JsonElement.class)));
        }

        if (src.has("telemetry")) {
            JsonObject srcTelemetry = src.get("telemetry").getAsJsonObject();
            if (srcTelemetry.has("values")) {
                srcTelemetry.get("values").getAsJsonObject().entrySet().forEach(e -> {
                    tsValues.add(e.getKey(), e.getValue());
                });
            }
            if (srcTelemetry.has("ts")) {
                telemetry.add("ts", srcTelemetry.get("ts"));
            }
        }

        if (src.has("attributes")) {
            src.get("attributes").getAsJsonObject().entrySet().forEach(e -> {
                attributes.add(e.getKey(), e.getValue());
            });
        }

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(!config.isDevice());
        String entityName = processTemplate(config.getName(), kvMap);
        String profile = processTemplate(config.getProfile(), kvMap);
        String label = processTemplate(config.getLabel(), kvMap);
        String customer = processTemplate(config.getCustomer(), kvMap);
        String group = processTemplate(config.getGroup(), kvMap);

        if (config.isDevice()) {
            builder.deviceName(entityName);
            builder.deviceType(profile);
            if (label != null) {
                builder.deviceLabel(label);
            }
        } else {
            builder.assetName(entityName);
            builder.assetType(profile);
            if (label != null) {
                builder.assetLabel(label);
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
        if (!telemetry.isEmpty()) {
            TransportProtos.PostTelemetryMsg parsedTelemetry = parseTelemetry(telemetry);
            if (!this.updateOnlyKeys.isEmpty()) {
                parsedTelemetry = filterTelemetryOnKeyValueUpdateAndUpdateMap(parsedTelemetry, currentOnValueTelemetryUpdate);
            }
            builder.telemetry(parsedTelemetry);
        }
        if (!attributes.isEmpty()) {
            TransportProtos.PostAttributeMsg parsedAttributes = parseAttributesUpdate(attributes);
            if (!this.updateOnlyKeys.isEmpty()) {
                parsedAttributes = filterAttributeOnKeyValueUpdateAndUpdateMap(parsedAttributes, currentOnValueAttributesUpdate);
            }
            builder.attributesUpdate(parsedAttributes);
        }

        if (!currentOnValueTelemetryUpdate.isEmpty()) {
            this.currentUpdateOnlyTelemetryPerEntity.put(entityName, currentOnValueTelemetryUpdate);
        }

        if (!currentOnValueAttributesUpdate.isEmpty()) {
            this.currentUpdateOnlyAttributesPerEntity.put(entityName, currentOnValueAttributesUpdate);
        }

        return builder.build();
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
