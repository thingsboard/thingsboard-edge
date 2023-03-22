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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    @Getter
    private final NotificationRequest request;

    @Getter
    private final NotificationTemplate notificationTemplate;
    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private Set<NotificationDeliveryMethod> deliveryMethods;
    @Getter
    private final NotificationRequestStats stats;

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, NotificationSettings settings,
                                         NotificationTemplate template) {
        this.tenantId = tenantId;
        this.request = request;
        this.settings = settings;
        this.notificationTemplate = template;
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
        init();
    }

    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (!template.isEnabled()) return;

            template = template.copy();
            if (StringUtils.isEmpty(template.getBody())) {
                template.setBody(templateConfig.getDefaultTextTemplate());
            }
            if (template instanceof HasSubject) {
                if (StringUtils.isEmpty(((HasSubject) template).getSubject())) {
                    ((HasSubject) template).setSubject(templateConfig.getNotificationSubject());
                }
            }
            templates.put(deliveryMethod, template);
        });
        deliveryMethods = templates.keySet();
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    public <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, Map<String, String> templateContext) {
        NotificationInfo info = request.getInfo();
        if (info != null) {
            templateContext = new HashMap<>(templateContext);
            templateContext.putAll(info.getTemplateData());
        }

        T template = (T) templates.get(deliveryMethod).copy();
        template.setBody(processTemplate(template.getBody(), templateContext));
        if (template instanceof HasSubject) {
            String subject = ((HasSubject) template).getSubject();
            ((HasSubject) template).setSubject(processTemplate(subject, templateContext));
        }

        if (deliveryMethod == NotificationDeliveryMethod.WEB) {
            WebDeliveryMethodNotificationTemplate webNotificationTemplate = (WebDeliveryMethodNotificationTemplate) template;
            Optional<ObjectNode> buttonConfig = Optional.ofNullable(webNotificationTemplate.getAdditionalConfig())
                    .map(config -> config.get("actionButtonConfig")).filter(JsonNode::isObject)
                    .map(config -> (ObjectNode) config);
            if (buttonConfig.isPresent()) {
                JsonNode text = buttonConfig.get().get("text");
                if (text != null && text.isTextual()) {
                    text = new TextNode(processTemplate(text.asText(), templateContext));
                    buttonConfig.get().set("text", text);
                }
                JsonNode link = buttonConfig.get().get("link");
                if (link != null && link.isTextual()) {
                    link = new TextNode(processTemplate(link.asText(), templateContext).toLowerCase());
                    buttonConfig.get().set("link", link);
                }
            }
        }
        return template;
    }

    private static String processTemplate(String template, Map<String, String> context) {
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> kv : context.entrySet()) {
            String value = Strings.nullToEmpty(kv.getValue());
            result = result.replace("${" + kv.getKey() + '}', value);
        }
        return result;
    }

    public static String processTemplate(String template, NotificationInfo notificationInfo) {
        if (notificationInfo == null) return template;
        Map<String, String> templateContext = notificationInfo.getTemplateData();
        return processTemplate(template, templateContext);
    }

    public Map<String, String> createTemplateContext(User recipient) {
        Map<String, String> templateContext = new HashMap<>();
        templateContext.put("recipientEmail", recipient.getEmail());
        templateContext.put("recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()));
        templateContext.put("recipientLastName", Strings.nullToEmpty(recipient.getLastName()));
        return templateContext;
    }

    public CustomerId getCustomerId() {
        if (request.getInfo() instanceof RuleOriginatedNotificationInfo) {
            return ((RuleOriginatedNotificationInfo) request.getInfo()).getOriginatorEntityCustomerId();
        } else {
            return null;
        }
    }

}
