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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleEngineComponentLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.notification.trigger.RuleEngineComponentLifecycleEventTrigger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

@Service
public class RuleEngineComponentLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<RuleEngineComponentLifecycleEventTrigger, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineComponentLifecycleEventTrigger trigger, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getRuleChains())) {
            if (!triggerConfig.getRuleChains().contains(trigger.getRuleChainId().getId())) {
                return false;
            }
        }

        EntityType componentType = trigger.getComponentId().getEntityType();
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

        if (!trackedEvents.contains(trigger.getEventType())) {
            return false;
        }
        if (onlyFailures) {
            return trigger.getError() != null;
        }
        return true;
    }

    @Override
    public NotificationInfo constructNotificationInfo(RuleEngineComponentLifecycleEventTrigger trigger, RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        return RuleEngineComponentLifecycleEventNotificationInfo.builder()
                .ruleChainId(trigger.getRuleChainId())
                .ruleChainName(trigger.getRuleChainName())
                .componentId(trigger.getComponentId())
                .componentName(trigger.getComponentName())
                .action(trigger.getEventType() == ComponentLifecycleEvent.STARTED ? "start" :
                        trigger.getEventType() == ComponentLifecycleEvent.UPDATED ? "update" :
                        trigger.getEventType() == ComponentLifecycleEvent.STOPPED ? "stop" : null)
                .eventType(trigger.getEventType())
                .error(getErrorMsg(trigger.getError()))
                .build();
    }

    private String getErrorMsg(Exception error) {
        if (error == null) return null;

        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return StringUtils.abbreviate(ExceptionUtils.getStackTrace(error), 200);
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT;
    }

}
