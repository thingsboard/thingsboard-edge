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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.asset.BaseAssetProcessor;

import java.util.UUID;

@Component
@Slf4j
public class AssetCloudProcessor extends BaseAssetProcessor {

    public ListenableFuture<Void> processAssetMsgFromCloud(TenantId tenantId,
                                                           AssetUpdateMsg assetUpdateMsg,
                                                           Long queueStartTs) throws ThingsboardException {
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, queueStartTs);
                    return requestForAdditionalData(tenantId, assetId, queueStartTs);
                case ENTITY_DELETED_RPC_MESSAGE:
                    if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
                        UUID entityGroupUUID = safeGetUUID(assetUpdateMsg.getEntityGroupIdMSB(),
                                assetUpdateMsg.getEntityGroupIdLSB());
                        EntityGroupId entityGroupId =
                                new EntityGroupId(entityGroupUUID);
                        entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, assetId);
                    } else {
                        Asset assetById = assetService.findAssetById(tenantId, assetId);
                        if (assetById != null) {
                            assetService.deleteAsset(tenantId, assetId);
                            pushAssetDeletedEventToRuleEngine(tenantId, assetById);
                        }
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, Long queueStartTs) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, false);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushAssetCreatedEventToRuleEngine(tenantId, assetId);
        }
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null, null, queueStartTs);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, AssetId assetId) {
        Asset asset = assetService.findAssetById(tenantId, assetId);
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

    public UplinkMsg convertAssetEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        AssetId assetId = new AssetId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ADDED_TO_ENTITY_GROUP:
                Asset asset = assetService.findAssetById(cloudEvent.getTenantId(), assetId);
                if (asset != null) {
                    if (BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
                        log.debug("Skipping TbServiceQueue asset [{}]", cloudEvent);
                    } else {
                        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                        AssetUpdateMsg assetUpdateMsg =
                                assetMsgConstructor.constructAssetUpdatedMsg(msgType, asset, entityGroupId, edgeVersion);
                        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                                .addAssetUpdateMsg(assetUpdateMsg);
                        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                            AssetProfile assetProfile = assetProfileService.findAssetProfileById(cloudEvent.getTenantId(), asset.getAssetProfileId());
                            builder.addAssetProfileUpdateMsg(assetProfileMsgConstructor.constructAssetProfileUpdatedMsg(msgType, assetProfile, edgeVersion));
                        }
                        msg = builder.build();
                    }
                } else {
                    log.debug("Skipping event as asset was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                AssetUpdateMsg assetUpdateMsg =
                        assetMsgConstructor.constructAssetDeleteMsg(assetId, entityGroupId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg).build();
                break;
        }
        return msg;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg, boolean isEdgeVersionDeprecated) {
        CustomerId assignedCustomerId = asset.getCustomerId();
        Customer customer = null;
        if (assignedCustomerId != null) {
            customer = customerService.findCustomerById(tenantId, assignedCustomerId);
        }
        asset.setCustomerId(customer != null ? customer.getId() : null);
    }

}
