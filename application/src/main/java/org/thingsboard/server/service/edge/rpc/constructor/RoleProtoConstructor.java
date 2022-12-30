/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

@Component
@Slf4j
public class RoleProtoConstructor {

    public RoleProto constructRoleProto(UpdateMsgType msgType, Role role) {
        RoleProto.Builder builder = RoleProto.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(role.getId().getId().getMostSignificantBits())
                .setIdLSB(role.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(role.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(role.getTenantId().getId().getLeastSignificantBits())
                .setName(role.getName())
                .setType(role.getType().name())
                .setPermissions(JacksonUtil.toString(role.getPermissions()));
        if (role.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(role.getAdditionalInfo()));
        }
        if (role.getCustomerId() != null) {
            builder.setCustomerIdMSB(role.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(role.getCustomerId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    public RoleProto constructRoleDeleteMsg(RoleId roleId) {
        return RoleProto.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(roleId.getId().getMostSignificantBits())
                .setIdLSB(roleId.getId().getLeastSignificantBits()).build();
    }
}
