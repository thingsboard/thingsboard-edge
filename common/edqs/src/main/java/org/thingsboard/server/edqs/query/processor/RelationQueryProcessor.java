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

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RelationQueryProcessor extends AbstractRelationQueryProcessor<RelationsQueryFilter> {

    private final boolean hasFilters;

    public RelationQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (RelationsQueryFilter) query.getEntityFilter());
        this.hasFilters = filter.getFilters() != null && !filter.getFilters().isEmpty();
    }

    @Override
    public Set<UUID> getRootEntities() {
        if (filter.isMultiRoot()) {
            return filter.getMultiRootEntityIds().stream().map(UUID::fromString).collect(Collectors.toSet());
        } else {
            return Set.of(filter.getRootEntity().getId());
        }
    }

    @Override
    public EntitySearchDirection getDirection() {
        return filter.getDirection();
    }

    @Override
    public int getMaxLevel() {
        return filter.getMaxLevel();
    }

    @Override
    public boolean isMultiRoot() {
        return filter.isMultiRoot();
    }

    @Override
    public boolean isFetchLastLevelOnly() {
        return filter.isFetchLastLevelOnly();
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        if (hasFilters) {
            for (var f : filter.getFilters()) {
                if (((!filter.isNegate() && !f.isNegate()) || (filter.isNegate() && f.isNegate())) == f.getRelationType().equals(relationInfo.getType())) {
                    if (f.getEntityTypes() == null || f.getEntityTypes().isEmpty()
                            || f.getEntityTypes().contains(relationInfo.getTarget().getEntityType())) {
                        return super.matches(relationInfo.getTarget());
                    }
                }
            }
            return false;
        } else {
            return super.matches(relationInfo.getTarget());
        }
    }

}
