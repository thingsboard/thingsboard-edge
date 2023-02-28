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
package org.thingsboard.server.service.notification.rule;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.rule.trigger.AlarmTriggerProcessor.AlarmTriggerObject;
import org.thingsboard.server.service.notification.rule.trigger.NotificationRuleTriggerProcessor;
import org.thingsboard.server.service.notification.rule.trigger.RuleEngineComponentLifecycleEventTriggerProcessor.RuleEngineComponentLifecycleEventTriggerObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationRuleProcessingService implements NotificationRuleProcessingService {

    private final NotificationRuleService notificationRuleService;
    private final NotificationRequestService notificationRequestService;
    @Autowired @Lazy
    private NotificationCenter notificationCenter;
    private Map<NotificationRuleTriggerType, NotificationRuleTriggerProcessor> triggerProcessors;

    private final NotificationExecutorService notificationExecutor;
    private final DbCallbackExecutorService dbCallbackExecutor;

    private final Map<String, NotificationRuleTriggerType> msgTypeToTriggerType = Map.of(
            DataConstants.INACTIVITY_EVENT, NotificationRuleTriggerType.DEVICE_INACTIVITY,
            DataConstants.ENTITY_CREATED, NotificationRuleTriggerType.ENTITY_ACTION,
            DataConstants.ENTITY_UPDATED, NotificationRuleTriggerType.ENTITY_ACTION,
            DataConstants.ENTITY_DELETED, NotificationRuleTriggerType.ENTITY_ACTION,
            DataConstants.COMMENT_CREATED, NotificationRuleTriggerType.ALARM_COMMENT,
            DataConstants.COMMENT_UPDATED, NotificationRuleTriggerType.ALARM_COMMENT
    );

    @Override
    public void process(TenantId tenantId, TbMsg ruleEngineMsg) {
        String msgType = ruleEngineMsg.getType();
        NotificationRuleTriggerType triggerType = msgTypeToTriggerType.get(msgType);
        if (triggerType == null) {
            return;
        }

        processTrigger(tenantId, triggerType, ruleEngineMsg.getOriginator(), ruleEngineMsg);
    }

    @Override
    public void process(TenantId tenantId, Alarm alarm, boolean deleted) {
        AlarmTriggerObject triggerObject = AlarmTriggerObject.builder()
                .alarm(alarm)
                .deleted(deleted)
                .build();
        processTrigger(tenantId, NotificationRuleTriggerType.ALARM, alarm.getId(), triggerObject);
    }

    @Override
    public void process(TenantId tenantId, RuleChainId ruleChainId, String ruleChainName, EntityId componentId, String componentName, ComponentLifecycleEvent eventType, Exception error) {
        RuleEngineComponentLifecycleEventTriggerObject triggerObject = RuleEngineComponentLifecycleEventTriggerObject.builder()
                .ruleChainId(ruleChainId)
                .ruleChainName(ruleChainName)
                .componentId(componentId)
                .componentName(componentName)
                .eventType(eventType)
                .error(error)
                .build();
        processTrigger(tenantId, NotificationRuleTriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, componentId, triggerObject);
    }

    private void processTrigger(TenantId tenantId, NotificationRuleTriggerType triggerType, EntityId originatorEntityId, Object triggerObject) {
        ListenableFuture<List<NotificationRule>> rulesFuture = dbCallbackExecutor.submit(() -> {
            return notificationRuleService.findNotificationRulesByTenantIdAndTriggerType(tenantId, triggerType);
        });
        DonAsynchron.withCallback(rulesFuture, rules -> {
            for (NotificationRule rule : rules) {
                notificationExecutor.submit(() -> {
                    processNotificationRule(rule, originatorEntityId, triggerObject);
                });
            }
        }, e -> {
            log.error("Failed to find notification rules by trigger type {}", triggerType, e);
        });
    }

    private void processNotificationRule(NotificationRule rule, EntityId originatorEntityId, Object triggerObject) {
        NotificationRuleTriggerConfig triggerConfig = rule.getTriggerConfig();
        log.debug("Processing notification rule '{}' for trigger type {}", rule.getName(), rule.getTriggerType());

        if (triggerConfig.getTriggerType().isUpdatable()) {
            List<NotificationRequest> notificationRequests = notificationRequestService.findNotificationRequestsByRuleIdAndOriginatorEntityId(rule.getTenantId(), rule.getId(), originatorEntityId);
            if (!notificationRequests.isEmpty()) {
                if (matchesClearRule(triggerObject, triggerConfig)) {
                    notificationRequests = notificationRequests.stream()
                            .filter(notificationRequest -> {
                                if (!notificationRequest.isSent()) {
                                    dbCallbackExecutor.submit(() -> {
                                        notificationCenter.deleteNotificationRequest(rule.getTenantId(), notificationRequest.getId());
                                    });
                                    return false;
                                } else {
                                    return true;
                                }
                            })
                            .collect(Collectors.toList());
                    // not returning because we need to update notifications if any
                }

                NotificationInfo notificationInfo = constructNotificationInfo(triggerObject, triggerConfig);
                for (NotificationRequest notificationRequest : notificationRequests) {
                    NotificationInfo previousNotificationInfo = notificationRequest.getInfo();
                    if (!notificationInfo.equals(previousNotificationInfo)) {
                        notificationRequest.setInfo(notificationInfo);
                        dbCallbackExecutor.submit(() -> {
                            notificationCenter.updateNotificationRequest(rule.getTenantId(), notificationRequest);
                        });
                    }
                }
                return;
            }
        }

        if (!matchesFilter(triggerObject, triggerConfig)) {
            return;
        }

        NotificationInfo notificationInfo = constructNotificationInfo(triggerObject, triggerConfig);
        rule.getRecipientsConfig().getTargetsTable().forEach((delay, targets) -> {
            notificationExecutor.submit(() -> {
                try {
                    log.debug("Submitting notification request for rule '{}' with delay of {} sec to targets {}", rule.getName(), delay, targets);
                    submitNotificationRequest(targets, rule, originatorEntityId, notificationInfo, delay);
                } catch (Exception e) {
                    log.error("Failed to submit notification request for rule {}", rule.getId(), e);
                }
            });
        });
    }

    private boolean matchesFilter(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesFilter(triggerObject, triggerConfig);
    }

    private boolean matchesClearRule(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesClearRule(triggerObject, triggerConfig);
    }

    private NotificationInfo constructNotificationInfo(Object triggerObject, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).constructNotificationInfo(triggerObject, triggerConfig);
    }

    private void submitNotificationRequest(List<UUID> targets, NotificationRule rule,
                                           EntityId originatorEntityId, NotificationInfo notificationInfo, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        if (delayInSec > 0) {
            config.setSendingDelayInSec(delayInSec);
        }
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(rule.getTenantId())
                .targets(targets)
                .templateId(rule.getTemplateId())
                .additionalConfig(config)
                .info(notificationInfo)
                .ruleId(rule.getId())
                .originatorEntityId(originatorEntityId)
                .build();
        notificationCenter.processNotificationRequest(rule.getTenantId(), notificationRequest);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onNotificationRuleDeleted(ComponentLifecycleMsg componentLifecycleMsg) {
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED ||
                componentLifecycleMsg.getEntityId().getEntityType() != EntityType.NOTIFICATION_RULE) {
            return;
        }

        TenantId tenantId = componentLifecycleMsg.getTenantId();
        NotificationRuleId notificationRuleId = (NotificationRuleId) componentLifecycleMsg.getEntityId();
        dbCallbackExecutor.submit(() -> {
            List<NotificationRequestId> scheduledForRule = notificationRequestService.findNotificationRequestsIdsByStatusAndRuleId(tenantId, NotificationRequestStatus.SCHEDULED, notificationRuleId);
            for (NotificationRequestId notificationRequestId : scheduledForRule) {
                notificationCenter.deleteNotificationRequest(tenantId, notificationRequestId);
            }
        });
    }

    @Autowired
    public void setTriggerProcessors(Collection<NotificationRuleTriggerProcessor> processors) {
        this.triggerProcessors = processors.stream()
                .collect(Collectors.toMap(NotificationRuleTriggerProcessor::getTriggerType, p -> p));
    }

}
