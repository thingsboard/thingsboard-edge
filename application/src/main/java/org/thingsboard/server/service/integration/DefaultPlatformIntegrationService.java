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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.util.limits.LimitedApi;
import org.thingsboard.server.dao.util.limits.RateLimitService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.DefaultTbAssetProfileCache;
import org.thingsboard.server.service.profile.DefaultTbDeviceProfileCache;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 02.12.17.
 */
@Slf4j
@TbCoreComponent
@Service
@Data
public class DefaultPlatformIntegrationService implements PlatformIntegrationService {

    private static final ReentrantLock entityCreationLock = new ReentrantLock();

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private DataConverterService dataConverterService;

    @Autowired
    protected IntegrationContextComponent contextComponent;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private EventService eventService;

    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Autowired
    @Lazy
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Lazy
    private ActorSystemContext actorContext;

    @Autowired
    private TelemetrySubscriptionService telemetrySubscriptionService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RemoteIntegrationRpcService remoteIntegrationRpcService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceStateService deviceStateService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private DbCallbackExecutorService callbackExecutorService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Autowired
    private DefaultTbDeviceProfileCache deviceProfileCache;

    @Autowired
    private DefaultTbAssetProfileCache assetProfileCache;

    @Autowired
    private RateLimitService rateLimitService;

    @Value("${integrations.reinit.enabled:false}")
    private boolean reinitEnabled;

    @Value("${integrations.reinit.frequency:3600000}")
    private long reinitFrequency;

    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    @Value("${integrations.statistics.persist_frequency}")
    private long statisticsPersistFrequency;

    @Value("${integrations.allow_Local_network_hosts:true}")
    private boolean allowLocalNetworkHosts;

    private ExecutorService callbackExecutor;

    private final Gson gson = new Gson();
    private volatile Set<TopicPartitionInfo> myPartitions = ConcurrentHashMap.newKeySet();

    private boolean initialized;

    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> integrationRuleEngineMsgProducer;

    @PostConstruct
    public void init() {
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
        integrationRuleEngineMsgProducer = producerProvider.getIntegrationRuleEngineMsgProducer();
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(20, "default-integration-callback");
    }

    @PreDestroy
    public void destroy() {
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
    }

    @Override
    public void processUplinkData(AbstractIntegration integration, DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        Device device = getOrCreateDevice(integration, data.getDeviceName(), data.getDeviceType(), data.getDeviceLabel(), data.getCustomerName(), data.getGroupName());

        UUID sessionId = integration.getId().getId(); //for local integration context sessionId is exact integrationId
        TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());

