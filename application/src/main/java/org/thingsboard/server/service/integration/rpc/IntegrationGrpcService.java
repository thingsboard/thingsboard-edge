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
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.gen.transport.SessionInfoProto;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class IntegrationGrpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase {

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();
    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${integrations.rpc.port}")
    private int rpcPort;
    @Value("${integrations.rpc.cert}")
    private String certFileResource;
    @Value("${integrations.rpc.privateKey}")
    private String privateKeyResource;

    @Autowired
    private PlatformIntegrationService platformIntegrationService;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private ConverterService converterService;
    @Autowired
    private ActorService actorService;
    @Autowired
    private EventService eventService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private RelationService relationService;

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
    public void connect(ConnectRequestMsg request, StreamObserver<ConnectResponseMsg> responseObserver) {
        try {
            responseObserver.onNext(validateConnect(request));
            responseObserver.onCompleted();
        } catch (JsonProcessingException e) {
            log.error("[{}] Failed to process the connection of integration!", request.getIntegrationRoutingKey(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void sendUplinkMsg(UplinkMsg msg, StreamObserver<UplinkResponseMsg> responseObserver) {
        responseObserver.onNext(processMsg(msg));
        responseObserver.onCompleted();
    }

    private UplinkResponseMsg processMsg(UplinkMsg msg) {
        if (msg.getDeviceDataCount() > 0) {
            List<DeviceUplinkDataProto> deviceDataList = msg.getDeviceDataList();
            for (DeviceUplinkDataProto data : deviceDataList) {
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
                    platformIntegrationService.process(sessionInfo, data.getPostTelemetryMsg(), null);
                }

                if (data.hasPostAttributesMsg()) {
                    platformIntegrationService.process(sessionInfo, data.getPostAttributesMsg(), null);
                }
            }
        }

        if (msg.getEventsDataCount() > 0) {
            for (TbEventProto proto : msg.getEventsDataList()) {
                try {
                    eventService.save(mapper.readValue(proto.getData(), Event.class));
                } catch (IOException e) {
                    log.warn("[{}] Failed to read string value to Event!", proto.getData(), e);
                    // TODO: 7/2/19 return error?
                }
            }
        }

        if (msg.getTbMsgCount() > 0) {
            for (ByteString tbMsgByteString : msg.getTbMsgList()) {
                TbMsg tbMsg = TbMsg.fromBytes(tbMsgByteString.toByteArray());
                actorService.onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(null, tbMsg))); // TODO: 7/2/19 tenantId?
            }
        }

        return UplinkResponseMsg.newBuilder()
                .setSuccess(true)
                .setErrorMsg("")
                .build();
    }

    private Device getOrCreateDevice(DeviceUplinkDataProto data) {
        Device device = deviceService.findDeviceByTenantIdAndName(null, data.getDeviceName()); // TODO: 7/2/19 tenantId?
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
        Device device = deviceService.findDeviceByTenantIdAndName(null, data.getDeviceName());
        if (device == null) {
            device = new Device();
            device.setName(data.getDeviceName());
            device.setType(data.getDeviceType());
            device.setTenantId(null);
            device = deviceService.saveDevice(device);
            EntityRelation relation = new EntityRelation();
            relation.setFrom(null);
            relation.setTo(device.getId());
            relation.setTypeGroup(RelationTypeGroup.COMMON);
            relation.setType(EntityRelation.INTEGRATION_TYPE);
            relationService.saveRelation(null, relation);
            actorService.onDeviceAdded(device);
            try {
                ObjectNode entityNode = mapper.valueToTree(device);
                TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), actionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
                actorService.onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(null, msg)));
            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
            }
        }
        return device;
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

    private ConnectResponseMsg validateConnect(ConnectRequestMsg request) throws JsonProcessingException {
        Optional<Integration> optional = integrationService.findIntegrationByRoutingKey(TenantId.SYS_TENANT_ID, request.getIntegrationRoutingKey());
        if (optional.isPresent()) {
            Integration configuration = optional.get();
            if (configuration.isRemote() && configuration.getSecret().equals(request.getIntegrationSecret())) {
                Converter defaultConverter = converterService.findConverterById(configuration.getTenantId(),
                        configuration.getDefaultConverterId());
                ConverterConfigurationProto defaultConverterProto = ConverterConfigurationProto.newBuilder()
                        .setTenantIdMSB(defaultConverter.getTenantId().getId().getMostSignificantBits())
                        .setTenantIdLSB(defaultConverter.getTenantId().getId().getLeastSignificantBits())
                        .setConverterIdMSB(defaultConverter.getId().getId().getMostSignificantBits())
                        .setConverterIdLSB(defaultConverter.getId().getId().getLeastSignificantBits())
                        .setName(defaultConverter.getName())
                        .setDebugMode(defaultConverter.isDebugMode())
                        .setConfiguration(mapper.writeValueAsString(defaultConverter.getConfiguration()))
                        .setAdditionalInfo(mapper.writeValueAsString(defaultConverter.getAdditionalInfo()))
                        .build();

                ConverterConfigurationProto downLinkConverterProto = ConverterConfigurationProto.newBuilder().getDefaultInstanceForType();
                if (configuration.getDownlinkConverterId() != null) {
                    Converter downlinkConverter = converterService.findConverterById(configuration.getTenantId(),
                            configuration.getDownlinkConverterId());
                    downLinkConverterProto = ConverterConfigurationProto.newBuilder()
                            .setTenantIdMSB(downlinkConverter.getTenantId().getId().getMostSignificantBits())
                            .setTenantIdLSB(downlinkConverter.getTenantId().getId().getLeastSignificantBits())
                            .setConverterIdMSB(downlinkConverter.getId().getId().getMostSignificantBits())
                            .setConverterIdLSB(downlinkConverter.getId().getId().getLeastSignificantBits())
                            .setName(downlinkConverter.getName())
                            .setDebugMode(downlinkConverter.isDebugMode())
                            .setConfiguration(mapper.writeValueAsString(downlinkConverter.getConfiguration()))
                            .setAdditionalInfo(mapper.writeValueAsString(downlinkConverter.getAdditionalInfo()))
                            .build();
                }

                IntegrationConfigurationProto proto = IntegrationConfigurationProto.newBuilder()
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
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.ACCEPTED)
                        .setErrorMsg("")
                        .setConfiguration(proto).build();
            }
            return ConnectResponseMsg.newBuilder()
                    .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                    .setErrorMsg("Failed to validate the integration!")
                    .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build();
        } else {
            return ConnectResponseMsg.newBuilder()
                    .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                    .setErrorMsg("Failed to find the integration! Routing key: " + request.getIntegrationRoutingKey())
                    .setConfiguration(IntegrationConfigurationProto.newBuilder().getDefaultInstanceForType()).build();
        }
    }
}
