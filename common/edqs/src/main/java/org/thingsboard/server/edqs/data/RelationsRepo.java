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
package org.thingsboard.server.edqs.data;

import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NoArgsConstructor
public class RelationsRepo {

    private final ConcurrentMap<UUID, Set<RelationInfo>> fromRelations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<RelationInfo>> toRelations = new ConcurrentHashMap<>();

    public boolean add(EntityData<?> from, EntityData<?> to, String type) {
        boolean addedFromRelation = fromRelations.computeIfAbsent(from.getId(), k -> ConcurrentHashMap.newKeySet()).add(new RelationInfo(type, to));
        boolean addedToRelation = toRelations.computeIfAbsent(to.getId(), k -> ConcurrentHashMap.newKeySet()).add(new RelationInfo(type, from));
        return addedFromRelation || addedToRelation;
    }

    public Set<RelationInfo> getFrom(UUID entityId) {
        var result = fromRelations.get(entityId);
        return result == null ? Collections.emptySet() : result;
    }

    public Set<RelationInfo> getTo(UUID entityId) {
        var result = toRelations.get(entityId);
        return result == null ? Collections.emptySet() : result;
    }

    public boolean remove(UUID from, UUID to, String type) {
        boolean removedFromRelation = false;
        boolean removedToRelation = false;
        Set<RelationInfo> fromRelations = this.fromRelations.get(from);
        if (fromRelations != null) {
            removedFromRelation = fromRelations.removeIf(relationInfo -> relationInfo.getTarget().getId().equals(to) && relationInfo.getType().equals(type));
        }
        Set<RelationInfo> toRelations = this.toRelations.get(to);
        if (toRelations != null) {
            removedToRelation = toRelations.removeIf(relationInfo -> relationInfo.getTarget().getId().equals(from) && relationInfo.getType().equals(type));
        }
        return removedFromRelation || removedToRelation;
    }

}
