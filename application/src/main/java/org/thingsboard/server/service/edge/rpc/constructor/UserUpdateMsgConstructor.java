/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserUpdateMsg;

@Component
@Slf4j
public class UserUpdateMsgConstructor {

    @Autowired
    private UserService userService;

    public UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits())
                .setEmail(user.getEmail())
                .setAuthority(user.getAuthority().name())
                .setEnabled(false);
        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        if (msgType.equals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE) ||
                msgType.equals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE)) {
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
            if (userCredentials != null) {
                builder.setEnabled(userCredentials.isEnabled());
                if (userCredentials.getPassword() != null) {
                    builder.setPassword(userCredentials.getPassword());
                }
            }
        }
        return builder.build();
    }

    public UserUpdateMsg constructUserDeleteMsg(UserId userId) {
        return UserUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(userId.getId().getMostSignificantBits())
                .setIdLSB(userId.getId().getLeastSignificantBits()).build();
    }
}
