package org.thingsboard.server.service.integration;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.thingsboard.server.actors.service.ActorService;
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
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.downlink.DownlinkService;
import org.thingsboard.server.service.integration.msg.IntegrationDownlinkMsg;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class LocalIntegrationContext implements IntegrationContext {
    private static final ReentrantLock deviceCreationLock = new ReentrantLock();
    protected final IntegrationContextComponent ctx;
    protected final Integration configuration;
    protected final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void processUplinkData(TransportProtos.IntegrationUplinkMsg data, TransportServiceCallback<Void> callback) {
        Device device = getOrCreateDevice(data);

        UUID sessionId = UUID.randomUUID();
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .build();

        if (data.hasPostTelemetryMsg()) {
            ctx.getIntegrationService().process(sessionInfo, data.getPostTelemetryMsg(), callback);
        }

        if (data.hasPostAttributesMsg()) {
            ctx.getIntegrationService().process(sessionInfo, data.getPostAttributesMsg(), callback);
        }
    }

    @Override
    public void processCustomMsg(TbMsg tbMsg) {
        ctx.getActorService().onMsg(new SendToClusterMsg(this.configuration.getId(), new ServiceToRuleEngineMsg(this.configuration.getTenantId(), tbMsg)));
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
        //TODO: implement
        return null;
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            ctx.getDownlinkService().remove(configuration.getId(), device.getId());
        }
    }

    @Override
    public void saveEvent(String type, JsonNode body) {
        Event event = new Event();
        event.setTenantId(configuration.getTenantId());
        event.setEntityId(configuration.getId());
        event.setType(type);
        event.setBody(body);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    private Device getOrCreateDevice(TransportProtos.IntegrationUplinkMsg data) {
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

    private Device processGetOrCreateDevice(TransportProtos.IntegrationUplinkMsg data) {
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
    public ConverterContext getConverterContext() {
        return null;
    }

}