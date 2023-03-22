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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.notification.trigger.RuleEngineMsgTrigger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class EntityActionTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<EntityActionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineMsgTrigger trigger, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        String msgType = trigger.getMsg().getType();
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
        return triggerConfig.getEntityType() == null || getEntityType(trigger.getMsg()) == triggerConfig.getEntityType();
    }

    @Override
    public NotificationInfo constructNotificationInfo(RuleEngineMsgTrigger trigger, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        TbMsg msg = trigger.getMsg();
        String msgType = msg.getType();
        ActionType actionType = msgType.equals(DataConstants.ENTITY_CREATED) ? ActionType.ADDED :
                                msgType.equals(DataConstants.ENTITY_UPDATED) ? ActionType.UPDATED :
                                msgType.equals(DataConstants.ENTITY_DELETED) ? ActionType.DELETED : null;
        return EntityActionNotificationInfo.builder()
                .entityId(actionType != ActionType.DELETED ? msg.getOriginator() : null)
                .entityName(msg.getMetaData().getValue("entityName"))
                .actionType(actionType)
                .originatorUserId(UUID.fromString(msg.getMetaData().getValue("userId")))
                .originatorUserName(msg.getMetaData().getValue("userName"))
                .entityCustomerId(msg.getCustomerId())
                .build();
    }

    private static EntityType getEntityType(TbMsg msg) {
        return Optional.ofNullable(msg.getMetaData().getValue("entityType"))
                .map(EntityType::valueOf).orElse(null);
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.ENTITY_CREATED, DataConstants.ENTITY_UPDATED, DataConstants.ENTITY_DELETED);
    }

}
