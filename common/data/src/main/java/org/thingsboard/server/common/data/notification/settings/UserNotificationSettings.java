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
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UserNotificationSettings {

    @NotNull
    @Valid
    private final Map<NotificationType, NotificationPref> prefs;

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyMap());

    public static final Set<NotificationDeliveryMethod> deliveryMethods = NotificationTargetType.PLATFORM_USERS.getSupportedDeliveryMethods();

    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") Map<NotificationType, NotificationPref> prefs) {
        this.prefs = prefs;
    }

    public boolean isEnabled(NotificationType notificationType, NotificationDeliveryMethod deliveryMethod) {
        NotificationPref pref = prefs.get(notificationType);
        if (pref == null) {
            return true;
        }
        if (!pref.isEnabled()) {
            return false;
        }
        return pref.getEnabledDeliveryMethods().getOrDefault(deliveryMethod, true);
    }

    @Data
    public static class NotificationPref {
        private boolean enabled;
        @NotNull
        private Map<NotificationDeliveryMethod, Boolean> enabledDeliveryMethods;

        public static NotificationPref createDefault() {
            NotificationPref pref = new NotificationPref();
            pref.setEnabled(true);
            pref.setEnabledDeliveryMethods(deliveryMethods.stream().collect(Collectors.toMap(v -> v, v -> true)));
            return pref;
        }

        @JsonIgnore
        @AssertTrue(message = "Only email, Web and SMS delivery methods are allowed")
        public boolean isValid() {
            return enabledDeliveryMethods.entrySet().stream()
                    .allMatch(entry -> deliveryMethods.contains(entry.getKey()) && entry.getValue() != null);
        }
    }

}
