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
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

@Component
@TbCoreComponent
public class AlarmMsgConstructor {

    public AlarmUpdateMsg constructAlarmUpdatedMsg(UpdateMsgType msgType, Alarm alarm, String entityName, EdgeVersion edgeVersion) {
        if (EdgeVersionUtils.isEdgeVersionOlderThan_3_6_2(edgeVersion)) {
            return constructDeprecatedAlarmUpdatedMsg(msgType, alarm, entityName);
        }
        return AlarmUpdateMsg.newBuilder().setMsgType(msgType)
                .setEntity(JacksonUtil.toString(alarm))
                .setIdMSB(alarm.getId().getId().getMostSignificantBits())
                .setIdLSB(alarm.getId().getId().getLeastSignificantBits()).build();
    }

    private AlarmUpdateMsg constructDeprecatedAlarmUpdatedMsg(UpdateMsgType msgType, Alarm alarm, String entityName) {
        AlarmUpdateMsg.Builder builder = AlarmUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(alarm.getId().getId().getMostSignificantBits())
                .setIdLSB(alarm.getId().getId().getLeastSignificantBits())
                .setName(alarm.getName())
                .setType(alarm.getType())
                .setOriginatorName(entityName)
                .setOriginatorType(alarm.getOriginator().getEntityType().name())
                .setSeverity(alarm.getSeverity().name())
                .setStatus(alarm.getStatus().name())
                .setStartTs(alarm.getStartTs())
                .setEndTs(alarm.getEndTs())
                .setAckTs(alarm.getAckTs())
                .setClearTs(alarm.getClearTs())
                .setDetails(JacksonUtil.toString(alarm.getDetails()))
                .setPropagate(alarm.isPropagate())
                .setPropagateToOwner(alarm.isPropagateToOwner())
                .setPropagateToOwnerHierarchy(alarm.isPropagateToOwnerHierarchy())
                .setPropagateToTenant(alarm.isPropagateToTenant());
        return builder.build();
    }
}
