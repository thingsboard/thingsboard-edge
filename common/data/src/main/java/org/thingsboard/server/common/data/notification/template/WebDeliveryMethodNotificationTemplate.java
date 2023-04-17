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
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.constraints.NotEmpty;
import java.util.Optional;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WebDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    @NoXss(fieldName = "web notification subject")
    @Length(fieldName = "web notification subject", max = 150, message = "cannot be longer than 150 chars")
    @NotEmpty
    private String subject;
    private JsonNode additionalConfig;

    public WebDeliveryMethodNotificationTemplate(WebDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.additionalConfig = other.additionalConfig != null ? other.additionalConfig.deepCopy() : null;
    }

    @NoXss(fieldName = "web notification message")
    @Length(fieldName = "web notification message", max = 250, message = "cannot be longer than 250 chars")
    @Override
    public String getBody() {
        return super.getBody();
    }

    @NoXss(fieldName = "web notification button text")
    @Length(fieldName = "web notification button text", max = 50, message = "cannot be longer than 50 chars")
    @JsonIgnore
    public String getButtonText() {
        return getButtonConfigProperty("text");
    }

    @JsonIgnore
    public void setButtonText(String buttonText) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("text", new TextNode(buttonText));
        });
    }

    @NoXss(fieldName = "web notification button link")
    @Length(fieldName = "web notification button link", max = 300, message = "cannot be longer than 300 chars")
    @JsonIgnore
    public String getButtonLink() {
        return getButtonConfigProperty("link");
    }

    @JsonIgnore
    public void setButtonLink(String buttonLink) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("link", new TextNode(buttonLink));
        });
    }

    private String getButtonConfigProperty(String property) {
        return getButtonConfig()
                .map(buttonConfig -> buttonConfig.get(property))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    private Optional<ObjectNode> getButtonConfig() {
        return Optional.ofNullable(additionalConfig)
                .map(config -> config.get("actionButtonConfig")).filter(JsonNode::isObject)
                .map(config -> (ObjectNode) config);
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.WEB;
    }

    @Override
    public WebDeliveryMethodNotificationTemplate copy() {
        return new WebDeliveryMethodNotificationTemplate(this);
    }

    @Override
    public boolean containsAny(String... params) {
        return super.containsAny(params) || StringUtils.containsAny(subject, params)
                || StringUtils.containsAny(getButtonText(), params) || StringUtils.containsAny(getButtonLink(), params);
    }

}
