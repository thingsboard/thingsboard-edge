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
package org.thingsboard.integration.service.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.service.integration.IntegrationProtoUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class TbIntegrationExecutorIntegrationContext implements IntegrationContext {

    private final String serviceId;
    private final IntegrationApiService apiService;
    private final IntegrationStatisticsService statisticsService;
    private final TbIntegrationExecutorContextComponent contextComponent;
    private final Integration configuration;
    private final IntegrationInfoProto integrationInfoProto;
    private final LogSettingsComponent logSettingsComponent;

    public TbIntegrationExecutorIntegrationContext(String serviceId, IntegrationApiService apiService, IntegrationStatisticsService statisticsService,
                                                   TbIntegrationExecutorContextComponent contextComponent, LogSettingsComponent logSettingsComponent,
                                                   Integration configuration) {
        this.serviceId = serviceId;
        this.apiService = apiService;
        this.statisticsService = statisticsService;
        this.contextComponent = contextComponent;
        this.configuration = configuration;
        this.logSettingsComponent = logSettingsComponent;
        this.integrationInfoProto = IntegrationProtoUtil.toProto(configuration);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public ConverterContext getUplinkConverterContext() {
        return new TbIntegrationExecutorConverterContext(configuration.getDefaultConverterId(), TbEventSource.UPLINK_CONVERTER);
    }

    @Override
    public ConverterContext getDownlinkConverterContext() {
        return new TbIntegrationExecutorConverterContext(configuration.getDownlinkConverterId(), TbEventSource.DOWNLINK_CONVERTER);
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.trace("Received uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.trace("Received uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void createEntityView(EntityViewDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.trace("Received uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        apiService.sendUplinkData(configuration, integrationInfoProto, msg, callback);
    }

    @Override
    public void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.INTEGRATION, configuration.getId(), event, null, callback);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.DEVICE, configuration.getTenantId(), RawDataEvent.builder()
                .tenantId(configuration.getTenantId())
                .serviceId(getServiceId())
                .uuid(uid)
                .messageType(type)
                .message(body.toString())
                .build(), deviceName, callback);
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return contextComponent.getEventLoopGroup();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return contextComponent.getScheduledExecutorService();
    }

    @Override
    public void onUplinkMessageProcessed(boolean success) {
        if (configuration != null) {
            statisticsService.onUplinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public void onDownlinkMessageProcessed(boolean success) {
        if (configuration != null) {
            statisticsService.onUplinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public ExecutorService getExecutorService() {
        return contextComponent.getGeneralExecutorService();
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return contextComponent.getCallBackExecutorService();
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        Device device = contextComponent.findCachedDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            return contextComponent.getDownlinkCacheService().get(configuration.getId(), device.getId());
        } else {
            return null;
        }
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return contextComponent.getDownlinkCacheService().put(msg);
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {
        Device device = contextComponent.findCachedDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            contextComponent.getDownlinkCacheService().remove(configuration.getId(), device.getId());
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return logSettingsComponent.isExceptionStackTraceEnabled();
    }

    private void doSaveEvent(TbEventSource tbEventSource, EntityId entityId, Event event, String deviceName, IntegrationCallback<Void> callback) {
        var builder = TbIntegrationEventProto.newBuilder()
                .setSource(tbEventSource)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event)));
        builder.setTenantIdMSB(configuration.getTenantId().getId().getMostSignificantBits());
        builder.setTenantIdLSB(configuration.getTenantId().getId().getLeastSignificantBits());
        if (event.getEntityId() != null) {
            builder.setEventSourceIdMSB(event.getEntityId().getMostSignificantBits());
            builder.setEventSourceIdLSB(event.getEntityId().getLeastSignificantBits());

        }
        if (StringUtils.isNotEmpty(deviceName)) {
            builder.setDeviceName(deviceName);
        }
        apiService.sendEventData(configuration.getTenantId(), entityId, builder.build(), callback);
    }

    @RequiredArgsConstructor
    private class TbIntegrationExecutorConverterContext implements ConverterContext {

        private final ConverterId converterId;
        private final TbEventSource eventSource;

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public void saveEvent(Event event, IntegrationCallback<Void> callback) {
            TbIntegrationExecutorIntegrationContext.this.doSaveEvent(eventSource, converterId, event, null, callback);
        }
    }
}
