/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.cloud;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

@Getter
public enum CloudEventType {
    DASHBOARD(EntityType.DASHBOARD),
    ASSET(EntityType.ASSET),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    DEVICE(EntityType.DEVICE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    ALARM(EntityType.ALARM),
    ALARM_COMMENT(null),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    RULE_CHAIN_METADATA(null),
    USER(EntityType.USER),
    TENANT(EntityType.TENANT),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    CUSTOMER(EntityType.CUSTOMER),
    RELATION(null),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    EDGE(EntityType.EDGE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    ENTITY_GROUP(EntityType.ENTITY_GROUP),
    SCHEDULER_EVENT(EntityType.SCHEDULER_EVENT),
    ROLE(EntityType.ROLE),
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION);

    private final EntityType entityType;

    CloudEventType(EntityType entityType) {
        this.entityType = entityType;
    }
}
