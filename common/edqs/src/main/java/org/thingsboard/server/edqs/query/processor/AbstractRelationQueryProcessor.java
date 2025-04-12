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
package org.thingsboard.server.edqs.query.processor;

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.data.RelationsRepo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public abstract class AbstractRelationQueryProcessor<T extends EntityFilter> extends AbstractQueryProcessor<T> {

    public static final int MAXIMUM_QUERY_LEVEL = 100;

    public AbstractRelationQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter) {
        super(repo, ctx, query, filter);
    }

    protected abstract Set<UUID> getRootEntities();

    protected abstract EntitySearchDirection getDirection();

    protected abstract int getMaxLevel();

    protected abstract boolean isFetchLastLevelOnly();

    protected boolean isMultiRoot() {
        return false;
    }

    @Override
    public List<SortableEntityData> processQuery() {
        var relations = repository.getRelations(RelationTypeGroup.COMMON);
        var entities = getEntitiesSet(relations);
        if (ctx.isTenantUser()) {
            return processTenantQuery(entities);
        } else {
            return processCustomerQuery(entities);
        }
    }

    @Override
    public long count() {
        var relations = repository.getRelations(RelationTypeGroup.COMMON);
        var entities = getEntitiesSet(relations);
        long result = 0;

        RelationQueryPermissions[] permissionsArray = buildPermissionsArray();
        if (ctx.isTenantUser()) {
            for (EntityData<?> ed : entities) {
                var permissions = permissionsArray[ed.getEntityType().ordinal()];
                if (permissions != null) {
                    if (permissions.isHasGroups()) {
                        CombinedPermissions combinedPermissions = getCombinedPermissions(ed.getId(),
                                permissions.isReadEntity(), permissions.isReadAttrs(), permissions.isReadTs(), permissions.getGroupPermissions());
                        if (combinedPermissions.isRead()) {
                            result++;
                        }
                    } else if (permissions.isReadEntity()) {
                        result++;
                    }
                }
            }
        } else {
            var customerIds = repository.getAllCustomers(ctx.getCustomerId().getId());
            for (EntityData<?> ed : entities) {
                var permissions = permissionsArray[ed.getEntityType().ordinal()];
                if (permissions != null) {
                    boolean isReadEntity = permissions.isReadEntity() && ed.getPermissionCustomerId() != null && customerIds.contains(ed.getPermissionCustomerId());
                    if (permissions.isHasGroups()) {
                        CombinedPermissions combinedPermissions = getCombinedPermissions(ed.getId(),
                                isReadEntity,
                                permissions.isReadAttrs(), permissions.isReadTs(), permissions.getGroupPermissions());
                        if (combinedPermissions.isRead()) {
                            result++;
                        }
                    } else if (isReadEntity) {
                        result++;
                    }
                }
            }
            return result;
        }
        return result;
    }

    private List<SortableEntityData> processTenantQuery(Set<EntityData<?>> entities) {
        List<SortableEntityData> result = new ArrayList<>();
        RelationQueryPermissions[] permissionsArray = buildPermissionsArray();
        for (EntityData<?> ed : entities) {
            var permissions = permissionsArray[ed.getEntityType().ordinal()];
            if (permissions != null) {
                if (permissions.isHasGroups()) {
                    CombinedPermissions combinedPermissions = getCombinedPermissions(ed.getId(),
                            permissions.isReadEntity(), permissions.isReadAttrs(), permissions.isReadTs(), permissions.getGroupPermissions());
                    if (combinedPermissions.isRead()) {
                        SortableEntityData sortData = new SortableEntityData(ed);
                        sortData.setSortValue(getSortValue(ed, sortKey, ctx));
                        sortData.setReadAttrs(combinedPermissions.isReadAttrs());
                        sortData.setReadTs(combinedPermissions.isReadTs());
                        result.add(sortData);
                    }
                } else if (permissions.isReadEntity()) {
                    result.add(toSortData(ed, permissions));
                }
            }
        }
        return result;
    }

    private List<SortableEntityData> processCustomerQuery(Set<EntityData<?>> entities) {
        var customerIds = repository.getAllCustomers(ctx.getCustomerId().getId());
        RelationQueryPermissions[] permissionsArray = buildPermissionsArray();
        List<SortableEntityData> result = new ArrayList<>();
        for (EntityData<?> ed : entities) {
            var permissions = permissionsArray[ed.getEntityType().ordinal()];
            if (permissions != null) {
                boolean isReadEntity = permissions.isReadEntity() && ed.getPermissionCustomerId() != null && customerIds.contains(ed.getPermissionCustomerId());
                if (permissions.isHasGroups()) {
                    SortableEntityData sortData = new SortableEntityData(ed);
                    sortData.setSortValue(getSortValue(ed, sortKey, ctx));
                    CombinedPermissions combinedPermissions = getCombinedPermissions(ed.getId(),
                            isReadEntity,
                            permissions.isReadAttrs(), permissions.isReadTs(), permissions.getGroupPermissions());
                    if (combinedPermissions.isRead()) {
                        sortData.setReadAttrs(combinedPermissions.isReadAttrs());
                        sortData.setReadTs(combinedPermissions.isReadTs());
                        result.add(sortData);
                    }
                } else if (isReadEntity) {
                    result.add(toSortData(ed, permissions));
                }
            }
        }
        return result;
    }

    private RelationQueryPermissions[] buildPermissionsArray() {
        RelationQueryPermissions[] permissionsArray = new RelationQueryPermissions[EntityType.values().length];
        var readEntityPermissionsMap = ctx.getMergedReadEntityPermissionsMap();
        var readAttrPermissionsMap = ctx.getMergedReadAttrPermissionsMap();
        var readTsPermissionsMap = ctx.getMergedReadTsPermissionsMap();
        for (EntityType et : EntityType.values()) {
            var resource = Resource.resourceFromEntityType(et);
            if (resource == null) {
                continue;
            }
            var readEntityPermissions = readEntityPermissionsMap.get(resource);
            var readAttrPermissions = readAttrPermissionsMap.get(resource);
            var readTsPermissions = readTsPermissionsMap.get(resource);
            var groupPermissions = toGroupPermissions(readEntityPermissions, readAttrPermissions, readTsPermissions);
            RelationQueryPermissions entityPermissions = RelationQueryPermissions
                    .builder()
                    .readEntity(readEntityPermissions.isHasGenericRead())
                    .readAttrs(readAttrPermissions.isHasGenericRead())
                    .readTs(readTsPermissions.isHasGenericRead())
                    .hasGroups(!groupPermissions.isEmpty())
                    .groupPermissions(groupPermissions)
                    .build();
            permissionsArray[et.ordinal()] = entityPermissions;
        }
        return permissionsArray;
    }

    private Set<EntityData<?>> getEntitiesSet(RelationsRepo relations) {
        Set<EntityData<?>> result = new HashSet<>();
        Set<UUID> processed = new HashSet<>();
        Queue<RelationSearchTask> tasks = new LinkedList<>();
        int maxLvl = getMaxLevel() == 0 ? MAXIMUM_QUERY_LEVEL : Math.max(1, getMaxLevel());
        for (UUID uuid : getRootEntities()) {
            tasks.add(new RelationSearchTask(uuid, 0));
        }
        while (!tasks.isEmpty()) {
            RelationSearchTask task = tasks.poll();
            if (processed.add(task.entityId)) {
                var entityLvl = task.lvl + 1;
                Set<RelationInfo> entities = EntitySearchDirection.FROM.equals(getDirection()) ? relations.getFrom(task.entityId) : relations.getTo(task.entityId);
                if (isFetchLastLevelOnly() && entities.isEmpty() && task.previous != null && check(task.previous)) {
                    result.add(task.previous.getTarget());
                }
                for (RelationInfo relationInfo : entities) {
                    var entity = relationInfo.getTarget();
                    if (entity.isEmpty()) {
                        continue;
                    }
                    var entityId = entity.getId();
                    if (isFetchLastLevelOnly()) {
                        if (entityLvl < maxLvl) {
                            tasks.add(new RelationSearchTask(entityId, entityLvl, relationInfo));
                        } else if (entityLvl == maxLvl) {
                            if (check(relationInfo)) {
                                if (isMultiRoot()) {
                                    ctx.getRelatedParentIdMap().put(entity.getId(), task.entityId);
                                }
                                result.add(entity);
                            }
                        }
                    } else {
                        if (check(relationInfo)) {
                            if (isMultiRoot()) {
                                ctx.getRelatedParentIdMap().put(entity.getId(), task.entityId);
                            }
                            result.add(entity);
                        }
                        if (entityLvl < maxLvl) {
                            tasks.add(new RelationSearchTask(entityId, entityLvl));
                        }
                    }
                }
            }
        }
        return result;
    }

    protected abstract boolean check(RelationInfo relationInfo);

    @RequiredArgsConstructor
    private static class RelationSearchTask {
        private final UUID entityId;
        private final int lvl;
        private final RelationInfo previous;

        public RelationSearchTask(UUID entityId, int lvl) {
            this(entityId, lvl, null);
        }

    }

}
