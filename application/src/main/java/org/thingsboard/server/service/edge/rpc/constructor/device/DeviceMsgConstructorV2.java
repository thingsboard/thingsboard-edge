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
package org.thingsboard.server.service.edge.rpc.constructor.device;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class DeviceMsgConstructorV2 extends BaseDeviceMsgConstructor {

    @Override
    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device, EntityGroupId entityGroupId) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(device))
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    @Override
    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        return DeviceCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(deviceCredentials)).build();
    }

    @Override
    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        return DeviceProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(deviceProfile))
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits()).build();
    }
}
