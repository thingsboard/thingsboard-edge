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
package org.thingsboard.server.service.edge.rpc.constructor.asset;

import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
public class AssetMsgConstructorV1 extends BaseAssetMsgConstructor {

    @Override
    public AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset, EntityGroupId entityGroupId) {
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(asset.getUuidId().getMostSignificantBits())
                .setIdLSB(asset.getUuidId().getLeastSignificantBits())
                .setName(asset.getName())
                .setType(asset.getType());
        if (asset.getLabel() != null) {
            builder.setLabel(asset.getLabel());
        }
        if (asset.getCustomerId() != null) {
            builder.setCustomerIdMSB(asset.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(asset.getCustomerId().getId().getLeastSignificantBits());
        }
        if (asset.getAssetProfileId() != null) {
            builder.setAssetProfileIdMSB(asset.getAssetProfileId().getId().getMostSignificantBits());
            builder.setAssetProfileIdLSB(asset.getAssetProfileId().getId().getLeastSignificantBits());
        }
        if (asset.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(asset.getAdditionalInfo()));
        }
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    @Override
    public AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, AssetProfile assetProfile) {
        AssetProfileUpdateMsg.Builder builder = AssetProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(assetProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(assetProfile.getId().getId().getLeastSignificantBits())
                .setName(assetProfile.getName())
                .setDefault(assetProfile.isDefault());
        if (assetProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(assetProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(assetProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        if (assetProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(assetProfile.getDefaultQueueName());
        }
        if (assetProfile.getDescription() != null) {
            builder.setDescription(assetProfile.getDescription());
        }
        if (assetProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(assetProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (assetProfile.getDefaultEdgeRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(assetProfile.getDefaultEdgeRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(assetProfile.getDefaultEdgeRuleChainId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }
}
