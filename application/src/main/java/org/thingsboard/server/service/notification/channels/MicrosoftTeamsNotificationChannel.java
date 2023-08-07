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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate.Button.LinkType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.notification.NotificationProcessingContext;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MicrosoftTeamsNotificationChannel implements NotificationChannel<MicrosoftTeamsNotificationTargetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate> {

    private final SystemSecurityService systemSecurityService;

    @Setter
    private RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .build();

    @Override
    public void sendNotification(MicrosoftTeamsNotificationTargetConfig targetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        Message message = new Message();
        message.setThemeColor(Strings.emptyToNull(processedTemplate.getThemeColor()));
        if (StringUtils.isEmpty(processedTemplate.getSubject())) {
            message.setText(processedTemplate.getBody());
        } else {
            message.setSummary(processedTemplate.getSubject());
            Message.Section section = new Message.Section();
            section.setActivityTitle(processedTemplate.getSubject());
            section.setActivitySubtitle(processedTemplate.getBody());
            message.setSections(List.of(section));
        }
        var button = processedTemplate.getButton();
        if (button != null && button.isEnabled()) {
            String uri;
            if (button.getLinkType() == LinkType.DASHBOARD) {
                String state = null;
                if (button.isSetEntityIdInState() || StringUtils.isNotEmpty(button.getDashboardState())) {
                    ObjectNode stateObject = JacksonUtil.newObjectNode();
                    if (button.isSetEntityIdInState()) {
                        stateObject.putObject("params")
                                .set("entityId", Optional.ofNullable(ctx.getRequest().getInfo())
                                        .map(NotificationInfo::getStateEntityId)
                                        .map(JacksonUtil::valueToTree)
                                        .orElse(null));
                    } else {
                        stateObject.putObject("params");
                    }
                    if (StringUtils.isNotEmpty(button.getDashboardState())) {
                        stateObject.put("id", button.getDashboardState());
                    }
                    state = Base64.encodeBase64String(JacksonUtil.OBJECT_MAPPER.writeValueAsBytes(List.of(stateObject)));
                }
                String baseUrl = systemSecurityService.getBaseUrl(ctx.getTenantId().isSysTenantId() ?
                        Authority.SYS_ADMIN : Authority.TENANT_ADMIN, ctx.getTenantId(), null, null);
                if (StringUtils.isEmpty(baseUrl)) {
                    throw new IllegalStateException("Failed to determine base url to construct dashboard link");
                }
                uri = baseUrl + "/dashboards/" + button.getDashboardId();
                if (state != null) {
                    uri += "?state=" + state;
                }
            } else {
                uri = button.getLink();
            }
            if (StringUtils.isNotBlank(uri) && button.getText() != null) {
                Message.ActionCard actionCard = new Message.ActionCard();
                actionCard.setType("OpenUri");
                actionCard.setName(button.getText());
                var target = new Message.ActionCard.Target("default", uri);
                actionCard.setTargets(List.of(target));
                message.setPotentialAction(List.of(actionCard));
            }
        }

        restTemplate.postForEntity(targetConfig.getWebhookUrl(), message, String.class);
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

    @Data
    public static class Message {
        @JsonProperty("@type")
        private final String type = "MessageCard";
        @JsonProperty("@context")
        private final String context = "http://schema.org/extensions";
        private String themeColor;
        private String summary;
        private String text;
        private List<Section> sections;
        private List<ActionCard> potentialAction;

        @Data
        public static class Section {
            private String activityTitle;
            private String activitySubtitle;
            private String activityImage;
            private List<Fact> facts;
            private boolean markdown;

            @Data
            public static class Fact {
                private final String name;
                private final String value;
            }
        }

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ActionCard {
            @JsonProperty("@type")
            private String type; // ActionCard, OpenUri
            private String name;
            private List<Input> inputs; // for ActionCard
            private List<Action> actions; // for ActionCard
            private List<Target> targets;

            @Data
            public static class Input {
                @JsonProperty("@type")
                private String type; // TextInput, DateInput, MultichoiceInput
                private String id;
                private boolean isMultiple;
                private String title;
                private boolean isMultiSelect;

                @Data
                public static class Choice {
                    private final String display;
                    private final String value;
                }
            }

            @Data
            public static class Action {
                @JsonProperty("@type")
                private final String type; // HttpPOST
                private final String name;
                private final String target; // url
            }

            @Data
            public static class Target {
                private final String os;
                private final String uri;
            }
        }

    }

}
