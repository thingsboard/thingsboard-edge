// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.cloud.CloudEventType;

import java.util.EnumMap;

@Slf4j
public final class CloudUtils {

    private static final EnumMap<EntityType, CloudEventType> entityTypeCloudEventTypeEnumMap;

    static {
        entityTypeCloudEventTypeEnumMap = new EnumMap<>(EntityType.class);
        for (CloudEventType cloudEventType : CloudEventType.values()) {
            if (cloudEventType.getEntityType() != null) {
                entityTypeCloudEventTypeEnumMap.put(cloudEventType.getEntityType(), cloudEventType);
            }
        }
    }

    private CloudUtils() {}

    public static CloudEventType getCloudEventTypeByEntityType(EntityType entityType) {
        return entityTypeCloudEventTypeEnumMap.get(entityType);
    }

}