        if (device.getCustomerId() != null && !device.getCustomerId().isNullUid()) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }

        TransportProtos.SessionInfoProto sessionInfo = builder.build();

        if (data.hasPostTelemetryMsg()) {
            process(sessionInfo, data.getPostTelemetryMsg(), callback);
        }

        if (data.hasPostAttributesMsg()) {
            process(sessionInfo, data.getPostAttributesMsg(), callback);
        }
    }

    @Override
    public void processUplinkData(AbstractIntegration configuration, AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        Asset asset = getOrCreateAsset(configuration, data.getAssetName(), data.getAssetType(), data.getAssetLabel(), data.getCustomerName(), data.getGroupName());

        if (data.hasPostTelemetryMsg()) {
            process(asset, data.getPostTelemetryMsg(), callback);
        }

        if (data.hasPostAttributesMsg()) {
            process(asset, data.getPostAttributesMsg(), callback);
        }
    }

    @Override
    public void processUplinkData(AbstractIntegration integrationInfo, EntityViewDataProto data, IntegrationCallback<Void> callback) {
        Device device = getOrCreateDevice(integrationInfo, data.getDeviceName(), data.getDeviceType(), null, null, null);
        getOrCreateEntityView(integrationInfo, device, data);
        callback.onSuccess(null);
    }

    @Override
    public void processUplinkData(AbstractIntegration info, TbMsg data, IntegrationApiCallback callback) {
        process(info.getTenantId(), data, callback);
    }

    @Override
    public void processUplinkData(TbIntegrationEventProto data, IntegrationApiCallback callback) {
        TenantId tenantId = new TenantId(new UUID(data.getTenantIdMSB(), data.getTenantIdLSB()));
        var eventSource = data.getSource();
        EntityId entityid = null;
        switch (eventSource) {
            case DEVICE:
                Device device = deviceService.findDeviceByTenantIdAndName(tenantId, data.getDeviceName());
                if (device != null) {
                    entityid = device.getId();
                }
                break;
            case INTEGRATION:
                entityid = new IntegrationId(new UUID(data.getEventSourceIdMSB(), data.getEventSourceIdLSB()));
                break;
            case UPLINK_CONVERTER:
            case DOWNLINK_CONVERTER:
                entityid = new ConverterId(new UUID(data.getEventSourceIdMSB(), data.getEventSourceIdLSB()));
                break;
        }
        if (entityid != null) {
            saveEvent(tenantId, entityid, data, callback);
        } else {
            callback.onSuccess(null);
        }
    }

    @Override
    public void processUplinkData(TbIntegrationTsDataProto data, IntegrationApiCallback integrationApiCallback) {
        TenantId tenantId = new TenantId(new UUID(data.getTenantIdMSB(), data.getTenantIdLSB()));
        var eventSource = data.getSource();
        EntityId entityid;
        switch (eventSource) {
            case INTEGRATION:
                entityid = new IntegrationId(new UUID(data.getEntityIdMSB(), data.getEntityIdLSB()));
                break;
            case UPLINK_CONVERTER:
            case DOWNLINK_CONVERTER:
                entityid = new ConverterId(new UUID(data.getEntityIdMSB(), data.getEntityIdLSB()));
                break;
            default:
                throw new RuntimeException("Not supported!");
        }

        List<TsKvEntry> statistics = KvProtoUtil.toTsKvEntityList(data.getTsDataList());
        telemetrySubscriptionService.saveAndNotifyInternal(tenantId, entityid, statistics, new FutureCallback<>() {
            @Override
            public void onSuccess(Integer result) {
                log.trace("[{}] Persisted statistics telemetry: {}", entityid, statistics);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to persist statistics telemetry: {}", entityid, statistics, t);
            }
        });
    }

    private void saveEvent(TenantId tenantId, EntityId entityId, TbIntegrationEventProto proto, IntegrationApiCallback callback) {
        try {
            Event event = JavaSerDesUtil.decode(proto.getEvent().toByteArray());
            event.setTenantId(tenantId);
            event.setEntityId(entityId.getId());

            ListenableFuture<Void> saveEventFuture = eventService.saveAsync(event);

            if (entityId.getEntityType().equals(EntityType.INTEGRATION) && event.getType().equals(EventType.LC_EVENT)) {
                LifecycleEvent lcEvent = (LifecycleEvent) event;

                String key = "integration_status_" + event.getServiceId().toLowerCase();

                if (lcEvent.getLcEventType().equals("STARTED") || lcEvent.getLcEventType().equals("UPDATED")) {
                    ObjectNode value = JacksonUtil.newObjectNode();

                    if (lcEvent.isSuccess()) {
                        value.put("success", true);
                    } else {
                        value.put("success", false);
                        value.put("serviceId", lcEvent.getServiceId());
                        value.put("error", lcEvent.getError());
                    }

                    AttributeKvEntry attr = new BaseAttributeKvEntry(new JsonDataEntry(key, JacksonUtil.toString(value)), event.getCreatedTime());

                    saveEventFuture = Futures.transformAsync(saveEventFuture, v -> {
                        attributesService.save(tenantId, entityId, "SERVER_SCOPE", Collections.singletonList(attr));
                        return null;
                    }, MoreExecutors.directExecutor());
                } else if (lcEvent.getLcEventType().equals("STOPPED")){
                    saveEventFuture = Futures.transformAsync(saveEventFuture, v -> {
                        attributesService.removeAll(tenantId, entityId, "SERVER_SCOPE", Collections.singletonList(key));
                        return null;
                    }, MoreExecutors.directExecutor());
                }
            }

            DonAsynchron.withCallback(saveEventFuture, callback::onSuccess, callback::onError);
        } catch (Exception t) {
            log.error("[{}][{}][{}] Failed to save event!", tenantId, entityId, proto.getEvent(), t);
            callback.onError(t);
            throw t;
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivity(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            int dataPoints = 0;
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                dataPoints += tsKv.getKvCount();
            }
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("deviceName", sessionInfo.getDeviceName());
                metaData.putValue("deviceType", sessionInfo.getDeviceType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, SessionMsgType.POST_TELEMETRY_REQUEST, packCallback);
            }
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivity(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, SessionMsgType.POST_ATTRIBUTES_REQUEST,
                    new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), msg.getKvList().size(), callback)));
        }
    }

    @Override
    public void process(TenantId tenantId, TbMsg tbMsg, IntegrationCallback<Void> callback) {
        sendToRuleEngine(tenantId, tbMsg, new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, tbMsg.getCustomerId(), 1, callback)));
    }


    @Override
    public Device getOrCreateDevice(AbstractIntegration integration, String deviceName, String deviceType, String deviceLabel, String customerName, String groupName) {
        Device device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), deviceName);
        if (device == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateDevice(integration, deviceName, deviceType, deviceLabel, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return device;
    }

    @Override
    public Asset getOrCreateAsset(AbstractIntegration integration, String assetName, String assetType, String assetLabel, String customerName, String groupName) {
        Asset asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), assetName);
        if (asset == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateAsset(integration, assetName, assetType, assetLabel, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return asset;
    }

    @Override
    public EntityView getOrCreateEntityView(AbstractIntegration configuration, Device device, EntityViewDataProto proto) {
        String entityViewName = proto.getViewName();
        EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
        if (entityView == null) {
            entityCreationLock.lock();
            try {
                entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
                if (entityView == null) {
                    entityView = new EntityView();
                    entityView.setName(entityViewName);
                    entityView.setType(proto.getViewType());
                    entityView.setTenantId(configuration.getTenantId());
                    entityView.setEntityId(device.getId());

                    TelemetryEntityView telemetryEntityView = new TelemetryEntityView();
                    telemetryEntityView.setTimeseries(proto.getTelemetryKeysList());
                    entityView.setKeys(telemetryEntityView);

                    entityView = entityViewService.saveEntityView(entityView);
                    createRelationFromIntegration(configuration, entityView.getId());
                }
            } finally {
                entityCreationLock.unlock();
            }
        }
        return entityView;
    }

    private Device processGetOrCreateDevice(AbstractIntegration integration, String deviceName, String deviceType, String deviceLabel, String customerName, String groupName) {
        Device device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), deviceName);
        if (device == null && integration.isAllowCreateDevicesOrAssets()) {
            device = new Device();
            device.setName(deviceName);
            device.setType(deviceType);
            device.setTenantId(integration.getTenantId());
            if (!StringUtils.isEmpty(deviceLabel)) {
                device.setLabel(deviceLabel);
            }
            if (!StringUtils.isEmpty(customerName)) {
                Customer customer = getOrCreateCustomer(integration, customerName);
                device.setCustomerId(customer.getId());
            }

            device = deviceService.saveDevice(device);

            if (!StringUtils.isEmpty(groupName)) {
                addEntityToEntityGroup(groupName, integration, device.getId(), device.getOwnerId(), device.getEntityType());
            }

            createRelationFromIntegration(integration, device.getId());
            clusterService.onDeviceUpdated(device, null);
            pushDeviceCreatedEventToRuleEngine(integration, device);
        } else {
            throw new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return device;
    }

    private Asset processGetOrCreateAsset(AbstractIntegration integration, String assetName, String assetType, String assetLabel, String customerName, String groupName) {
        Asset asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), assetName);
        if (asset == null && integration.isAllowCreateDevicesOrAssets()) {
            asset = new Asset();
            asset.setName(assetName);
            asset.setType(assetType);
            asset.setTenantId(integration.getTenantId());
            if (!StringUtils.isEmpty(assetLabel)) {
                asset.setLabel(assetLabel);
            }
            if (!StringUtils.isEmpty(customerName)) {
                Customer customer = getOrCreateCustomer(integration, customerName);
                asset.setCustomerId(customer.getId());
            }
            asset = assetService.saveAsset(asset);

            if (!StringUtils.isEmpty(groupName)) {
                addEntityToEntityGroup(groupName, integration, asset.getId(), asset.getOwnerId(), asset.getEntityType());
            }

            createRelationFromIntegration(integration, asset.getId());
            pushAssetCreatedEventToRuleEngine(integration, asset);
        } else {
            throw new ThingsboardRuntimeException("Creating assets is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return asset;
    }

    private Customer getOrCreateCustomer(AbstractIntegration integration, String customerName) {
        Customer customer;
        Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(integration.getTenantId(), customerName);
        if (customerOptional.isPresent()) {
            customer = customerOptional.get();
        } else {
            customer = new Customer();
            customer.setTitle(customerName);
            customer.setTenantId(integration.getTenantId());
            customer = customerService.saveCustomer(customer);
            pushCustomerCreatedEventToRuleEngine(integration, customer);
        }
        return customer;
    }

    private void addEntityToEntityGroup(String groupName, AbstractIntegration integration, EntityId entityId, EntityId parentId, EntityType entityType) {
        TenantId tenantId = integration.getTenantId();
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = entityGroupService
                .findEntityGroupByTypeAndNameAsync(tenantId, parentId, entityType, groupName);

        DonAsynchron.withCallback(futureEntityGroup, optionalEntityGroup -> {
            EntityGroup entityGroup =
                    optionalEntityGroup.orElseGet(() -> createEntityGroup(groupName, parentId, entityType, tenantId));
            pushEntityGroupCreatedEventToRuleEngine(integration, entityGroup);
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);
        }, throwable -> log.warn("[{}][{}] Failed to find entity group: {}:{}", tenantId, parentId, entityType, groupName, throwable), callbackExecutorService);
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, EntityType entityType, TenantId tenantId) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(entityType);
        return entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    private void createRelationFromIntegration(AbstractIntegration integration, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(integration.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        relationService.saveRelation(integration.getTenantId(), relation);
    }

    private void pushDeviceCreatedEventToRuleEngine(AbstractIntegration integration, Device device) {
        try {
            DeviceProfile deviceProfile = deviceProfileCache.find(device.getDeviceProfileId());
            RuleChainId ruleChainId;
            String queueName;

            if (deviceProfile == null) {
                ruleChainId = null;
                queueName = null;
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                queueName = deviceProfile.getDefaultQueueName();
            }

            JsonNode entityNode = JacksonUtil.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(queueName, DataConstants.ENTITY_CREATED, device.getId(), deviceActionTbMsgMetaData(integration, device),
                    JacksonUtil.toString(entityNode), ruleChainId, null);

            process(device.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(AbstractIntegration integration, Asset asset) {
        try {
            AssetProfile assetProfile = assetProfileCache.find(asset.getAssetProfileId());
            RuleChainId ruleChainId;
            String queueName;
            if (assetProfile == null) {
                ruleChainId = null;
                queueName = null;
            } else {
                ruleChainId = assetProfile.getDefaultRuleChainId();
                queueName = assetProfile.getDefaultQueueName();
            }
            JsonNode entityNode = JacksonUtil.valueToTree(asset);
            TbMsg tbMsg = TbMsg.newMsg(queueName, DataConstants.ENTITY_CREATED, asset.getId(), asset.getCustomerId(), assetActionTbMsgMetaData(integration, asset),
                    JacksonUtil.toString(entityNode), ruleChainId, null);
            process(integration.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push asset action to rule engine: {}", asset.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }


    private void pushEntityGroupCreatedEventToRuleEngine(AbstractIntegration integration, EntityGroup entityGroup) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(entityGroup);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, entityGroup.getId(), getTbMsgMetaData(integration), JacksonUtil.toString(entityNode));
            process(integration.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push entityGroup action to rule engine: {}", entityGroup.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushCustomerCreatedEventToRuleEngine(AbstractIntegration integration, Customer customer) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(customer);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, customer.getId(), customer.getParentCustomerId(), getTbMsgMetaData(integration), JacksonUtil.toString(entityNode));
            process(customer.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push customer action to rule engine: {}", customer.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(AbstractIntegration integration, Device device) {
        return getActionTbMsgMetaData(integration, device.getCustomerId());
    }

    private TbMsgMetaData assetActionTbMsgMetaData(AbstractIntegration integration, Asset asset) {
        return getActionTbMsgMetaData(integration, asset.getCustomerId());
    }

    private TbMsgMetaData getActionTbMsgMetaData(AbstractIntegration integration, CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(integration);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData getTbMsgMetaData(AbstractIntegration integration) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", integration.getId().toString());
        metaData.putValue("integrationName", integration.getName());
        return metaData;
    }

    private void reportActivity(SessionInfoProto sessionInfo) {
        TransportProtos.SubscriptionInfoProto subscriptionInfoProto = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(false).setRpcSubscription(false)
                .setLastActivityTime(System.currentTimeMillis()).build();
        TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                .setSubscriptionInfo(subscriptionInfoProto).build();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, getTenantId(sessionInfo), getDeviceId(sessionInfo));
        tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(getRoutingKey(sessionInfo),
                TransportProtos.ToCoreMsg.newBuilder().setToDeviceActorMsg(msg).build()), null);
    }

    private void process(Asset asset, PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        int dataPoints = 0;
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            dataPoints += tsKv.getKvCount();
        }
        MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(asset.getTenantId(), asset.getCustomerId(), dataPoints, callback));
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("assetName", asset.getName());
            metaData.putValue("assetType", asset.getType());
            metaData.putValue("ts", tsKv.getTs() + "");
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            sendToRuleEngine(asset.getTenantId(), asset.getId(), asset.getAssetProfileId(),
                    asset.getCustomerId(), json, metaData, SessionMsgType.POST_TELEMETRY_REQUEST, packCallback);
        }
    }

    private void process(Asset asset, PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assetName", asset.getName());
        metaData.putValue("assetType", asset.getType());
        sendToRuleEngine(asset.getTenantId(), asset.getId(), asset.getAssetProfileId(),
                asset.getCustomerId(), json, metaData, SessionMsgType.POST_ATTRIBUTES_REQUEST,
                     new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(asset.getTenantId(), asset.getCustomerId(), msg.getKvList().size(), callback)));
    }

    private void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, TransportProtos.SessionInfoProto sessionInfo, JsonObject json,
                                  TbMsgMetaData metaData, SessionMsgType sessionMsgType, TbQueueCallback callback) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));

        DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, deviceProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (deviceProfile == null) {
            log.warn("[{}] Device profile is null!", deviceProfileId);
            ruleChainId = null;
            queueName = null;
        } else {
            ruleChainId = deviceProfile.getDefaultRuleChainId();
            queueName = deviceProfile.getDefaultQueueName();
        }

        TbMsg tbMsg = TbMsg.newMsg(queueName, sessionMsgType.name(), deviceId, getCustomerId(sessionInfo), metaData, gson.toJson(json), ruleChainId, null);

        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private void sendToRuleEngine(TenantId tenantId, AssetId assetId, AssetProfileId assetProfileId, CustomerId customerId, JsonObject json,
                                  TbMsgMetaData metaData, SessionMsgType sessionMsgType, TbQueueCallback callback) {
        AssetProfile assetProfile = assetProfileCache.get(tenantId, assetProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (assetProfile == null) {
            log.warn("[{}] Asset profile is null!", assetProfileId);
            ruleChainId = null;
            queueName = null;
        } else {
            ruleChainId = assetProfile.getDefaultRuleChainId();
            queueName = assetProfile.getDefaultQueueName();
        }

        TbMsg tbMsg = TbMsg.newMsg(queueName, sessionMsgType.name(), assetId, customerId, metaData, gson.toJson(json), ruleChainId, null);

        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        integrationRuleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    protected UUID getRoutingKey(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
    }

    protected TenantId getTenantId(TransportProtos.SessionInfoProto sessionInfo) {
        return new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
    }

    protected DeviceId getDeviceId(TransportProtos.SessionInfoProto sessionInfo) {
        return new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
    }

    private boolean checkLimits(SessionInfoProto sessionInfo, Object msg, IntegrationCallback<Void> callback) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toId(sessionInfo), msg);
        }
        TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS, tenantId)) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.TENANT));
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Tenant level rate limit detected: {}", toId(sessionInfo), tenantId, msg);
            }
            return false;
        }
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS, tenantId, deviceId)) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.DEVICE));
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Device level rate limit detected: {}", toId(sessionInfo), deviceId, msg);
            }
            return false;
        }
        return true;
    }

    private UUID toId(SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private class IntegrationTbQueueCallback implements TbQueueCallback {
        private final IntegrationCallback<Void> callback;

        private IntegrationTbQueueCallback(IntegrationCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            });
        }

        @Override
        public void onFailure(Throwable t) {
            DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                if (callback != null) {
                    callback.onError(t);
                }
            });
        }
    }

    private class MsgPackCallback implements TbQueueCallback {
        private final AtomicInteger msgCount;
        private final IntegrationCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, IntegrationCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0 && callback != null) {
                DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> callback.onSuccess(null));
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (callback != null) {
                callback.onError(t);
            }
        }
    }

    private class ApiStatsProxyCallback<T> implements IntegrationCallback<T> {
        private final TenantId tenantId;
        private final CustomerId customerId;
        private final int dataPoints;
        private final IntegrationCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, CustomerId customerId, int dataPoints, IntegrationCallback<T> callback) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                if (callback != null) {
                    callback.onSuccess(msg);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }


    private static CustomerId getCustomerId(SessionInfoProto sessionInfo) {
        CustomerId customerId;
        if (sessionInfo.getCustomerIdMSB() > 0 && sessionInfo.getCustomerIdLSB() > 0) {
            customerId = new CustomerId(new UUID(sessionInfo.getCustomerIdMSB(), sessionInfo.getCustomerIdLSB()));
        } else {
            customerId = null;
        }
        return customerId;
    }
}
