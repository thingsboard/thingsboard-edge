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
package org.thingsboard.server.service.integration.rpc;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.ConverterUpdateMsg;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.IntegrationUpdateMsg;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.gen.transport.SessionInfoProto;
import org.thingsboard.server.service.integration.IntegrationContextComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class IntegrationGrpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase implements IntegrationRpcService {

    private static final String DEVICE_VIEW_NAME_ENDING = "_View";
    private static final ReentrantLock entityCreationLock = new ReentrantLock();
    public static final ObjectMapper mapper = new ObjectMapper();

    private final Map<StreamObserver<ResponseMsg>, IntegrationGrpcSession> sessions = new ConcurrentHashMap<>();

    @Value("${integrations.rpc.port}")
    private int rpcPort;
    @Value("${integrations.rpc.cert}")
    private String certFileResource;
    @Value("${integrations.rpc.privateKey}")
    private String privateKeyResource;

    @Autowired
    private IntegrationContextComponent ctx;

    private Server server;

    @PostConstruct
    public void init() {
        log.info("Initializing RPC service!");
        File certFile;
        File privateKeyFile;
        /*try {
            certFile = new File(Resources.getResource(certFileResource).toURI());
            privateKeyFile = new File(Resources.getResource(privateKeyResource).toURI());
        } catch (Exception e) {
            log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
            throw new RuntimeException("Unable to set up SSL context!", e);
        }*/
        server = ServerBuilder
                .forPort(rpcPort)
//                .useTransportSecurity(certFile, privateKeyFile)
                .addService(this)
                .build();
        log.info("Going to start RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!");
        }
        log.info("RPC service initialized!");
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> responseObserver) {
        return sessions.computeIfAbsent(responseObserver, r -> new IntegrationGrpcSession(this, responseObserver, sessions)).getInputStream();
    }

    @Override
    public void updateIntegration(Integration configuration) {
        for (Map.Entry<StreamObserver<ResponseMsg>, IntegrationGrpcSession> entry : sessions.entrySet()) {
            if (entry.getValue().getIntegrationId().equals(configuration.getId())) {
                try {
                    Converter defaultConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                            configuration.getDefaultConverterId());
                    ConverterConfigurationProto defaultConverterProto = constructConverterConfigProto(defaultConverter);

                    ConverterConfigurationProto downLinkConverterProto = ConverterConfigurationProto.getDefaultInstance();
                    if (configuration.getDownlinkConverterId() != null) {
                        Converter downlinkConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                                configuration.getDownlinkConverterId());
                        downLinkConverterProto = constructConverterConfigProto(downlinkConverter);
                    }

                    entry.getKey().onNext(ResponseMsg.newBuilder()
                            .setIntegrationUpdateMsg(IntegrationUpdateMsg.newBuilder()
                                    .setConfiguration(constructIntegrationConfigProto(configuration, defaultConverterProto, downLinkConverterProto))
                                    .build())
                            .build());
                } catch (JsonProcessingException e) {
                    log.error("Failed to construct proto objects!", e);
                }
            }
        }
    }

    @Override
    public void updateConverter(Converter converter) {
        try {
            for (Map.Entry<StreamObserver<ResponseMsg>, IntegrationGrpcSession> entry : sessions.entrySet()) {
                if (entry.getValue().getUplinkConverterId().equals(converter.getId()) || entry.getValue().getDownlinkConverterId().equals(converter.getId())) {
                    ConverterConfigurationProto defaultConverterProto = constructConverterConfigProto(converter);

                    entry.getKey().onNext(ResponseMsg.newBuilder()
                            .setConverterUpdateMsg(ConverterUpdateMsg.newBuilder()
                                    .setConfiguration(defaultConverterProto)
                                    .build())
                            .build());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to construct converter proto object!", e);
        }
    }

    @Override
    public ConnectResponseMsg validateConnect(ConnectRequestMsg request, IntegrationGrpcSession session) {
        Optional<Integration> optional = ctx.getIntegrationService().findIntegrationByRoutingKey(TenantId.SYS_TENANT_ID, request.getIntegrationRoutingKey());
        if (optional.isPresent()) {
            Integration configuration = optional.get();
            session.setIntegrationId(configuration.getId());
            try {
                if (configuration.isRemote() && configuration.getSecret().equals(request.getIntegrationSecret())) {
                    Converter defaultConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                            configuration.getDefaultConverterId());
                    session.setUplinkConverterId(defaultConverter.getId());
                    ConverterConfigurationProto defaultConverterProto = constructConverterConfigProto(defaultConverter);

                    ConverterConfigurationProto downLinkConverterProto = ConverterConfigurationProto.getDefaultInstance();
                    if (configuration.getDownlinkConverterId() != null) {
                        Converter downlinkConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                                configuration.getDownlinkConverterId());
                        session.setDownlinkConverterId(downlinkConverter.getId());
                        downLinkConverterProto = constructConverterConfigProto(downlinkConverter);
                    }

                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(constructIntegrationConfigProto(configuration, defaultConverterProto, downLinkConverterProto)).build();
                }
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg("Failed to validate the integration!")
                        .setConfiguration(IntegrationConfigurationProto.getDefaultInstance()).build();
            } catch (Exception e) {
                log.error("[{}] Failed to process integration connection!", request.getIntegrationRoutingKey(), e);
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                        .setErrorMsg("Failed to process integration connection!")
                        .setConfiguration(IntegrationConfigurationProto.getDefaultInstance()).build();
            }
        }
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                .setErrorMsg("Failed to find the integration! Routing key: " + request.getIntegrationRoutingKey())
                .setConfiguration(IntegrationConfigurationProto.getDefaultInstance()).build();
    }

    @Override
    public UplinkResponseMsg processUplinkMsg(TenantId tenantId, IntegrationId integrationId, ConverterId defaultConverterId, ConverterId downlinkConverterId, UplinkMsg msg) {
        if (msg.getDeviceDataCount() > 0) {
            for (DeviceUplinkDataProto data : msg.getDeviceDataList()) {
                Device device = getOrCreateDevice(tenantId, integrationId, data.getDeviceName(), data.getDeviceType());

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
                    ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostTelemetryMsg(), null);
                }

                if (data.hasPostAttributesMsg()) {
                    ctx.getPlatformIntegrationService().process(sessionInfo, data.getPostAttributesMsg(), null);
                }
            }
        }

        if (msg.getEntityViewDataCount() > 0) {
            for (EntityViewDataProto data : msg.getEntityViewDataList()) {
                createEntityViewForDeviceIfAbsent(tenantId, integrationId, getOrCreateDevice(tenantId, integrationId, data.getDeviceName(), data.getDeviceType()));
            }
        }

        if (msg.getEventsDataCount() > 0) {
            for (TbEventProto proto : msg.getEventsDataList()) {
                switch (proto.getSource()) {
                    case INTEGRATION:
                        saveDebugEvent(tenantId, integrationId, proto);
                        break;
                    case UPLINK_CONVERTER:
                        saveDebugEvent(tenantId, defaultConverterId, proto);
                        break;
                    case DOWNLINK_CONVERTER:
                        saveDebugEvent(tenantId, downlinkConverterId, proto);
                        break;
                }
            }
        }

        if (msg.getTbMsgCount() > 0) {
            for (ByteString tbMsgByteString : msg.getTbMsgList()) {
                TbMsg tbMsg = TbMsg.fromBytes(tbMsgByteString.toByteArray());
                ctx.getActorService().onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(tenantId, tbMsg)));
            }
        }

        return UplinkResponseMsg.newBuilder()
                .setSuccess(true)
                .setErrorMsg("")
                .build();
    }

    private IntegrationConfigurationProto constructIntegrationConfigProto(Integration configuration, ConverterConfigurationProto defaultConverterProto, ConverterConfigurationProto downLinkConverterProto) throws JsonProcessingException {
        return IntegrationConfigurationProto.newBuilder()
                .setTenantIdMSB(configuration.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(configuration.getTenantId().getId().getLeastSignificantBits())
                .setUplinkConverter(defaultConverterProto)
                .setDownlinkConverter(downLinkConverterProto)
                .setName(configuration.getName())
                .setRoutingKey(configuration.getRoutingKey())
                .setType(configuration.getType().toString())
                .setDebugMode(configuration.isDebugMode())
                .setConfiguration(mapper.writeValueAsString(configuration.getConfiguration()))
                .setAdditionalInfo(mapper.writeValueAsString(configuration.getAdditionalInfo()))
                .build();
    }

    private ConverterConfigurationProto constructConverterConfigProto(Converter converter) throws JsonProcessingException {
        return ConverterConfigurationProto.newBuilder()
                .setTenantIdMSB(converter.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(converter.getTenantId().getId().getLeastSignificantBits())
                .setConverterIdMSB(converter.getId().getId().getMostSignificantBits())
                .setConverterIdLSB(converter.getId().getId().getLeastSignificantBits())
                .setName(converter.getName())
                .setDebugMode(converter.isDebugMode())
                .setConfiguration(mapper.writeValueAsString(converter.getConfiguration()))
                .setAdditionalInfo(mapper.writeValueAsString(converter.getAdditionalInfo()))
                .build();
    }

    private void saveDebugEvent(TenantId tenantId, EntityId entityId, TbEventProto proto) {
        try {
            Event event = new Event();
            event.setTenantId(tenantId);
            event.setEntityId(entityId);
            event.setType(proto.getType());
            event.setUid(proto.getUid());
            event.setBody(mapper.readTree(proto.getData()));
            ctx.getEventService().save(event);
        } catch (IOException e) {
            log.warn("[{}] Failed to convert event body to JSON!", proto.getData(), e);
        }
    }

    private Device getOrCreateDevice(TenantId tenantId, IntegrationId integrationId, String deviceName, String deviceType) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(tenantId, deviceName);
        if (device == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateDevice(tenantId, integrationId, deviceName, deviceType);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(TenantId tenantId, IntegrationId integrationId, String deviceName, String deviceType) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(tenantId, deviceName);
        if (device == null) {
            device = createDevice(tenantId, deviceName, deviceType);
            createRelation(tenantId, integrationId, device.getId());
            ctx.getActorService().onDeviceAdded(device);
            try {
                ObjectNode entityNode = mapper.valueToTree(device);
                TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), actionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
                ctx.getActorService().onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(tenantId, msg)));
            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
            }
        }
        return device;
    }

    private Device createDevice(TenantId tenantId, String deviceName, String deviceType) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(deviceType);
        device.setTenantId(tenantId);
        return ctx.getDeviceService().saveDevice(device);
    }

    private void createEntityViewForDeviceIfAbsent(TenantId tenantId, IntegrationId integrationId, Device device) {
        String entityViewName = device.getName() + DEVICE_VIEW_NAME_ENDING;
        EntityView entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(tenantId, entityViewName);
        if (entityView == null) {
            entityCreationLock.lock();
            try {
                entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(tenantId, entityViewName);
                if (entityView == null) {
                    entityView = new EntityView();
                    entityView.setName(entityViewName);
                    entityView.setType("deviceView");
                    entityView.setTenantId(tenantId);
                    entityView.setEntityId(device.getId());

                    TelemetryEntityView telemetryEntityView = new TelemetryEntityView();
                    telemetryEntityView.setTimeseries(Collections.singletonList("FILLINGLEVEL"));
                    entityView.setKeys(telemetryEntityView);

                    entityView = ctx.getEntityViewService().saveEntityView(entityView);
                    createRelation(tenantId, integrationId, entityView.getId());
                }
            } finally {
                entityCreationLock.unlock();
            }
        }
    }

    private void createRelation(TenantId tenantId, IntegrationId integrationId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(integrationId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        ctx.getRelationService().saveRelation(tenantId, relation);
    }

    private TbMsgMetaData actionTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", null);
        metaData.putValue("integrationName", null);
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }
}
