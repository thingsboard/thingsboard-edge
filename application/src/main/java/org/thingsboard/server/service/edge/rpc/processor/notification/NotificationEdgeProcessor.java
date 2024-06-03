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
package org.thingsboard.server.service.edge.rpc.processor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class NotificationEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertNotificationRuleToDownlink(EdgeEvent edgeEvent) {
        NotificationRuleId notificationRuleId = new NotificationRuleId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(edgeEvent.getTenantId(), notificationRuleId);
                if (notificationRule != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationRuleUpdateMsg notificationRuleUpdateMsg = notificationMsgConstructor.constructNotificationRuleUpdateMsg(msgType, notificationRule);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationRuleUpdateMsg notificationRuleUpdateMsg = notificationMsgConstructor.constructNotificationRuleDeleteMsg(notificationRuleId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationRuleUpdateMsg(notificationRuleUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertNotificationTargetToDownlink(EdgeEvent edgeEvent) {
        NotificationTargetId notificationTargetId = new NotificationTargetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTarget notificationTarget = notificationTargetService.findNotificationTargetById(edgeEvent.getTenantId(), notificationTargetId);
                if (notificationTarget != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTargetUpdateMsg notificationTargetUpdateMsg = notificationMsgConstructor.constructNotificationTargetUpdateMsg(msgType, notificationTarget);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTargetUpdateMsg notificationTargetUpdateMsg = notificationMsgConstructor.constructNotificationTargetDeleteMsg(notificationTargetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTargetUpdateMsg(notificationTargetUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertNotificationTemplateToDownlink(EdgeEvent edgeEvent) {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                NotificationTemplate notificationTemplate = notificationTemplateService.findNotificationTemplateById(edgeEvent.getTenantId(), notificationTemplateId);
                if (notificationTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = notificationMsgConstructor.constructNotificationTemplateUpdateMsg(msgType, notificationTemplate);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                NotificationTemplateUpdateMsg notificationTemplateUpdateMsg = notificationMsgConstructor.constructNotificationTemplateDeleteMsg(notificationTemplateId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addNotificationTemplateUpdateMsg(notificationTemplateUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

}
