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
import com.google.common.util.concurrent.FutureCallback;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
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
import org.thingsboard.server.gen.integration.DeviceDownlinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.DownlinkMsg;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationStatisticsProto;
import org.thingsboard.server.gen.integration.IntegrationUpdateMsg;
import org.thingsboard.server.gen.integration.MessageType;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.gen.transport.KeyValueProto;
import org.thingsboard.server.gen.transport.KeyValueType;
import org.thingsboard.server.gen.transport.SessionInfoProto;
import org.thingsboard.server.gen.transport.TsKvListProto;
import org.thingsboard.server.service.integration.IntegrationContextComponent;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@Slf4j
public final class IntegrationGrpcSession implements Closeable {

    private static final ReentrantLock entityCreationLock = new ReentrantLock();
    public static final ObjectMapper mapper = new ObjectMapper();

    private final UUID sessionId;
    private final BiConsumer<IntegrationId, IntegrationGrpcSession> sessionOpenListener;
    private final Consumer<IntegrationId> sessionCloseListener;

    private IntegrationContextComponent ctx;
    private Integration configuration;
    private StreamObserver<RequestMsg> inputStream;
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;

    IntegrationGrpcSession(IntegrationContextComponent ctx, StreamObserver<ResponseMsg> outputStream
            , BiConsumer<IntegrationId, IntegrationGrpcSession> sessionOpenListener
            , Consumer<IntegrationId> sessionCloseListener) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        initInputStream();
    }

    private void initInputStream() {
        this.inputStream = new StreamObserver<RequestMsg>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                if (!connected && requestMsg.getMessageType().equals(MessageType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    }
                }
                if (connected) {
                    if (requestMsg.getMessageType().equals(MessageType.UPLINK_RPC_MESSAGE) && requestMsg.hasUplinkMsg()) {
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setUplinkResponseMsg(processUplinkMsg(requestMsg.getUplinkMsg()))
                                .build());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Failed to deliver message from client!", t);
            }

            @Override
            public void onCompleted() {
                sessionCloseListener.accept(configuration.getId());
                outputStream.onCompleted();
            }
        };
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        Optional<Integration> optional = ctx.getIntegrationService().findIntegrationByRoutingKey(TenantId.SYS_TENANT_ID, request.getIntegrationRoutingKey());
        if (optional.isPresent()) {
            configuration = optional.get();
            try {
                if (configuration.isRemote() && configuration.getSecret().equals(request.getIntegrationSecret())) {
                    Converter defaultConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                            configuration.getDefaultConverterId());
                    ConverterConfigurationProto defaultConverterProto = constructConverterConfigProto(defaultConverter);

                    ConverterConfigurationProto downLinkConverterProto = ConverterConfigurationProto.getDefaultInstance();
                    if (configuration.getDownlinkConverterId() != null) {
                        Converter downlinkConverter = ctx.getConverterService().findConverterById(configuration.getTenantId(),
                                configuration.getDownlinkConverterId());
                        downLinkConverterProto = constructConverterConfigProto(downlinkConverter);
                    }
                    connected = true;
                    sessionOpenListener.accept(configuration.getId(), this);
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

    private UplinkResponseMsg processUplinkMsg(UplinkMsg msg) {
        if (msg.getDeviceDataCount() > 0) {
            for (DeviceUplinkDataProto data : msg.getDeviceDataList()) {
                Device device = getOrCreateDevice(data.getDeviceName(), data.getDeviceType(), data.getCustomerName());

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
                createEntityViewForDeviceIfAbsent(getOrCreateDevice(data.getDeviceName(), data.getDeviceType(), null), data);
            }
        }

        if (msg.getIntegrationStatisticsCount() > 0) {
            for (IntegrationStatisticsProto data : msg.getIntegrationStatisticsList()) {
                processIntegrationStatistics(data);
            }
        }

        if (msg.getEventsDataCount() > 0) {
            for (TbEventProto proto : msg.getEventsDataList()) {
                switch (proto.getSource()) {
                    case INTEGRATION:
                        saveDebugEvent(configuration.getTenantId(), configuration.getId(), proto);
                        break;
                    case UPLINK_CONVERTER:
                        saveDebugEvent(configuration.getTenantId(), configuration.getDefaultConverterId(), proto);
                        break;
                    case DOWNLINK_CONVERTER:
                        saveDebugEvent(configuration.getTenantId(), configuration.getDownlinkConverterId(), proto);
                        break;
                    case DEVICE:
                        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), proto.getDeviceName());
                        if (device != null) {
                            saveDebugEvent(configuration.getTenantId(), device.getId(), proto);
                        }
                        break;
                }
            }
        }

        if (msg.getTbMsgCount() > 0) {
            for (ByteString tbMsgByteString : msg.getTbMsgList()) {
                TbMsg tbMsg = TbMsg.fromBytes(tbMsgByteString.toByteArray());
                ctx.getActorService().onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(configuration.getTenantId(), tbMsg)));
            }
        }

        return UplinkResponseMsg.newBuilder()
                .setSuccess(true)
                .setErrorMsg("")
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

    private Device getOrCreateDevice(String deviceName, String deviceType, String customerName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateDevice(deviceName, deviceType, customerName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(String deviceName, String deviceType, String customerName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device == null) {
            device = new Device();
            device.setName(deviceName);
            device.setType(deviceType);
            device.setTenantId(configuration.getTenantId());
            if (!StringUtils.isEmpty(customerName)) {
                Customer customer;
                Optional<Customer> customerOptional = ctx.getCustomerService().findCustomerByTenantIdAndTitle(configuration.getTenantId(), customerName);
                if (customerOptional.isPresent()) {
                    customer = customerOptional.get();
                } else {
                    customer = new Customer();
                    customer.setTitle(customerName);
                    customer.setTenantId(configuration.getTenantId());
                    customer = ctx.getCustomerService().saveCustomer(customer);
                    pushCustomerCreatedEventToRuleEngine(customer);
                }
                device.setCustomerId(customer.getId());
            }
            device = ctx.getDeviceService().saveDevice(device);
            createRelationFromIntegration(device.getId());
            ctx.getActorService().onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(device);
        }
        return device;
    }

    private void createEntityViewForDeviceIfAbsent(Device device, EntityViewDataProto proto) {
        String entityViewName = proto.getViewName();
        EntityView entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
        if (entityView == null) {
            entityCreationLock.lock();
            try {
                entityView = ctx.getEntityViewService().findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
                if (entityView == null) {
                    entityView = new EntityView();
                    entityView.setName(entityViewName);
                    entityView.setType(proto.getViewType());
                    entityView.setTenantId(configuration.getTenantId());
                    entityView.setEntityId(device.getId());

                    TelemetryEntityView telemetryEntityView = new TelemetryEntityView();
                    telemetryEntityView.setTimeseries(proto.getTelemetryKeysList());
                    entityView.setKeys(telemetryEntityView);

                    entityView = ctx.getEntityViewService().saveEntityView(entityView);
                    createRelationFromIntegration(entityView.getId());
                }
            } finally {
                entityCreationLock.unlock();
            }
        }
    }

    private void createRelationFromIntegration(EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(configuration.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        ctx.getRelationService().saveRelation(configuration.getTenantId(), relation);
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
                .setEnabled(configuration.isEnabled())
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

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), deviceActionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
            ctx.getActorService().onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(configuration.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushCustomerCreatedEventToRuleEngine(Customer customer) {
        try {
            ObjectNode entityNode = mapper.valueToTree(customer);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, customer.getId(), customerActionTbMsgMetaData(), mapper.writeValueAsString(entityNode), null, null, 0L);
            ctx.getActorService().onMsg(new SendToClusterMsg(customer.getId(), new ServiceToRuleEngineMsg(configuration.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push customer action to rule engine: {}", customer.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = getTbMsgMetaData();
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData customerActionTbMsgMetaData() {
        return getTbMsgMetaData();
    }

    private TbMsgMetaData getTbMsgMetaData() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", configuration.getId().toString());
        metaData.putValue("integrationName", configuration.getName());
        return metaData;
    }

    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }

    void onConfigurationUpdate(Integration configuration) {
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
            this.configuration = configuration;
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setIntegrationUpdateMsg(IntegrationUpdateMsg.newBuilder()
                            .setConfiguration(constructIntegrationConfigProto(configuration, defaultConverterProto, downLinkConverterProto))
                            .build())
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to construct proto objects!", e);
        }
    }

    void onConverterUpdate(Converter converter) {
        try {
            ConverterConfigurationProto defaultConverterProto = constructConverterConfigProto(converter);

            outputStream.onNext(ResponseMsg.newBuilder()
                    .setConverterUpdateMsg(ConverterUpdateMsg.newBuilder()
                            .setConfiguration(defaultConverterProto)
                            .build())
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to construct proto objects!", e);
        }
    }

    void onDownlink(Device device, IntegrationDownlinkMsg msg) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setDownlinkMsg(DownlinkMsg.newBuilder()
                        .setDeviceData(
                                DeviceDownlinkDataProto.newBuilder()
                                        .setDeviceName(device.getName())
                                        .setDeviceType(device.getType())
                                        .setTbMsg(ByteString.copyFrom(TbMsg.toBytes(msg.getTbMsg())))
                                        .build()
                        )
                        .build())
                .build());
    }

    private void processIntegrationStatistics(IntegrationStatisticsProto data) {
        List<TsKvEntry> statsTs = new ArrayList<>();
        for (TsKvListProto tsKvListProto : data.getPostTelemetryMsg().getTsKvListList()) {
            for (KeyValueProto keyValueProto : tsKvListProto.getKvList()) {
                if (keyValueProto.getType().equals(KeyValueType.LONG_V)) {
                    statsTs.add(new BasicTsKvEntry(tsKvListProto.getTs(), new LongDataEntry(keyValueProto.getKey(), keyValueProto.getLongV())));
                } else if (keyValueProto.getType().equals(KeyValueType.DOUBLE_V)) {
                    statsTs.add(new BasicTsKvEntry(tsKvListProto.getTs(), new DoubleDataEntry(keyValueProto.getKey(), keyValueProto.getDoubleV())));
                } else if (keyValueProto.getType().equals(KeyValueType.BOOLEAN_V)) {
                    statsTs.add(new BasicTsKvEntry(tsKvListProto.getTs(), new BooleanDataEntry(keyValueProto.getKey(), keyValueProto.getBoolV())));
                } else {
                    statsTs.add(new BasicTsKvEntry(tsKvListProto.getTs(), new StringDataEntry(keyValueProto.getKey(), keyValueProto.getStringV())));
                }
            }
        }
        ctx.getTelemetrySubscriptionService().saveAndNotify(configuration.getTenantId(), configuration.getId(), statsTs, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                log.trace("[{}] Persisted statistics telemetry!", configuration.getId());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to persist statistics telemetry!", configuration.getId(), t);
            }
        });
    }
}
