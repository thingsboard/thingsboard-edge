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
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Data
@Slf4j
@RequiredArgsConstructor
public class RemoteIntegrationContext implements IntegrationContext {

    private static final String REMOTE_INTEGRATION_CACHE = "remoteIntegration";

    protected final EventStorage eventStorage;
    protected final Integration configuration;
    protected final String clientId;
    protected final int port;
    protected final ConverterContext uplinkConverterContext;
    protected final ConverterContext downlinkConverterContext;
    protected final ScheduledExecutorService scheduledExecutorService;
    protected final ExecutorService generalExecutorService;
    protected final ExecutorService callBackExecutorService;

    public RemoteIntegrationContext(EventStorage eventStorage, ScheduledExecutorService scheduledExecutorService,
                                    ExecutorService generalExecutorService, ExecutorService callBackExecutorService,
                                    Integration configuration, String clientId, int port) {
        this.eventStorage = eventStorage;
        this.configuration = configuration;
        this.clientId = clientId;
        this.port = port;
        this.uplinkConverterContext = new RemoteConverterContext(eventStorage, true, clientId, port);
        this.downlinkConverterContext = new RemoteConverterContext(eventStorage, false, clientId, port);
        this.scheduledExecutorService = scheduledExecutorService;
        this.generalExecutorService = generalExecutorService;
        this.callBackExecutorService = callBackExecutorService;
    }

    @Override
    public String getServiceId() {
        return "[" + clientId + ":" + port + "]";
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addDeviceData(msg).build(), callback);
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addAssetData(msg).build(), callback);
    }

    @Override
    public void createEntityView(EntityViewDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addEntityViewData(msg).build(), callback);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addTbMsg(TbMsg.toByteString(msg)).build(), callback);
    }

    @Override
    public void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.INTEGRATION, event, null, callback);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.DEVICE, RawDataEvent.builder()
                .tenantId(configuration.getTenantId())
                .serviceId(getServiceId())
                .uuid(uid)
                .messageType(type)
                .message(body.toString())
                .build(), deviceName, callback);
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return null;
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        return null;
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return null;
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {

    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Override
    public ExecutorService getExecutorService() {
        return generalExecutorService;
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return callBackExecutorService;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return true;
    }

    @Override
    public void onUplinkMessageProcessed(boolean success) {
        // Statistics for remote integrations is not supported
    }

    @Override
    public void onDownlinkMessageProcessed(boolean success) {
        // Statistics for remote integrations is not supported
    }

    private void doSaveEvent(TbEventSource tbEventSource, Event event, String deviceName, IntegrationCallback<Void> callback) {
        var builder = TbEventProto.newBuilder()
                .setSource(tbEventSource)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event)));
        if (deviceName != null) {
            builder.setDeviceName(deviceName);
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(builder.build())
                .build(), callback);
    }

}
