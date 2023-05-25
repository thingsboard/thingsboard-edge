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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.IntegrationLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationRuleImportService extends BaseEntityImportService<NotificationRuleId, NotificationRule, EntityExportData<NotificationRule>> {

    private final NotificationRuleService notificationRuleService;

    @Override
    protected void setOwner(TenantId tenantId, NotificationRule notificationRule, IdProvider idProvider) {
        notificationRule.setTenantId(tenantId);
    }

    @Override
    protected NotificationRule prepare(EntitiesImportCtx ctx, NotificationRule notificationRule, NotificationRule oldNotificationRule, EntityExportData<NotificationRule> exportData, IdProvider idProvider) {
        notificationRule.setTemplateId(idProvider.getInternalId(notificationRule.getTemplateId()));

        NotificationRuleTriggerConfig ruleTriggerConfig = notificationRule.getTriggerConfig();
        NotificationRuleTriggerType triggerType = ruleTriggerConfig.getTriggerType();
        switch (triggerType) {
            case DEVICE_ACTIVITY: {
                DeviceActivityNotificationRuleTriggerConfig triggerConfig = (DeviceActivityNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> devices = triggerConfig.getDevices();
                if (devices != null) {
                    triggerConfig.setDevices(devices.stream().map(DeviceId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }

                Set<UUID> deviceProfiles = triggerConfig.getDeviceProfiles();
                if (deviceProfiles != null) {
                    triggerConfig.setDeviceProfiles(deviceProfiles.stream().map(DeviceProfileId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }
                break;
            }
            case RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT: {
                RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig triggerConfig = (RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> ruleChains = triggerConfig.getRuleChains();
                if (ruleChains != null) {
                    triggerConfig.setRuleChains(ruleChains.stream().map(RuleChainId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }
                break;
            }
            case INTEGRATION_LIFECYCLE_EVENT: {
                IntegrationLifecycleEventNotificationRuleTriggerConfig triggerConfig = (IntegrationLifecycleEventNotificationRuleTriggerConfig) ruleTriggerConfig;
                Set<UUID> integrations = triggerConfig.getIntegrations();
                if (integrations != null) {
                    triggerConfig.setIntegrations(integrations.stream().map(IntegrationId::new)
                            .map(idProvider::getInternalId).map(UUIDBased::getId)
                            .collect(Collectors.toSet()));
                }
                break;
            }
        }
        if (!triggerType.isTenantLevel()) {
            throw new IllegalArgumentException("Trigger type " + triggerType + " is not available for tenants");
        }

        NotificationRuleRecipientsConfig ruleRecipientsConfig = notificationRule.getRecipientsConfig();
        switch (triggerType) {
            case ALARM: {
                EscalatedNotificationRuleRecipientsConfig recipientsConfig = (EscalatedNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                Map<Integer, List<UUID>> escalationTable = new LinkedHashMap<>(recipientsConfig.getEscalationTable());
                escalationTable.replaceAll((delay, targets) -> targets.stream()
                        .map(NotificationTargetId::new).map(idProvider::getInternalId)
                        .map(UUIDBased::getId).collect(Collectors.toList()));
                recipientsConfig.setEscalationTable(escalationTable);
                break;
            }
            default: {
                DefaultNotificationRuleRecipientsConfig recipientsConfig = (DefaultNotificationRuleRecipientsConfig) ruleRecipientsConfig;
                List<UUID> targets = recipientsConfig.getTargets().stream()
                        .map(NotificationTargetId::new).map(idProvider::getInternalId)
                        .map(UUIDBased::getId).collect(Collectors.toList());
                recipientsConfig.setTargets(targets);
                break;
            }
        }
        return notificationRule;
    }

    @Override
    protected NotificationRule saveOrUpdate(EntitiesImportCtx ctx, NotificationRule notificationRule, EntityExportData<NotificationRule> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(notificationRule);
        return notificationRuleService.saveNotificationRule(ctx.getTenantId(), notificationRule);
    }

    @Override
    protected void onEntitySaved(User user, NotificationRule savedEntity, NotificationRule oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedEntity.getId(),
                oldEntity == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    @Override
    protected NotificationRule deepCopy(NotificationRule notificationRule) {
        return new NotificationRule(notificationRule);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_RULE;
    }

}
