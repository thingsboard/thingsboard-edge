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
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.permission.Resource.NOTIFICATION;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
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
            notes = "Creates or updates notification template." + NEW_LINE +
                    "Here is an example of template to send notification via Web, SMS and Slack:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Greetings\",\n" +
                    "  \"notificationType\": \"GENERAL\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"deliveryMethodsTemplates\": {\n" +
                    "      \"WEB\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"subject\": \"Greetings\",\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"additionalConfig\": {\n" +
                    "          \"icon\": {\n" +
                    "            \"enabled\": true,\n" +
                    "            \"icon\": \"back_hand\",\n" +
                    "            \"color\": \"#757575\"\n" +
                    "          },\n" +
                    "          \"actionButtonConfig\": {\n" +
                    "            \"enabled\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"method\": \"WEB\"\n" +
                    "      },\n" +
                    "      \"SMS\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"method\": \"SMS\"\n" +
                    "      },\n" +
                    "      \"SLACK\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, @${recipientTitle}\",\n" +
                    "        \"method\": \"SLACK\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        notificationTemplate.setTenantId(getTenantId());
        checkEntity(notificationTemplate.getId(), notificationTemplate, NOTIFICATION);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @ApiOperation(value = "Get notification template by id (getNotificationTemplateById)",
            notes = "Fetches notification template by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @ApiOperation(value = "Get notification templates (getNotificationTemplates)",
            notes = "Returns the page of notification templates owned by sysadmin or tenant." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTemplate> getNotificationTemplates(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                                   @RequestParam int pageSize,
                                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                   @RequestParam int page,
                                                                   @ApiParam(value = "Case-insensitive 'substring' filter based on template's name and notification type")
                                                                   @RequestParam(required = false) String textSearch,
                                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortProperty,
                                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @ApiParam(value = "Comma-separated list of notification types to filter the templates")
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
            notes = "Deletes notification template by its id." + NEW_LINE +
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
            notes = "List available Slack conversations by type." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversationType type,
                                                          @ApiParam(value = "Slack bot token. If absent - system Slack settings will be used")
                                                          @RequestParam(required = false) String token,
                                                          @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, NOTIFICATION, Operation.READ);
        if (StringUtils.isEmpty(token)) {
            if (user.isSystemAdmin()) {
                accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
            } else {
                accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
            }
            NotificationSettings settings = notificationSettingsService.findNotificationSettings(user.getTenantId());
            SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                    settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
            if (slackConfig == null) {
                throw new IllegalArgumentException("Slack is not configured");
            }
            token = slackConfig.getBotToken();
        }

        return slackService.listConversations(user.getTenantId(), token, type);
    }

}
