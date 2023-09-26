/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

@Component
@TbCoreComponent
public class UserMsgConstructor {

    public UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user, EdgeVersion edgeVersion) {
        return EdgeVersionUtils.isEdgeProtoDeprecated(edgeVersion)
                ? constructDeprecatedUserUpdatedMsg(msgType, user)
                : UserUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(user))
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits()).build();
    }

    private UserUpdateMsg constructDeprecatedUserUpdatedMsg(UpdateMsgType msgType, User user) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits())
                .setEmail(user.getEmail())
                .setAuthority(user.getAuthority().name());
        if (user.getCustomerId() != null) {
            builder.setCustomerIdMSB(user.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(user.getCustomerId().getId().getLeastSignificantBits());
        }
        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        return builder.build();
    }

    public UserUpdateMsg constructUserDeleteMsg(UserId userId) {
        return UserUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(userId.getId().getMostSignificantBits())
                .setIdLSB(userId.getId().getLeastSignificantBits()).build();
    }

    public UserCredentialsUpdateMsg constructUserCredentialsUpdatedMsg(UserCredentials userCredentials, EdgeVersion edgeVersion) {
        return EdgeVersionUtils.isEdgeProtoDeprecated(edgeVersion)
                ? constructDeprecatedUserCredentialsUpdatedMsg(userCredentials)
                : UserCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(userCredentials)).build();
    }

    private UserCredentialsUpdateMsg constructDeprecatedUserCredentialsUpdatedMsg(UserCredentials userCredentials) {
        UserCredentialsUpdateMsg.Builder builder = UserCredentialsUpdateMsg.newBuilder()
                .setUserIdMSB(userCredentials.getUserId().getId().getMostSignificantBits())
                .setUserIdLSB(userCredentials.getUserId().getId().getLeastSignificantBits())
                .setEnabled(userCredentials.isEnabled())
                .setPassword(userCredentials.getPassword());
        return builder.build();
    }
}
