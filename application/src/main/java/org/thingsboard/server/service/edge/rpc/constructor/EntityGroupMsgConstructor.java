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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;


@Component
@Slf4j
public class EntityGroupMsgConstructor {

    public EntityGroupUpdateMsg constructEntityGroupUpdatedMsg(UpdateMsgType msgType, EntityGroup entityGroup) {
        EntityGroupUpdateMsg.Builder builder = EntityGroupUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(entityGroup.getId().getId().getMostSignificantBits())
                .setIdLSB(entityGroup.getId().getId().getLeastSignificantBits())
                .setName(entityGroup.getName())
                .setType(entityGroup.getType().name())
                .setOwnerIdMSB(entityGroup.getOwnerId().getId().getMostSignificantBits())
                .setOwnerIdLSB(entityGroup.getOwnerId().getId().getLeastSignificantBits())
                .setOwnerEntityType(entityGroup.getOwnerId().getEntityType().name())
                .setConfiguration(JacksonUtil.toString(entityGroup.getConfiguration()));
        if (entityGroup.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(entityGroup.getAdditionalInfo()));
        }
        return builder.build();
    }

    public EntityGroupUpdateMsg constructEntityGroupDeleteMsg(EntityGroupId entityGroupId) {
        return EntityGroupUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(entityGroupId.getId().getMostSignificantBits())
                .setIdLSB(entityGroupId.getId().getLeastSignificantBits()).build();
    }
}
