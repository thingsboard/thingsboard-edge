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
package org.thingsboard.server.service.edge.rpc.constructor;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.gen.edge.v1.EdgeEntityType;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

@Component
@TbCoreComponent
public class EntityViewMsgConstructor {

    public EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView, EntityGroupId entityGroupId, EdgeVersion edgeVersion) {
        if (EdgeVersionUtils.isEdgeVersionOlderThan_3_6_2(edgeVersion)) {
            return constructDeprecatedEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
        }
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(entityView))
                .setIdMSB(entityView.getId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    private EntityViewUpdateMsg constructDeprecatedEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView, EntityGroupId entityGroupId) {
        EdgeEntityType edgeEntityType = checkEntityType(entityView.getEntityId().getEntityType());
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(entityView.getId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getId().getId().getLeastSignificantBits())
                .setName(entityView.getName())
                .setType(entityView.getType())
                .setEntityIdMSB(entityView.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(entityView.getEntityId().getId().getLeastSignificantBits())
                .setEntityType(edgeEntityType);
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        if (entityView.getCustomerId() != null) {
            builder.setCustomerIdMSB(entityView.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(entityView.getCustomerId().getId().getLeastSignificantBits());
        }
        if (entityView.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(entityView.getAdditionalInfo()));
        }
        return builder.build();
    }

    private EdgeEntityType checkEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEntityType.DEVICE;
            case ASSET:
                return EdgeEntityType.ASSET;
            default:
                throw new RuntimeException("Unsupported entity type [" + entityType + "]");
        }
    }

    public EntityViewUpdateMsg constructEntityViewDeleteMsg(EntityViewId entityViewId, EntityGroupId entityGroupId) {
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(entityViewId.getId().getMostSignificantBits())
                .setIdLSB(entityViewId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }
}
