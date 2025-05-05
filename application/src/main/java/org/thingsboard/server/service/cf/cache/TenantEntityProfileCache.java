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
package org.thingsboard.server.service.cf.cache;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TenantEntityProfileCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<EntityId, Set<EntityId>> allEntities = new HashMap<>();

    public void removeProfileId(EntityId profileId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            allEntities.remove(profileId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeEntityId(EntityId entityId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            allEntities.values().forEach(set -> set.remove(entityId));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(EntityId profileId, EntityId entityId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            removeSafely(allEntities, profileId, entityId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void add(EntityId profileId, EntityId entityId) {
        lock.writeLock().lock();
        try {
            if (EntityType.DEVICE.equals(profileId.getEntityType()) || EntityType.ASSET.equals(profileId.getEntityType())) {
                throw new RuntimeException("Entity type '" + profileId.getEntityType() + "' is not a profileId.");
            }
            allEntities.computeIfAbsent(profileId, k -> new HashSet<>()).add(entityId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void update(EntityId oldProfileId, EntityId newProfileId, EntityId entityId) {
        remove(oldProfileId, entityId);
        add(newProfileId, entityId);
    }

    public Collection<EntityId> getEntityIdsByProfileId(EntityId profileId) {
        lock.readLock().lock();
        try {
            var entities = allEntities.getOrDefault(profileId, Collections.emptySet());
            List<EntityId> result = new ArrayList<>(entities.size());
            result.addAll(entities);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void removeSafely(Map<EntityId, Set<EntityId>> map, EntityId profileId, EntityId entityId) {
        var set = map.get(profileId);
        if (set != null) {
            set.remove(entityId);
        }
    }
}
