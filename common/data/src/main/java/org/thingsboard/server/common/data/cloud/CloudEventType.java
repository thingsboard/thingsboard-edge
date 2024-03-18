/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    TB_RESOURCE(EntityType.TB_RESOURCE);

    private final EntityType entityType;

    CloudEventType(EntityType entityType) {
        this.entityType = entityType;
    }
}
