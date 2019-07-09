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
package org.thingsboard.server.service.integration;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.transport.SessionInfoProto;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class LocalIntegrationContext implements IntegrationContext {

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();

    protected final IntegrationContextComponent ctx;
    protected final Integration configuration;
    protected final ConverterContext uplinkConverterContext;
    protected final ConverterContext downlinkConverterContext;
    protected final ObjectMapper mapper = new ObjectMapper();

    public LocalIntegrationContext(IntegrationContextComponent ctx, Integration configuration) {
        this.ctx = ctx;
        this.configuration = configuration;
        this.uplinkConverterContext = new LocalConverterContext(ctx.getConverterContextComponent(), configuration.getTenantId(), configuration.getDefaultConverterId());
        this.downlinkConverterContext = new LocalConverterContext(ctx.getConverterContextComponent(), configuration.getTenantId(), configuration.getDownlinkConverterId());
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        Device device = getOrCreateDevice(data);

        UUID sessionId = UUID.randomUUID();
        SessionInfoProto sessionInfo = SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .build();

        if (data.hasPostTelemetryMsg()) {
            ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostTelemetryMsg(), callback);
        }

        if (data.hasPostAttributesMsg()) {
            ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostAttributesMsg(), callback);
        }
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        ctx.getActorService().onMsg(new SendToClusterMsg(this.configuration.getId(), new ServiceToRuleEngineMsg(this.configuration.getTenantId(), msg)));
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(type);
        event.setBody(body);
        DonAsynchron.withCallback(ctx.getEventService().saveAsync(event), res -> callback.onSuccess(null), callback::onError);
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

    private Device getOrCreateDevice(DeviceUplinkDataProto data) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), data.getDeviceName());
        if (device == null) {
            deviceCreationLock.lock();
            try {
                return processGetOrCreateDevice(data);
            } finally {
                deviceCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(DeviceUplinkDataProto data) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), data.getDeviceName());
        if (device == null) {
            device = new Device();
            device.setName(data.getDeviceName());
            device.setType(data.getDeviceType());
            device.setTenantId(configuration.getTenantId());
            device = ctx.getDeviceService().saveDevice(device);
            EntityRelation relation = new EntityRelation();
            relation.setFrom(configuration.getId());
            relation.setTo(device.getId());
            relation.setTypeGroup(RelationTypeGroup.COMMON);
            relation.setType(EntityRelation.INTEGRATION_TYPE);
            ctx.getRelationService().saveRelation(configuration.getTenantId(), relation);
            ctx.getActorService().onDeviceAdded(device);
            try {
                ObjectNode entityNode = mapper.valueToTree(device);
                TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), actionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
                ctx.getActorService().onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(configuration.getTenantId(), msg)));
            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
            }
        }
        return device;
    }

    private TbMsgMetaData actionTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", configuration.getId().toString());
        metaData.putValue("integrationName", configuration.getName());
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    @Override
    public ServerAddress getServerAddress() {
        return ctx.getDiscoveryService().getCurrentServer().getServerAddress();
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return ctx.getEventLoopGroup();
    }
}