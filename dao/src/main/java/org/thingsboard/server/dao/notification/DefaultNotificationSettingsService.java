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
package org.thingsboard.server.dao.notification;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceInactivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.ActionTargetUserFilter;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.OriginatorEntityOwnerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.common.util.JacksonUtil.newObjectNode;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Service
@RequiredArgsConstructor
public class DefaultNotificationSettingsService implements NotificationSettingsService {

    private final AdminSettingsService adminSettingsService;
    private final NotificationTargetService notificationTargetService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationRuleService notificationRuleService;

    private static final String SETTINGS_KEY = "notifications";

    @Override
    public void saveNotificationSettings(TenantId tenantId, NotificationSettings settings) {
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(tenantId);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

    @Override
    public NotificationSettings findNotificationSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), NotificationSettings.class))
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setDeliveryMethodsConfigs(Collections.emptyMap());
                    return settings;
                });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // so that parent transaction is not aborted on method failure
    @Override
    public void createDefaultNotificationConfigs(TenantId tenantId) {
        NotificationTarget allUsers = createTarget(tenantId, "All users", new AllUsersFilter(),
                tenantId.isSysTenantId() ? "All platform users" : "All users in scope of the tenant");
        NotificationTarget tenantAdmins = createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(),
                tenantId.isSysTenantId() ? "All tenant administrators" : "Tenant administrators");

        if (tenantId.isSysTenantId()) {
            createTemplate(tenantId, "Maintenance work notification", NotificationType.GENERAL,
                    "Infrastructure maintenance",
                    "Maintenance work is scheduled for tomorrow (7:00 a.m. - 9:00 a.m. UTC). You may face major service interruptions, sorry for the inconvenience");
            return;
        }

        NotificationTarget originatorEntityOwnerUsers = createTarget(tenantId, "Users of rule trigger entity's owner", new OriginatorEntityOwnerUsersFilter(),
                "Customer users in case trigger entity (e.g. alarm) has customer, tenant admins otherwise");
        NotificationTarget actionTargetUser = createTarget(tenantId, "Action target", new ActionTargetUserFilter(),
                "If rule trigger is an action that targets some user (e.g. alarm assigned to user) - this user");

        NotificationTemplate alarmNotificationTemplate = createTemplate(tenantId, "Alarm notification", NotificationType.ALARM,
                "Alarm '${alarmType}' - ${action}",
                "Severity: ${alarmSeverity}, originator: ${alarmOriginatorEntityType} '${alarmOriginatorName}'",
                "notifications", null, null);
        AlarmNotificationRuleTriggerConfig alarmRuleTriggerConfig = new AlarmNotificationRuleTriggerConfig();
        alarmRuleTriggerConfig.setAlarmTypes(null);
        alarmRuleTriggerConfig.setAlarmSeverities(Set.of(AlarmSeverity.MAJOR, AlarmSeverity.CRITICAL));
        alarmRuleTriggerConfig.setNotifyOn(Set.of(AlarmAction.CREATED, AlarmAction.SEVERITY_CHANGED, AlarmAction.ACKNOWLEDGED, AlarmAction.CLEARED));
        createRule(tenantId, "Major or critical alarm", alarmNotificationTemplate.getId(), alarmRuleTriggerConfig,
                List.of(originatorEntityOwnerUsers.getId()), "Send notification to tenant admins or customer users " +
                        "when any major or critical alarm is created, updated or cleared");

        NotificationTemplate deviceActionNotificationTemplate = createTemplate(tenantId, "Device action notification", NotificationType.ENTITY_ACTION,
                "${entityType} was ${actionType}",
                "${entityType} '${entityName}' was ${actionType} by user ${originatorUserName}",
                "info", "Go to Device", "/devices/${entityId}");
        EntityActionNotificationRuleTriggerConfig deviceActionRuleTriggerConfig = new EntityActionNotificationRuleTriggerConfig();
        deviceActionRuleTriggerConfig.setEntityType(EntityType.DEVICE);
        deviceActionRuleTriggerConfig.setCreated(true);
        deviceActionRuleTriggerConfig.setUpdated(false);
        deviceActionRuleTriggerConfig.setDeleted(true);
        createRule(tenantId, "Device created or deleted", deviceActionNotificationTemplate.getId(), deviceActionRuleTriggerConfig,
                List.of(originatorEntityOwnerUsers.getId()), "Send notification to tenant admins or customer users " +
                        "when device is created or deleted");

        NotificationTemplate deviceInactivityNotificationTemplate = createTemplate(tenantId, "Device inactivity notification", NotificationType.DEVICE_INACTIVITY,
                "Device '${deviceName}' inactive",
                "Device '${deviceName}' with type '${deviceType}' became inactive",
                "info", "Go to Device", "/devices/${deviceId}");
        DeviceInactivityNotificationRuleTriggerConfig deviceInactivityRuleTriggerConfig = new DeviceInactivityNotificationRuleTriggerConfig();
        deviceInactivityRuleTriggerConfig.setDevices(null);
        deviceInactivityRuleTriggerConfig.setDeviceProfiles(null);
        createRule(tenantId, "Device inactivity", deviceInactivityNotificationTemplate.getId(), deviceInactivityRuleTriggerConfig,
                List.of(originatorEntityOwnerUsers.getId()), "Send notification to tenant admins or customer users " +
                        "when any device became inactive");

        NotificationTemplate alarmCommentNotificationTemplate = createTemplate(tenantId, "Alarm comment notification", NotificationType.ALARM_COMMENT,
                "Comment on '${alarmType}' alarm",
                "${userName} ${action} comment: ${comment}",
                "people", null, null);
        AlarmCommentNotificationRuleTriggerConfig alarmCommentRuleTriggerConfig = new AlarmCommentNotificationRuleTriggerConfig();
        alarmCommentRuleTriggerConfig.setAlarmTypes(null);
        alarmCommentRuleTriggerConfig.setAlarmSeverities(null);
        alarmCommentRuleTriggerConfig.setAlarmStatuses(Set.of(AlarmSearchStatus.ACTIVE));
        alarmCommentRuleTriggerConfig.setOnlyUserComments(true);
        alarmCommentRuleTriggerConfig.setNotifyOnCommentUpdate(false);
        createRule(tenantId, "Comment on active alarm", alarmCommentNotificationTemplate.getId(), alarmCommentRuleTriggerConfig,
                List.of(originatorEntityOwnerUsers.getId()), "Send notification to tenant admins or customer users " +
                        "when comment is added by user on active alarm");

        NotificationTemplate alarmAssignedNotificationTemplate = createTemplate(tenantId, "Alarm assigned notification", NotificationType.ALARM_ASSIGNMENT,
                "Alarm '${alarmType}' (${alarmSeverity}) was assigned to user",
                "${userName} assigned alarm on ${alarmOriginatorEntityType} '${alarmOriginatorName}' to ${assigneeEmail}",
                "person", null, null);
        AlarmAssignmentNotificationRuleTriggerConfig alarmAssignmentRuleTriggerConfig = new AlarmAssignmentNotificationRuleTriggerConfig();
        alarmAssignmentRuleTriggerConfig.setAlarmTypes(null);
        alarmAssignmentRuleTriggerConfig.setAlarmSeverities(null);
        alarmAssignmentRuleTriggerConfig.setAlarmStatuses(null);
        alarmAssignmentRuleTriggerConfig.setNotifyOn(Set.of(AlarmAssignmentNotificationRuleTriggerConfig.Action.ASSIGNED));
        createRule(tenantId, "Alarm assigned", alarmAssignedNotificationTemplate.getId(), alarmAssignmentRuleTriggerConfig,
                List.of(actionTargetUser.getId()), "Send notification to user when any alarm was assigned to him");

        NotificationTemplate ruleEngineComponentLifecycleFailureNotificationTemplate = createTemplate(tenantId, "Rule chain/node lifecycle failure notification", NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT,
                "${componentType} '${componentName}' failed to ${action}",
                "Rule chain '${ruleChainName}' - ${action} failure:<br/>${error}",
                "warning", "Go to Rule chain", "/features/ruleChains/${ruleChainId}");
        RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig ruleEngineComponentLifecycleEventRuleTriggerConfig = new RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig();
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setRuleChains(null);
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setRuleChainEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED));
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setOnlyRuleChainLifecycleFailures(true);
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setTrackRuleNodeEvents(true);
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setRuleNodeEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED));
        ruleEngineComponentLifecycleEventRuleTriggerConfig.setOnlyRuleNodeLifecycleFailures(true);
        createRule(tenantId, "Rule chain/node lifecycle failure", ruleEngineComponentLifecycleFailureNotificationTemplate.getId(),
                ruleEngineComponentLifecycleEventRuleTriggerConfig, List.of(tenantAdmins.getId()),
                "Send notification to tenant admins when any Rule chain or Rule node failed to start, update or stop");
    }

    private NotificationTarget createTarget(TenantId tenantId, String name, UsersFilter filter, String description) {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(tenantId);
        target.setName(name);

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(filter);
        targetConfig.setDescription(description);
        target.setConfiguration(targetConfig);
        return notificationTargetService.saveNotificationTarget(tenantId, target);
    }

    private NotificationTemplate createTemplate(TenantId tenantId, String name, NotificationType notificationType,
                                                String subjectTemplate, String textTemplate) {
        return createTemplate(tenantId, name, notificationType, subjectTemplate, textTemplate, null, null, null);
    }

    private NotificationTemplate createTemplate(TenantId tenantId, String name, NotificationType notificationType,
                                                String subjectTemplate, String textTemplate,
                                                String icon, String button, String link) {
        NotificationTemplate template = new NotificationTemplate();
        template.setTenantId(tenantId);
        template.setName(name);
        template.setNotificationType(notificationType);

        NotificationTemplateConfig templateConfig = new NotificationTemplateConfig();
        templateConfig.setNotificationSubject(subjectTemplate);
        templateConfig.setDefaultTextTemplate(textTemplate);

        WebDeliveryMethodNotificationTemplate webTemplate = new WebDeliveryMethodNotificationTemplate();
        ObjectNode additionalConfig = newObjectNode();
        ObjectNode iconConfig = newObjectNode();
        additionalConfig.set("icon", iconConfig);
        ObjectNode buttonConfig = newObjectNode();
        additionalConfig.set("actionButtonConfig", buttonConfig);
        if (icon != null) {
            iconConfig.put("enabled", true)
                    .put("icon", icon)
                    .put("color", "#757575");
        } else {
            iconConfig.put("enabled", false);
        }
        if (button != null) {
            buttonConfig.put("enabled", true)
                    .put("text", button)
                    .put("linkType", "LINK")
                    .put("link", link);
        } else {
            buttonConfig.put("enabled", false);
        }
        webTemplate.setAdditionalConfig(additionalConfig);
        webTemplate.setEnabled(true);
        templateConfig.setDeliveryMethodsTemplates(Map.of(
                NotificationDeliveryMethod.WEB, webTemplate
        ));
        template.setConfiguration(templateConfig);
        return notificationTemplateService.saveNotificationTemplate(tenantId, template);
    }

    private NotificationRule createRule(TenantId tenantId, String name, NotificationTemplateId templateId,
                                        NotificationRuleTriggerConfig triggerConfig, List<NotificationTargetId> targets,
                                        String description) {
        NotificationRule rule = new NotificationRule();
        rule.setTenantId(tenantId);
        rule.setName(name);
        rule.setTemplateId(templateId);
        rule.setTriggerType(triggerConfig.getTriggerType());
        rule.setTriggerConfig(triggerConfig);
        if (rule.getTriggerType() == NotificationRuleTriggerType.ALARM) {
            EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
            recipientsConfig.setTriggerType(rule.getTriggerType());
            recipientsConfig.setEscalationTable(Map.of(0, toUUIDs(targets)));
            rule.setRecipientsConfig(recipientsConfig);
        } else {
            DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
            recipientsConfig.setTriggerType(rule.getTriggerType());
            recipientsConfig.setTargets(toUUIDs(targets));
            rule.setRecipientsConfig(recipientsConfig);
        }
        NotificationRuleConfig additionalConfig = new NotificationRuleConfig();
        additionalConfig.setDescription(description);
        rule.setAdditionalConfig(additionalConfig);
        return notificationRuleService.saveNotificationRule(tenantId, rule);
    }

}
