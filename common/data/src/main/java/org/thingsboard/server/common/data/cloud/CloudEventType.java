// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    RULE_NODE(EntityType.RULE_NODE),
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
    GROUP_PERMISSION(EntityType.GROUP_PERMISSION),
    CALCULATED_FIELD(EntityType.CALCULATED_FIELD),
    REPORT_TEMPLATE(EntityType.REPORT_TEMPLATE),
    AI_MODEL(EntityType.AI_MODEL),
    API_KEY(EntityType.API_KEY);

    private final EntityType entityType;

    CloudEventType(EntityType entityType) {
        this.entityType = entityType;
    }
}
