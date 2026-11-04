// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
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

    public ListenableFuture<Void> processAssetMsgFromCloud(TenantId tenantId, AssetUpdateMsg assetUpdateMsg) throws ThingsboardException {
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    Pair<Boolean, Boolean> limitReachedCreated = saveOrUpdateAssetFromCloud(tenantId, assetId, assetUpdateMsg);
                    boolean limitReached = limitReachedCreated.getFirst();
                    if (limitReached) {
                        yield Futures.immediateFuture(null);
                    }
                    boolean created = limitReachedCreated.getSecond();
                    if (created) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();

                        futures.add(requestForAdditionalData(tenantId, assetId));
                        futures.add(requestForCalculatedFieldData(tenantId, assetId));

                        yield Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                    }
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    assetCreationLock.lock();
                    try {
                        if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
                            UUID entityGroupUUID = safeGetUUID(assetUpdateMsg.getEntityGroupIdMSB(),
                                    assetUpdateMsg.getEntityGroupIdLSB());
                            EntityGroupId entityGroupId =
                                    new EntityGroupId(entityGroupUUID);
                            edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, assetId);
                            yield removeEntityIfInSingleAllGroup(tenantId, assetId, () -> edgeCtx.getAssetService().deleteAsset(tenantId, assetId));
                        } else {
                            deleteAsset(tenantId, assetId);
                        }
                        yield Futures.immediateFuture(null);
                    } finally {
                        assetCreationLock.unlock();
                    }
                }
                default -> handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private Pair<Boolean, Boolean> saveOrUpdateAssetFromCloud(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) throws ThingsboardException {
        boolean created;
        try {
            Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
            created = resultPair.getFirst();
            if (created) {
                pushAssetCreatedEventToRuleEngine(tenantId, assetId);
            }
            Boolean assetNameUpdated = resultPair.getSecond();
            if (assetNameUpdated) {
                cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null, null);
            }
        } catch (SubscriptionException e) {
            log.error("[{}][{}] Subscription error occurred for msg {}", tenantId, assetId, assetUpdateMsg, e);
            if (e.getMessage().contains("limit reached")) {
                return Pair.of(true, false);
            } else {
                throw e;
            }
        }
        return Pair.of(false, created);
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, AssetId assetId) {
        Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        pushEntityEventToRuleEngine(tenantId, asset, TbMsgType.ENTITY_CREATED);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        AssetId assetId = new AssetId(cloudEvent.getEntityId());
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ADDED_TO_ENTITY_GROUP -> {
                Asset asset = edgeCtx.getAssetService().findAssetById(cloudEvent.getTenantId(), assetId);
                if (asset != null) {
                    if (BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
                        log.debug("Skipping TbServiceQueue asset [{}]", cloudEvent);
                    } else {
                        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                        AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetUpdatedMsg(msgType, asset, entityGroupId);
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
            case DELETED, REMOVED_FROM_ENTITY_GROUP -> {
                AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetDeleteMsg(assetId, entityGroupId);
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
