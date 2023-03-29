/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.cloud.CloudEventType;

@Slf4j
public final class CloudUtils {

    private CloudUtils() {
    }

    public static CloudEventType getCloudEventTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return CloudEventType.DEVICE;
            case ASSET:
                return CloudEventType.ASSET;
            case ENTITY_VIEW:
                return CloudEventType.ENTITY_VIEW;
            case DASHBOARD:
                return CloudEventType.DASHBOARD;
            case USER:
                return CloudEventType.USER;
            case ALARM:
                return CloudEventType.ALARM;
            case TENANT:
                return CloudEventType.TENANT;
            case CUSTOMER:
                return CloudEventType.CUSTOMER;
            case EDGE:
                return CloudEventType.EDGE;
            default:
                log.warn("Unsupported entity type: [{}]", entityType);
                return null;
        }
    }
}
