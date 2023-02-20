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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NotificationRequestStats {

    private final Map<NotificationDeliveryMethod, AtomicInteger> sent;
    private final Map<NotificationDeliveryMethod, Map<String, String>> errors;
    @JsonIgnore
    private final Map<NotificationDeliveryMethod, Set<Object>> processedRecipients;

    public NotificationRequestStats() {
        this.sent = new ConcurrentHashMap<>();
        this.errors = new ConcurrentHashMap<>();
        this.processedRecipients = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public NotificationRequestStats(@JsonProperty("sent") Map<NotificationDeliveryMethod, AtomicInteger> sent,
                                    @JsonProperty("errors") Map<NotificationDeliveryMethod, Map<String, String>> errors) {
        this.sent = sent;
        this.errors = errors;
        this.processedRecipients = Collections.emptyMap();
    }

    public void reportSent(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        sent.computeIfAbsent(deliveryMethod, k -> new AtomicInteger()).incrementAndGet();
        processedRecipients.computeIfAbsent(deliveryMethod, k -> ConcurrentHashMap.newKeySet()).add(recipient.getId());
    }

    public void reportError(NotificationDeliveryMethod deliveryMethod, Throwable error, NotificationRecipient recipient) {
        if (error instanceof AlreadySentException) {
            return;
        }
        String errorMessage = error.getMessage();
        String key;
        if (recipient instanceof User) {
            key = ((User) recipient).getEmail();
        } else {
            key = "";
        }
        errors.computeIfAbsent(deliveryMethod, k -> new ConcurrentHashMap<>()).put(key, errorMessage);
    }

    public boolean contains(NotificationDeliveryMethod deliveryMethod) {
        return sent.containsKey(deliveryMethod) || errors.containsKey(deliveryMethod);
    }

    public boolean contains(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        Set<Object> processedRecipients = this.processedRecipients.get(deliveryMethod);
        return processedRecipients != null && processedRecipients.contains(recipientId);
    }

}
