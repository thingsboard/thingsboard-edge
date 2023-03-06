/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.UplinkContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.ExceptionUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ashvayka on 25.12.17.
 */
@Slf4j
public abstract class AbstractIntegration<T> implements ThingsboardPlatformIntegration<T> {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected Integration configuration;
    protected IntegrationContext context;
    protected TBUplinkDataConverter uplinkConverter;
    protected TBDownlinkDataConverter downlinkConverter;
    protected UplinkMetaData metadataTemplate;
    protected IntegrationStatistics integrationStatistics;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        this.configuration = params.getConfiguration();
        this.context = params.getContext();
        this.uplinkConverter = params.getUplinkConverter();
        this.downlinkConverter = params.getDownlinkConverter();
        Map<String, String> mdMap = new HashMap<>();
        mdMap.put("integrationName", configuration.getName());
        JsonNode metadata = configuration.getConfiguration().get("metadata");
        for (Iterator<Map.Entry<String, JsonNode>> it = metadata.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> md = it.next();
            mdMap.put(md.getKey(), md.getValue().asText());
        }
        this.metadataTemplate = new UplinkMetaData(getDefaultUplinkContentType(), mdMap);

        if (integrationStatistics == null) {
            this.integrationStatistics = new IntegrationStatistics(context);
        }
    }

    public void setConfiguration(Integration configuration) {
        this.configuration = configuration;
    }

    protected UplinkContentType getDefaultUplinkContentType() {
        return UplinkContentType.JSON;
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public Integration getConfiguration() {
        return configuration;
    }

    @Override
    public void validateConfiguration(Integration configuration, boolean allowLocalNetworkHosts) throws ThingsboardException {
        if (configuration == null || configuration.getConfiguration() == null) {
            throw new IllegalArgumentException("Integration configuration is empty!");
        }
        if (!configuration.isRemote()) {
            doValidateConfiguration(configuration.getConfiguration(), allowLocalNetworkHosts);
        }
    }

    @Override
    public void checkConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        if (integration == null || integration.getConfiguration() == null) {
            throw new IllegalArgumentException("Integration configuration is empty!");
        }
        if (!integration.isRemote()) {
            doCheckConnection(integration, ctx);
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {

    }

    @Override
    public IntegrationStatistics popStatistics() {
        IntegrationStatistics statistics = this.integrationStatistics;
        this.integrationStatistics = new IntegrationStatistics(context);
        return statistics;
    }

    protected <T> T getClientConfiguration(Integration configuration, Class<T> clazz) {
        JsonNode clientConfiguration = configuration.getConfiguration().get("clientConfiguration");
        return getClientConfiguration(clientConfiguration, clazz);
    }

    protected <T> T getClientConfiguration(JsonNode clientConfiguration, Class<T> clazz) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("clientConfiguration field is missing!");
        } else {
            return JacksonUtil.convertValue(clientConfiguration, clazz);
        }
    }

    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) throws ThingsboardException {

    }

    protected void doCheckConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {

    }

    protected void processUplinkData(IntegrationContext context, UplinkData data) {
        if (data.isAsset()) {
            processAssetUplinkData(context, data);
        } else {
            processDeviceUplinkData(context, data);
        }
    }

    private void processDeviceUplinkData(IntegrationContext context, UplinkData data) {
        DeviceUplinkDataProto.Builder builder = DeviceUplinkDataProto.newBuilder()
                .setDeviceName(data.getDeviceName())
                .setDeviceType(data.getDeviceType());
        if (StringUtils.isNotEmpty(data.getDeviceLabel())) {
            builder.setDeviceLabel(data.getDeviceLabel());
        }
        if (StringUtils.isNotEmpty(data.getCustomerName())) {
            builder.setCustomerName(data.getCustomerName());
        }
        if (StringUtils.isNotEmpty(data.getGroupName())) {
            builder.setGroupName(data.getGroupName());
        }
        if (data.getTelemetry() != null) {
            builder.setPostTelemetryMsg(data.getTelemetry());
        }
        if (data.getAttributesUpdate() != null) {
            builder.setPostAttributesMsg(data.getAttributesUpdate());
        }
        context.processUplinkData(builder.build(), null);
    }

    private void processAssetUplinkData(IntegrationContext context, UplinkData data) {
        AssetUplinkDataProto.Builder builder = AssetUplinkDataProto.newBuilder()
                .setAssetName(data.getAssetName()).setAssetType(data.getAssetType());
        if (StringUtils.isNotEmpty(data.getAssetLabel())) {
            builder.setAssetLabel(data.getAssetLabel());
        }
        if (StringUtils.isNotEmpty(data.getCustomerName())) {
            builder.setCustomerName(data.getCustomerName());
        }
        if (StringUtils.isNotEmpty(data.getGroupName())) {
            builder.setGroupName(data.getGroupName());
        }
        if (data.getTelemetry() != null) {
            builder.setPostTelemetryMsg(data.getTelemetry());
        }
        if (data.getAttributesUpdate() != null) {
            builder.setPostAttributesMsg(data.getAttributesUpdate());
        }
        context.processUplinkData(builder.build(), null);
    }

    protected void createEntityView(IntegrationContext context, UplinkData data, String viewName, String viewType, List<String> telemetryKeys) {
        context.createEntityView(EntityViewDataProto.newBuilder()
                .setViewName(viewName).setViewType(viewType)
                .setDeviceName(data.getDeviceName()).setDeviceType(data.getDeviceType()).addAllTelemetryKeys(telemetryKeys).build(), null);
    }

    protected static boolean isLocalNetworkHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
                    address.isSiteLocalAddress()) {
                return true;
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve provided hostname: " + host);
        }
        return false;
    }

    protected void persistDebug(IntegrationContext context, String type, UplinkContentType messageType, String message, String status, Exception exception) {
        persistDebug(context, type, messageType.name(), message, status, exception);
    }

    protected void persistDebug(IntegrationContext context, String type, String messageType, String message, String status, Exception exception) {
        var event = IntegrationDebugEvent.builder()
                .tenantId(configuration.getTenantId())
                .entityId(configuration.getId().getId())
                .serviceId(context.getServiceId())
                .eventType(type)
                .messageType(messageType)
                .message(message)
                .status(status);
        if (exception != null) {
            event.error(toString(exception));
        }

        context.saveEvent(event.build(), new DebugEventCallback());
    }

    protected String toString(Exception e) {
        return ExceptionUtil.toString(e, configuration.getId(), context.isExceptionStackTraceEnabled());
    }

    protected ListenableFuture<List<UplinkData>> convertToUplinkDataListAsync(IntegrationContext context, byte[] data, UplinkMetaData md) throws Exception {
        try {
            return this.uplinkConverter.convertUplink(context.getUplinkConverterContext(), data, md, context.getCallBackExecutorService());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Failed to apply uplink data converter function for data: {} and metadata: {}", configuration.getId(), configuration.getName(), Base64Utils.encodeToString(data), md);
            }
            throw e;
        }
    }

    protected List<UplinkData> convertToUplinkDataList(IntegrationContext context, byte[] data, UplinkMetaData md) throws Exception {
        return convertToUplinkDataListAsync(context, data, md).get();
    }

    protected void reportDownlinkOk(IntegrationContext context, DownlinkData data) {
        context.onDownlinkMessageProcessed(true);
        integrationStatistics.incMessagesProcessed();
        if (configuration.isDebugMode()) {
            try {
                ObjectNode json = mapper.createObjectNode();
                if (data.getMetadata() != null && !data.getMetadata().isEmpty()) {
                    json.set("metadata", mapper.valueToTree(data.getMetadata()));
                }
                json.set("payload", getDownlinkPayloadJson(data));
                persistDebug(context, "Downlink", "JSON", mapper.writeValueAsString(json), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    protected void reportDownlinkError(IntegrationContext context, TbMsg msg, String status, Exception exception) {
        if (!status.equals("OK")) {
            context.onDownlinkMessageProcessed(false);
            integrationStatistics.incErrorsOccurred();
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Failed to apply downlink data converter function for data: {} and metadata: {}", configuration.getId(), configuration.getName(), msg.getData(), msg.getMetaData());
            }
            if (configuration.isDebugMode()) {
                try {
                    persistDebug(context, "Downlink", "JSON", mapper.writeValueAsString(msg), status, exception);
                } catch (Exception e) {
                    log.warn("Failed to persist debug message", e);
                }
            }
        }
    }

    protected JsonNode getDownlinkPayloadJson(DownlinkData data) throws IOException {
        String contentType = data.getContentType();
        if ("JSON".equals(contentType)) {
            return mapper.readTree(data.getData());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(data.getData(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64Utils.encodeToString(data.getData()));
        }
    }

    protected <T> void logDownlink(IntegrationContext context, String updateType, T msg) {
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, updateType, "JSON", mapper.writeValueAsString(msg), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private static class DebugEventCallback implements IntegrationCallback<Void> {
        @Override
        public void onSuccess(Void msg) {
            if (log.isDebugEnabled()) {
                log.debug("Event has been saved successfully!");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.error("Failed to save the debug event!", e);
        }
    }

}
