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
package org.thingsboard.server.service.notification;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.util.TemplateUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    @Getter
    private final NotificationRequest request;
    @Getter
    private final Set<NotificationDeliveryMethod> deliveryMethods;
    @Getter
    private final NotificationTemplate notificationTemplate;

    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private final NotificationRequestStats stats;

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, Set<NotificationDeliveryMethod> deliveryMethods,
                                           NotificationTemplate template, NotificationSettings settings) {
        this.tenantId = tenantId;
        this.request = request;
        this.deliveryMethods = deliveryMethods;
        this.settings = settings;
        this.notificationTemplate = template;
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
        init();
    }

    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (template.isEnabled()) {
                template = processTemplate(template, null); // processing template with immutable params
                templates.put(deliveryMethod, template);
            }
        });
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    public <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        T template = (T) templates.get(deliveryMethod);
        Map<String, String> additionalTemplateContext = null;
        if (recipient != null) {
            additionalTemplateContext = createTemplateContextForRecipient(recipient);
        }
        if (MapUtils.isNotEmpty(additionalTemplateContext) && template.containsAny(additionalTemplateContext.keySet().toArray(String[]::new))) {
            template = processTemplate(template, additionalTemplateContext);
        }
        return template;
    }

    private <T extends DeliveryMethodNotificationTemplate> T processTemplate(T template, Map<String, String> additionalTemplateContext) {
        Map<String, String> templateContext = new HashMap<>();
        if (request.getInfo() != null) {
            templateContext.putAll(request.getInfo().getTemplateData());
        }
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        if (templateContext.isEmpty()) return template;

        template = (T) template.copy();
        template.setBody(TemplateUtils.processTemplate(template.getBody(), templateContext));
        if (template instanceof HasSubject) {
            String subject = ((HasSubject) template).getSubject();
            ((HasSubject) template).setSubject(TemplateUtils.processTemplate(subject, templateContext));
        }
        if (template instanceof WebDeliveryMethodNotificationTemplate) {
            WebDeliveryMethodNotificationTemplate webNotificationTemplate = (WebDeliveryMethodNotificationTemplate) template;
            String buttonText = webNotificationTemplate.getButtonText();
            if (isNotEmpty(buttonText)) {
                webNotificationTemplate.setButtonText(TemplateUtils.processTemplate(buttonText, templateContext));
            }
            String buttonLink = webNotificationTemplate.getButtonLink();
            if (isNotEmpty(buttonLink)) {
                webNotificationTemplate.setButtonLink(TemplateUtils.processTemplate(buttonLink, templateContext));
            }
        }
        return template;
    }

    private Map<String, String> createTemplateContextForRecipient(NotificationRecipient recipient) {
        return Map.of(
                "recipientEmail", Strings.nullToEmpty(recipient.getEmail()),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
    }

}
