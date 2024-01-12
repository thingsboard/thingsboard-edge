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
package org.thingsboard.server.common.data.notification.rule.trigger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlarmTrigger.class, name = "ALARM"),
        @JsonSubTypes.Type(value = DeviceActivityTrigger.class, name = "DEVICE_ACTIVITY"),
        @JsonSubTypes.Type(value = EntityActionTrigger.class, name = "ENTITY_ACTION"),
        @JsonSubTypes.Type(value = AlarmCommentTrigger.class, name = "ALARM_COMMENT"),
        @JsonSubTypes.Type(value = RuleEngineComponentLifecycleEventTrigger.class, name = "RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT"),
        @JsonSubTypes.Type(value = AlarmAssignmentTrigger.class, name = "ALARM_ASSIGNMENT"),
        @JsonSubTypes.Type(value = NewPlatformVersionTrigger.class, name = "NEW_PLATFORM_VERSION"),
        @JsonSubTypes.Type(value = EntitiesLimitTrigger.class, name = "ENTITIES_LIMIT"),
        @JsonSubTypes.Type(value = ApiUsageLimitTrigger.class, name = "API_USAGE_LIMIT"),
        @JsonSubTypes.Type(value = IntegrationLifecycleEventTrigger.class, name = "INTEGRATION_LIFECYCLE_EVENT"),
        @JsonSubTypes.Type(value = RateLimitsTrigger.class, name = "RATE_LIMITS"),
})

public interface NotificationRuleTrigger extends Serializable {

    NotificationRuleTriggerType getType();

    TenantId getTenantId();

    EntityId getOriginatorEntityId();

    @JsonIgnore
    default boolean deduplicate() {
        return false;
    }

    @JsonIgnore
    default String getDeduplicationKey() {
        EntityId originatorEntityId = getOriginatorEntityId();
        return String.join(":", getType().toString(), originatorEntityId.getEntityType().toString(), originatorEntityId.getId().toString());
    }

    @JsonIgnore
    default long getDefaultDeduplicationDuration() {
        return 0;
    }

}
