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
package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DataSnapshot {

    private volatile boolean ready;
    @Getter
    @Setter
    private long ts;
    private final Set<AlarmConditionFilterKey> keys;
    private final Map<AlarmConditionFilterKey, EntityKeyValue> values = new ConcurrentHashMap<>();

    DataSnapshot(Set<AlarmConditionFilterKey> entityKeysToFetch) {
        this.keys = entityKeysToFetch;
    }

    static AlarmConditionFilterKey toConditionKey(EntityKey key) {
        return new AlarmConditionFilterKey(toConditionKeyType(key.getType()), key.getKey());
    }

    static AlarmConditionKeyType toConditionKeyType(EntityKeyType keyType) {
        switch (keyType) {
            case ATTRIBUTE:
            case SERVER_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case CLIENT_ATTRIBUTE:
                return AlarmConditionKeyType.ATTRIBUTE;
            case TIME_SERIES:
                return AlarmConditionKeyType.TIME_SERIES;
            case ENTITY_FIELD:
                return AlarmConditionKeyType.ENTITY_FIELD;
            default:
                throw new RuntimeException("Not supported entity key: " + keyType.name());
        }
    }

    void removeValue(EntityKey key) {
        values.remove(toConditionKey(key));
    }

    boolean putValue(AlarmConditionFilterKey key, long newTs, EntityKeyValue value) {
        return putIfKeyExists(key, value, ts != newTs);
    }

    private boolean putIfKeyExists(AlarmConditionFilterKey key, EntityKeyValue value, boolean updateOfTs) {
        if (keys.contains(key)) {
            EntityKeyValue oldValue = values.put(key, value);
            if (updateOfTs) {
                return true;
            } else {
                return oldValue == null || !oldValue.equals(value);
            }
        } else {
            return false;
        }
    }

    EntityKeyValue getValue(AlarmConditionFilterKey key) {
        return values.get(key);
    }
}
