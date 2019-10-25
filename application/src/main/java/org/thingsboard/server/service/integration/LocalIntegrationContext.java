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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.transport.SessionInfoProto;
import org.thingsboard.server.utils.JsonUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_TELEMETRY_REQUEST;

@Data
@Slf4j
public class LocalIntegrationContext implements IntegrationContext {

    private static final String DEVICE_VIEW_NAME_ENDING = "_View";
    private static final ReentrantLock entityCreationLock = new ReentrantLock();

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
        Device device = getOrCreateDevice(data.getDeviceName(), data.getDeviceType(), data.getCustomerName(), data.getGroupName());

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
    public void processUplinkData(AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        Asset asset = getOrCreateAsset(data.getAssetName(), data.getAssetType(), data.getCustomerName(), data.getGroupName());

        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData();
        tbMsgMetaData.putValue("assetName", data.getAssetName());
        tbMsgMetaData.putValue("assetType", data.getAssetType());

        if (data.hasPostTelemetryMsg()) {
            data.getPostTelemetryMsg().getTsKvListList()
                    .forEach(tsKvListProto -> {
                        JsonObject json = JsonUtils.getJsonObject(tsKvListProto.getKvList());
                        TbMsg tbMsg = new TbMsg(
                                UUIDs.timeBased(),
                                POST_TELEMETRY_REQUEST.name(),
                                asset.getId(),
                                tbMsgMetaData,
                                TbMsgDataType.JSON,
                                gson.toJson(json),
                                null,
                                null,
                                0L);

                        ctx.getActorService().onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(asset.getTenantId(), tbMsg)));
                    });
        }

        if (data.hasPostAttributesMsg()) {
            JsonObject json = JsonUtils.getJsonObject(data.getPostAttributesMsg().getKvList());
            TbMsg tbMsg = new TbMsg(
                    UUIDs.timeBased(),
                    POST_ATTRIBUTES_REQUEST.name(),
                    asset.getId(),
                    tbMsgMetaData,
                    TbMsgDataType.JSON,
                    gson.toJson(json),
                    null,
                    null,
                    0L);

            ctx.getActorService().onMsg(new SendToClusterMsg(tbMsg.getOriginator(), new ServiceToRuleEngineMsg(asset.getTenantId(), tbMsg)));
        }
    }

    @Override
    public void createEntityView(EntityViewDataProto data, IntegrationCallback<Void> callback) {
        createEntityViewForDeviceIfAbsent(getOrCreateDevice(data.getDeviceName(), data.getDeviceType(), null, null), data);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        ctx.getActorService().onMsg(new SendToClusterMsg(this.configuration.getId(), new ServiceToRuleEngineMsg(this.configuration.getTenantId(), msg)));
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

    private Device getOrCreateDevice(String deviceName, String deviceType, String customerName, String groupName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateDevice(deviceName, deviceType, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(String deviceName, String deviceType, String customerName, String entityGroupName) {
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
            EntityId parentId = StringUtils.isEmpty(customerName) ? device.getTenantId() : device.getCustomerId();
            if (!StringUtils.isEmpty(entityGroupName)) {
                addEntityToEntityGroup(entityGroupName, configuration.getTenantId(), device.getId(), parentId, device.getEntityType());
            }

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

    private Asset getOrCreateAsset(String assetName, String assetType, String customerName, String groupName) {
        Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(configuration.getTenantId(), assetName);
        if (asset == null) {
            entityCreationLock.lock();
            try {
                return processGetOrCreateAsset(assetName, assetType, customerName, groupName);
            } finally {
                entityCreationLock.unlock();
            }
        }
        return asset;
    }

    private Asset processGetOrCreateAsset(String assetName, String assetType, String customerName, String groupName) {
        Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(configuration.getTenantId(), assetName);
        if (asset == null) {
            asset = new Asset();
            asset.setName(assetName);
            asset.setType(assetType);
            asset.setTenantId(configuration.getTenantId());
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
                asset.setCustomerId(customer.getId());
            }
            asset = ctx.getAssetService().saveAsset(asset);

            EntityId parentId = StringUtils.isEmpty(customerName) ? asset.getTenantId() : asset.getCustomerId();
            if (!StringUtils.isEmpty(groupName)) {
                addEntityToEntityGroup(groupName, configuration.getTenantId(), asset.getId(), parentId, asset.getEntityType());
            }

            createRelationFromIntegration(asset.getId());
            pushAssetCreatedEventToRuleEngine(asset);
        }
        return asset;
    }

    private void addEntityToEntityGroup(String groupName, TenantId tenantId, EntityId entityId, EntityId parentId, EntityType entityType) {
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = ctx.getEntityGroupService()
                .findEntityGroupByTypeAndName(tenantId, parentId, entityType, groupName);

        DonAsynchron.withCallback(futureEntityGroup, optionalEntityGroup -> {
            EntityGroup entityGroup =
                    optionalEntityGroup.orElseGet(() -> createEntityGroup(groupName, parentId, entityType, tenantId));

            pushEntityGroupCreatedEventToRuleEngine(entityGroup);

            ctx.getEntityGroupService().addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);

        }, throwable -> {
        }, Executors.newSingleThreadExecutor());
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, EntityType entityType, TenantId tenantId) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(entityType);
        return ctx.getEntityGroupService().saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    private void createRelationFromIntegration(EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(configuration.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        ctx.getRelationService().saveRelation(configuration.getTenantId(), relation);
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

    private void pushAssetCreatedEventToRuleEngine(Asset asset) {
        try {
            ObjectNode entityNode = mapper.valueToTree(asset);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, asset.getId(), assetActionTbMsgMetaData(asset), mapper.writeValueAsString(entityNode), null, null, 0L);
            ctx.getActorService().onMsg(new SendToClusterMsg(asset.getId(), new ServiceToRuleEngineMsg(configuration.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push asset action to rule engine: {}", asset.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private void pushEntityGroupCreatedEventToRuleEngine(EntityGroup entityGroup) {
        try {
            ObjectNode entityNode = mapper.valueToTree(entityGroup);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, entityGroup.getId(), entityGroupActionTbMsgMetaData(), mapper.writeValueAsString(entityNode), null, null, 0L);
            ctx.getActorService().onMsg(new SendToClusterMsg(entityGroup.getId(), new ServiceToRuleEngineMsg(configuration.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push entityGroup action to rule engine: {}", entityGroup.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(Device device) {
        return getActionTbMsgMetaData(device.getCustomerId());
    }

    private TbMsgMetaData assetActionTbMsgMetaData(Asset asset) {
        return getActionTbMsgMetaData(asset.getCustomerId());
    }

    private TbMsgMetaData entityGroupActionTbMsgMetaData() {
        return getTbMsgMetaData();
    }

    private TbMsgMetaData getActionTbMsgMetaData(CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData();
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
    public ServerAddress getServerAddress() {
        return ctx.getDiscoveryService().getCurrentServer().getServerAddress();
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