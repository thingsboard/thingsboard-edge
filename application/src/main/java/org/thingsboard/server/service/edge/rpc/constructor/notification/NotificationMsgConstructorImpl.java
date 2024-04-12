/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.constructor.notification;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class NotificationMsgConstructorImpl implements NotificationMsgConstructor {

    @Override
    public NotificationRuleUpdateMsg constructNotificationRuleUpdateMsg(UpdateMsgType msgType, NotificationRule notificationRule) {
        return NotificationRuleUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationRule)).build();
    }

    @Override
    public NotificationRuleUpdateMsg constructNotificationRuleDeleteMsg(NotificationRuleId notificationRuleId) {
        return NotificationRuleUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationRuleId.getId().getMostSignificantBits())
                .setIdLSB(notificationRuleId.getId().getLeastSignificantBits()).build();
    }

    @Override
    public NotificationTargetUpdateMsg constructNotificationTargetUpdateMsg(UpdateMsgType msgType, NotificationTarget notificationTarget) {
        return NotificationTargetUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationTarget)).build();
    }

    @Override
    public NotificationTargetUpdateMsg constructNotificationTargetDeleteMsg(NotificationTargetId notificationTargetId) {
        return NotificationTargetUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationTargetId.getId().getMostSignificantBits())
                .setIdLSB(notificationTargetId.getId().getLeastSignificantBits()).build();
    }

    @Override
    public NotificationTemplateUpdateMsg constructNotificationTemplateUpdateMsg(UpdateMsgType msgType, NotificationTemplate notificationTemplate) {
        return NotificationTemplateUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationTemplate)).build();
    }

    @Override
    public NotificationTemplateUpdateMsg constructNotificationTemplateDeleteMsg(NotificationTemplateId notificationTemplateId) {
        return NotificationTemplateUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationTemplateId.getId().getMostSignificantBits())
                .setIdLSB(notificationTemplateId.getId().getLeastSignificantBits()).build();
    }

}
