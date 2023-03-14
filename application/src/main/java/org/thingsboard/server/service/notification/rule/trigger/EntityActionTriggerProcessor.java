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
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@Service
public class EntityActionTriggerProcessor implements NotificationRuleTriggerProcessor<TbMsg, EntityActionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        String msgType = ruleEngineMsg.getType();
        if (msgType.equals(DataConstants.ENTITY_CREATED)) {
            if (!triggerConfig.isCreated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_UPDATED)) {
            if (!triggerConfig.isUpdated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_DELETED)) {
            if (!triggerConfig.isDeleted()) {
                return false;
            }
        } else {
            return false;
        }
        return triggerConfig.getEntityType() == null || ruleEngineMsg.getOriginator().getEntityType() == triggerConfig.getEntityType();
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        EntityId entityId = ruleEngineMsg.getOriginator();
        String msgType = ruleEngineMsg.getType();
        ActionType actionType = msgType.equals(DataConstants.ENTITY_CREATED) ? ActionType.ADDED :
                                msgType.equals(DataConstants.ENTITY_UPDATED) ? ActionType.UPDATED :
                                msgType.equals(DataConstants.ENTITY_DELETED) ? ActionType.DELETED : null;
        return EntityActionNotificationInfo.builder()
                .entityId(entityId)
                .entityName(ruleEngineMsg.getMetaData().getValue("entityName"))
                .actionType(actionType)
                .originatorUserId(UUID.fromString(ruleEngineMsg.getMetaData().getValue("userId")))
                .originatorUserName(ruleEngineMsg.getMetaData().getValue("userName"))
                .entityCustomerId(ruleEngineMsg.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

}
