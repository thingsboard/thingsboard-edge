/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class RemoteIntegrationContext implements IntegrationContext {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String REMOTE_INTEGRATION_CACHE = "remoteIntegration";

    protected final EventStorage eventStorage;
    protected final CacheManager cacheManager;
    protected final Integration configuration;
    protected final String clientId;
    protected final int port;
    protected final ConverterContext uplinkConverterContext;
    protected final ConverterContext downlinkConverterContext;

    public RemoteIntegrationContext(EventStorage eventStorage, CacheManager cacheManager, Integration configuration, String clientId, int port) {
        this.eventStorage = eventStorage;
        this.cacheManager = cacheManager;
        this.configuration = configuration;
        this.clientId = clientId;
        this.port = port;
        this.uplinkConverterContext = new RemoteConverterContext(eventStorage, true, mapper, clientId, port);
        this.downlinkConverterContext = new RemoteConverterContext(eventStorage, false, mapper, clientId, port);
    }

    @Override
    public ServerAddress getServerAddress() {
        return new ServerAddress(clientId, port, ServerType.CORE);
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addDeviceData(msg).build(), callback);
    }

    @Override
    public void processEntityViewCreation(EntityViewDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addEntityViewData(msg).build(), callback);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addTbMsg(ByteString.copyFrom(TbMsg.toBytes(msg))).build(), callback);
    }

    @Override
    public void saveEvent(String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        String eventData = "";
        try {
            eventData = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to convert event body!", body, e);
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(TbEventSource.INTEGRATION)
                        .setType(type)
                        .setUid(uid)
                        .setData(eventData)
                        .build())
                .build(), callback);
    }

    @Override
    public long findDeviceAttributeValue(String deviceName, String scope, String key) {
        Cache cache = cacheManager.getCache(REMOTE_INTEGRATION_CACHE);

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add("attr_");
        cacheKey.add(deviceName);
        cacheKey.add(scope);
        cacheKey.add(key);

        Long value = cache.get(cacheKey, Long.class);
        if (value != null) {
            return value;
        }
        return 0L;
    }

    @Override
    public void saveDeviceAttributeValueInCache(String deviceName, String scope, String key, long value) {
        Cache cache = cacheManager.getCache(REMOTE_INTEGRATION_CACHE);

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add("attr_");
        cacheKey.add(deviceName);
        cacheKey.add(scope);
        cacheKey.add(key);

        cache.put(cacheKey, value);
    }

    @Override
    public String findEventUid(String deviceName, String type, String uid) {
        Cache cache = cacheManager.getCache(REMOTE_INTEGRATION_CACHE);

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add("event_");
        cacheKey.add(deviceName);
        cacheKey.add(type);
        cacheKey.add(uid);

        return cache.get(cacheKey, String.class);
    }

    @Override
    public void saveEventUidInCache(String deviceName, String type, String uid) {
        Cache cache = cacheManager.getCache(REMOTE_INTEGRATION_CACHE);

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add("event_");
        cacheKey.add(deviceName);
        cacheKey.add(type);
        cacheKey.add(uid);

        cache.put(cacheKey, "");
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
    public boolean isClosed() {
        return false;
    }
}
