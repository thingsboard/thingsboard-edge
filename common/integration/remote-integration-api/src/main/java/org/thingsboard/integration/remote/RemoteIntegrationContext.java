/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.concurrent.ScheduledExecutorService;

@Data
@Slf4j
public class RemoteIntegrationContext implements IntegrationContext {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String REMOTE_INTEGRATION_CACHE = "remoteIntegration";

    protected final EventStorage eventStorage;
    protected final Integration configuration;
    protected final String clientId;
    protected final int port;
    protected final ConverterContext uplinkConverterContext;
    protected final ConverterContext downlinkConverterContext;
    protected final ScheduledExecutorService scheduledExecutorService;

    public RemoteIntegrationContext(EventStorage eventStorage, ScheduledExecutorService scheduledExecutorService, Integration configuration, String clientId, int port) {
        this.eventStorage = eventStorage;
        this.configuration = configuration;
        this.clientId = clientId;
        this.port = port;
        this.uplinkConverterContext = new RemoteConverterContext(eventStorage, true, mapper, clientId, port);
        this.downlinkConverterContext = new RemoteConverterContext(eventStorage, false, mapper, clientId, port);
        this.scheduledExecutorService = scheduledExecutorService;
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
    public void saveEvent(String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        saveEvent(TbEventSource.INTEGRATION, "", type, uid, body, callback);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        saveEvent(TbEventSource.DEVICE, deviceName, type, uid, body, callback);
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
    public boolean isClosed() {
        return false;
    }

    private void saveEvent(TbEventSource tbEventSource, String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        String eventData = "";
        try {
            eventData = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to convert event body!", body, e);
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(tbEventSource)
                        .setType(type)
                        .setUid(uid)
                        .setData(eventData)
                        .setDeviceName(deviceName)
                        .build())
                .build(), callback);
    }
}
