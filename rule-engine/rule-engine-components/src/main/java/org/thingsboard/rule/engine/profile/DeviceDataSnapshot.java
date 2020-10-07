/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceDataSnapshot {

    private volatile boolean ready;
    @Getter
    @Setter
    private long ts;
    private final Set<EntityKey> keys;
    private final Map<EntityKey, EntityKeyValue> values = new ConcurrentHashMap<>();

    public DeviceDataSnapshot(Set<EntityKey> entityKeysToFetch) {
        this.keys = entityKeysToFetch;
    }

    void removeValue(EntityKey key) {
        switch (key.getType()) {
            case ATTRIBUTE:
                values.remove(key);
                values.remove(getAttrKey(key, EntityKeyType.CLIENT_ATTRIBUTE));
                values.remove(getAttrKey(key, EntityKeyType.SHARED_ATTRIBUTE));
                values.remove(getAttrKey(key, EntityKeyType.SERVER_ATTRIBUTE));
                break;
            case CLIENT_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case SERVER_ATTRIBUTE:
                values.remove(key);
                values.remove(getAttrKey(key, EntityKeyType.ATTRIBUTE));
                break;
            default:
                values.remove(key);
        }
    }

    void putValue(EntityKey key, EntityKeyValue value) {
        switch (key.getType()) {
            case ATTRIBUTE:
                putIfKeyExists(key, value);
                putIfKeyExists(getAttrKey(key, EntityKeyType.CLIENT_ATTRIBUTE), value);
                putIfKeyExists(getAttrKey(key, EntityKeyType.SHARED_ATTRIBUTE), value);
                putIfKeyExists(getAttrKey(key, EntityKeyType.SERVER_ATTRIBUTE), value);
                break;
            case CLIENT_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case SERVER_ATTRIBUTE:
                putIfKeyExists(key, value);
                putIfKeyExists(getAttrKey(key, EntityKeyType.ATTRIBUTE), value);
                break;
            default:
                putIfKeyExists(key, value);
        }
    }

    private void putIfKeyExists(EntityKey key, EntityKeyValue value) {
        if (keys.contains(key)) {
            values.put(key, value);
        }
    }

    EntityKeyValue getValue(EntityKey key) {
        if (EntityKeyType.ATTRIBUTE.equals(key.getType())) {
            EntityKeyValue value = values.get(key);
            if (value == null) {
                value = values.get(getAttrKey(key, EntityKeyType.CLIENT_ATTRIBUTE));
                if (value == null) {
                    value = values.get(getAttrKey(key, EntityKeyType.SHARED_ATTRIBUTE));
                    if (value == null) {
                        value = values.get(getAttrKey(key, EntityKeyType.SERVER_ATTRIBUTE));
                    }
                }
            }
            return value;
        } else {
            return values.get(key);
        }
    }

    private EntityKey getAttrKey(EntityKey key, EntityKeyType clientAttribute) {
        return new EntityKey(clientAttribute, key.getKey());
    }
}
