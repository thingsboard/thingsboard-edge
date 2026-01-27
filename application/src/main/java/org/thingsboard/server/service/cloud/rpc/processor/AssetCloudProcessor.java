/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.asset.BaseAssetProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AssetCloudProcessor extends BaseAssetProcessor {

    public ListenableFuture<Void> processAssetMsgFromCloud(TenantId tenantId, AssetUpdateMsg assetUpdateMsg) {
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateAssetFromCloud(tenantId, assetId, assetUpdateMsg);
                    if (created) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();

                        futures.add(requestForAdditionalData(tenantId, assetId));
                        futures.add(requestForCalculatedFieldData(tenantId, assetId));

                        yield Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                    }
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    Asset assetById = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
                    if (assetById != null) {
                        edgeCtx.getAssetService().deleteAsset(tenantId, assetId);
                        pushAssetDeletedEventToRuleEngine(tenantId, assetById);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private boolean saveOrUpdateAssetFromCloud(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushAssetCreatedEventToRuleEngine(tenantId, assetId);
        }
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null);
        }
        return created;
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, AssetId assetId) {
        Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        pushAssetEventToRuleEngine(tenantId, asset, TbMsgType.ENTITY_CREATED);
    }

    private void pushAssetDeletedEventToRuleEngine(TenantId tenantId, Asset asset) {
        pushAssetEventToRuleEngine(tenantId, asset, TbMsgType.ENTITY_DELETED);
    }

    private void pushAssetEventToRuleEngine(TenantId tenantId, Asset asset, TbMsgType msgType) {
        try {
            String assetAsString = JacksonUtil.toString(asset);
            pushEntityEventToRuleEngine(tenantId, asset.getId(), asset.getCustomerId(), msgType, assetAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push asset action to rule engine: {}", tenantId, asset.getId(), msgType.name(), e);
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        AssetId assetId = new AssetId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Asset asset = edgeCtx.getAssetService().findAssetById(cloudEvent.getTenantId(), assetId);
                if (asset != null) {
                    if (BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
                        log.debug("Skipping TbServiceQueue asset [{}]", cloudEvent);
                    } else {
                        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                        AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetUpdatedMsg(msgType, asset);
                        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                                .addAssetUpdateMsg(assetUpdateMsg);
                        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                            AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(cloudEvent.getTenantId(), asset.getAssetProfileId());
                            builder.addAssetProfileUpdateMsg(EdgeMsgConstructorUtils.constructAssetProfileUpdatedMsg(msgType, assetProfile));
                        }
                        return builder.build();
                    }
                } else {
                    log.debug("Skipping event as asset was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetDeleteMsg(assetId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg) {
        CustomerId assignedCustomerId = asset.getCustomerId();
        Customer customer = null;
        if (assignedCustomerId != null) {
            customer = edgeCtx.getCustomerService().findCustomerById(tenantId, assignedCustomerId);
        }
        asset.setCustomerId(customer != null ? customer.getId() : null);
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.ASSET;
    }

}
