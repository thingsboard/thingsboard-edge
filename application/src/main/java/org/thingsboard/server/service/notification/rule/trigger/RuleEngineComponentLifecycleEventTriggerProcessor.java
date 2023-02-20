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

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleEngineComponentLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineComponentLifecycleEventTriggerProcessor.RuleEngineComponentLifecycleEventTriggerObject;

import java.util.Set;

@Service
public class RuleEngineComponentLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<RuleEngineComponentLifecycleEventTriggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineComponentLifecycleEventTriggerObject triggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getRuleChains())) {
            if (!triggerConfig.getRuleChains().contains(triggerObject.getRuleChainId().getId())) {
                return false;
            }
        }

        EntityType componentType = triggerObject.getComponentId().getEntityType();
        Set<ComponentLifecycleEvent> trackedEvents;
        boolean onlyFailures;
        if (componentType == EntityType.RULE_CHAIN) {
            trackedEvents = triggerConfig.getRuleChainEvents();
            onlyFailures = triggerConfig.isOnlyRuleChainLifecycleFailures();
        } else if (componentType == EntityType.RULE_NODE && triggerConfig.isTrackRuleNodeEvents()) {
            trackedEvents = triggerConfig.getRuleNodeEvents();
            onlyFailures = triggerConfig.isOnlyRuleNodeLifecycleFailures();
        } else {
            return false;
        }
        if (CollectionUtils.isEmpty(trackedEvents)) {
            trackedEvents = Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED);
        }

        if (!trackedEvents.contains(triggerObject.getEventType())) {
            return false;
        }
        if (onlyFailures) {
            return triggerObject.getError() != null;
        }
        return true;
    }

    @Override
    public NotificationInfo constructNotificationInfo(RuleEngineComponentLifecycleEventTriggerObject triggerObject, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        return RuleEngineComponentLifecycleEventNotificationInfo.builder()
                .ruleChainId(triggerObject.getRuleChainId())
                .ruleChainName(triggerObject.getRuleChainName())
                .componentId(triggerObject.getComponentId())
                .componentName(triggerObject.getComponentName())
                .eventType(triggerObject.getEventType())
                .error(getErrorMsg(triggerObject.getError()))
                .build();
    }

    private String getErrorMsg(Exception error) {
        String errorMsg = error != null ? error.getMessage() : null;
        errorMsg = Strings.nullToEmpty(errorMsg);
        int lengthLimit = 150;
        if (errorMsg.length() > lengthLimit) {
            errorMsg = StringUtils.substring(errorMsg, 0, lengthLimit + 1).trim() + "[...]";
        }
        return errorMsg;
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

    @Data
    @Builder
    public static class RuleEngineComponentLifecycleEventTriggerObject {
        private final RuleChainId ruleChainId;
        private final String ruleChainName;
        private final EntityId componentId;
        private final String componentName;
        private final ComponentLifecycleEvent eventType;
        private final Exception error;
    }

}
