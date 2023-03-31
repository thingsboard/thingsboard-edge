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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.permission.Resource.NOTIFICATION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationTemplateController extends BaseController {

    private final NotificationTemplateService notificationTemplateService;
    private final NotificationSettingsService notificationSettingsService;
    private final SlackService slackService;

    @ApiOperation(value = "Save notification template (saveNotificationTemplate)",
            notes = "Create or update notification template.\n\n" +
                    "Example:\n" +
                    "```\n{\n  \"name\": \"Hello to all my users\",\n" +
                    "  \"notificationType\": \"Message from administrator\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"defaultTextTemplate\": \"Hello everyone\",  # required if any of the templates' bodies is not set\n" +
                    "    \"templates\": {\n" +
                    "      \"PUSH\": {\n        \"method\": \"PUSH\",\n        \"body\": null  # defaultTextTemplate will be used if body is not set\n      },\n" +
                    "      \"SMS\": {\n        \"method\": \"SMS\",\n        \"body\": null\n      },\n" +
                    "      \"EMAIL\": {\n        \"method\": \"EMAIL\",\n        \"body\": \"Non-default value for email notification: <body>Hello everyone</body>\",\n        \"subject\": \"Message from administrator\"\n      },\n" +
                    "      \"SLACK\": {\n        \"method\": \"SLACK\",\n        \"body\": null,\n        \"conversationType\": \"PUBLIC_CHANNEL\",\n        \"conversationId\": \"U02LD7BJOU2\"  # received from listSlackConversations API method\n      }\n" +
                    "    }\n" +
                    "  }\n}\n```" +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        notificationTemplate.setTenantId(getTenantId());
        checkEntity(notificationTemplate.getId(), notificationTemplate, NOTIFICATION);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @ApiOperation(value = "Get notification template by id (getNotificationTemplateById)",
            notes = "Fetch notification template by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @ApiOperation(value = "Get notification templates (getNotificationTemplates)",
            notes = "Fetch the page of notification templates owned by sysadmin or tenant." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTemplate> getNotificationTemplates(@RequestParam int pageSize,
                                                                   @RequestParam int page,
                                                                   @RequestParam(required = false) String textSearch,
                                                                   @RequestParam(required = false) String sortProperty,
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @RequestParam(required = false) NotificationType[] notificationTypes,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, NOTIFICATION, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (notificationTypes == null || notificationTypes.length == 0) {
            notificationTypes = NotificationType.values();
        }
        return notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(user.getTenantId(),
                List.of(notificationTypes), pageLink);
    }

    @ApiOperation(value = "Delete notification template by id (deleteNotificationTemplateById",
            notes = "Delete notification template by its id.\n\n" +
                    "This template cannot be referenced by existing scheduled notification requests or any notification rules." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTemplateById(@PathVariable UUID id) throws Exception {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        NotificationTemplate notificationTemplate = checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::deleteNotificationTemplateById);
    }

    @ApiOperation(value = "List Slack conversations (listSlackConversations)",
            notes = "List available Slack conversations by type to use in notification template.\n\n" +
                    "Slack must be configured in notification settings." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversationType type,
                                                          @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, NOTIFICATION, Operation.READ);
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(user.getTenantId());
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        if (slackConfig == null) {
            throw new IllegalArgumentException("Slack is not configured");
        }

        return slackService.listConversations(user.getTenantId(), slackConfig.getBotToken(), type);
    }

}
