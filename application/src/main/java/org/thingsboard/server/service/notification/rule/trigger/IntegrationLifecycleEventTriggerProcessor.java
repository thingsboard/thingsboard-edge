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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.IntegrationLifecycleEventNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.IntegrationLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.notification.trigger.IntegrationLifecycleEventTrigger;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class IntegrationLifecycleEventTriggerProcessor implements NotificationRuleTriggerProcessor<IntegrationLifecycleEventTrigger, IntegrationLifecycleEventNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(IntegrationLifecycleEventTrigger trigger, IntegrationLifecycleEventNotificationRuleTriggerConfig triggerConfig) {
        return (isEmpty(triggerConfig.getIntegrationTypes()) || triggerConfig.getIntegrationTypes().contains(trigger.getIntegrationType())) &&
                (isEmpty(triggerConfig.getIntegrations()) || triggerConfig.getIntegrations().contains(trigger.getIntegrationId().getId())) &&
                (isEmpty(triggerConfig.getNotifyOn()) || triggerConfig.getNotifyOn().contains(trigger.getEvent())) &&
                (!triggerConfig.isOnlyOnError() || trigger.getError() != null);
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(IntegrationLifecycleEventTrigger trigger) {
        return IntegrationLifecycleEventNotificationInfo.builder()
                .integrationId(trigger.getIntegrationId())
                .integrationType(trigger.getIntegrationType())
                .integrationName(trigger.getIntegrationName())
                .action(trigger.getEvent() == ComponentLifecycleEvent.STARTED ? "start"
                        : trigger.getEvent() == ComponentLifecycleEvent.UPDATED ? "update"
                        : trigger.getEvent() == ComponentLifecycleEvent.STOPPED ? "stop" : null)
                .eventType(trigger.getEvent())
                .error(trigger.getError() != null ? StringUtils.abbreviate(ExceptionUtils.getStackTrace(trigger.getError()), 200) : null)
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.INTEGRATION_LIFECYCLE_EVENT;
    }

}
