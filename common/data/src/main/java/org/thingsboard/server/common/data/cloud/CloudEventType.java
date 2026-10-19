// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    CALCULATED_FIELD(EntityType.CALCULATED_FIELD);

    private final EntityType entityType;

    CloudEventType(EntityType entityType) {
        this.entityType = entityType;
    }
}
