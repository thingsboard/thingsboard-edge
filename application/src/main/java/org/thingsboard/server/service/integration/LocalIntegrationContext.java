/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_TELEMETRY_REQUEST;

@Data
@Slf4j
public class LocalIntegrationContext implements IntegrationContext {

    private static final String DEVICE_VIEW_NAME_ENDING = "_View";

    protected final IntegrationContextComponent ctx;
    protected final Integration configuration;
    protected final ConverterContext uplinkConverterContext;
    protected final ConverterContext downlinkConverterContext;
    protected final ObjectMapper mapper = new ObjectMapper();
    private final Gson gson = new Gson();

    public LocalIntegrationContext(IntegrationContextComponent ctx, Integration configuration) {
        this.ctx = ctx;
        this.configuration = configuration;
        this.uplinkConverterContext = new LocalConverterContext(ctx.getConverterContextComponent(), configuration.getTenantId(), configuration.getDefaultConverterId());
        this.downlinkConverterContext = new LocalConverterContext(ctx.getConverterContextComponent(), configuration.getTenantId(), configuration.getDownlinkConverterId());
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        Device device = ctx.getPlatformIntegrationService().getOrCreateDevice(configuration, data.getDeviceName(), data.getDeviceType(), data.getCustomerName(), data.getGroupName());

        UUID sessionId = UUID.randomUUID();
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .build();

        if (data.hasPostTelemetryMsg()) {
            ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostTelemetryMsg(), callback);
        }

        if (data.hasPostAttributesMsg()) {
            ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostAttributesMsg(), callback);
        }
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        Asset asset = ctx.getPlatformIntegrationService().getOrCreateAsset(configuration, data.getAssetName(), data.getAssetType(), data.getCustomerName(), data.getGroupName());

        if (data.hasPostTelemetryMsg()) {
            data.getPostTelemetryMsg().getTsKvListList()
                    .forEach(tsKv -> {
                        TbMsgMetaData metaData = new TbMsgMetaData();
                        metaData.putValue("assetName", data.getAssetName());
                        metaData.putValue("assetType", data.getAssetType());
                        metaData.putValue("ts", tsKv.getTs() + "");
                        JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                        TbMsg tbMsg = TbMsg.newMsg(POST_TELEMETRY_REQUEST.name(), asset.getId(), metaData, gson.toJson(json));
                        ctx.getPlatformIntegrationService().process(asset.getTenantId(), tbMsg, callback);
                    });
        }

        if (data.hasPostAttributesMsg()) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("assetName", data.getAssetName());
            metaData.putValue("assetType", data.getAssetType());
            JsonObject json = JsonUtils.getJsonObject(data.getPostAttributesMsg().getKvList());
            TbMsg tbMsg = TbMsg.newMsg(POST_ATTRIBUTES_REQUEST.name(), asset.getId(), metaData, gson.toJson(json));
            ctx.getPlatformIntegrationService().process(asset.getTenantId(), tbMsg, callback);
        }
    }

    @Override
    public void createEntityView(EntityViewDataProto data, IntegrationCallback<Void> callback) {
        Device device = ctx.getPlatformIntegrationService()
                .getOrCreateDevice(configuration, data.getDeviceName(), data.getDeviceType(), null, null);
        ctx.getPlatformIntegrationService().getOrCreateEntityView(configuration, device, data);
    }

    @Override
    public void processCustomMsg(TbMsg tbMsg, IntegrationCallback<Void> callback) {
        ctx.getPlatformIntegrationService().process(this.configuration.getTenantId(), tbMsg, callback);
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void saveEvent(String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        saveEvent(configuration.getId(), type, uid, body, callback);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            saveEvent(device.getId(), type, uid, body, callback);
        }
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            return ctx.getDownlinkService().get(configuration.getId(), device.getId());
        } else {
            return null;
        }
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return ctx.getDownlinkService().put(msg);
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            ctx.getDownlinkService().remove(configuration.getId(), device.getId());
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    private void saveEvent(EntityId entityId, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(entityId);
        event.setType(type);
        event.setUid(uid);
        event.setBody(body);
        DonAsynchron.withCallback(ctx.getEventService().saveAsync(event), res -> callback.onSuccess(null), callback::onError);
    }

    @Override
    public String getServiceId() {
        return ctx.getServiceInfoProvider().getServiceId();
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return ctx.getEventLoopGroup();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return ctx.getScheduledExecutorService();
    }
}