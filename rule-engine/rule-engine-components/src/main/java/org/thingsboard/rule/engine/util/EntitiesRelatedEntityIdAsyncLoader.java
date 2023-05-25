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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EntitiesRelatedEntityIdAsyncLoader {

    public static ListenableFuture<EntityId> findEntityAsync(
            TbContext ctx,
            EntityId originator,
            RelationsQuery relationsQuery
    ) {
        var relationService = ctx.getRelationService();
        var query = buildQuery(originator, relationsQuery);
        var relationListFuture = relationService.findByQuery(ctx.getTenantId(), query);
        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            return Futures.transformAsync(relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ?
                            Futures.immediateFuture(relationList.get(0).getTo())
                            : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            return Futures.transformAsync(relationListFuture,
                    relationList -> CollectionUtils.isNotEmpty(relationList) ?
                            Futures.immediateFuture(relationList.get(0).getFrom())
                            : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
        }
        return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
    }

    public static ListenableFuture<List<EntityId>> findEntitiesAsync(TbContext ctx, EntityId originator,
                                                                     RelationsQuery relationsQuery) {
        return findEntitiesAsync(ctx, originator, relationsQuery, entityId -> true);
    }

    public static ListenableFuture<List<EntityId>> findEntitiesAsync(TbContext ctx, EntityId originator,
                                                                     RelationsQuery relationsQuery, Predicate<EntityId> entityFilter) {
        RelationService relationService = ctx.getRelationService();
        EntityRelationsQuery query = buildQuery(originator, relationsQuery);
        ListenableFuture<List<EntityRelation>> asyncRelation = relationService.findByQuery(ctx.getTenantId(), query);

        Function<EntityRelation, EntityId> mapFunction;

        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            mapFunction = EntityRelation::getTo;
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            mapFunction = EntityRelation::getFrom;
        } else {
            return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
        }

        return Futures.transformAsync(asyncRelation, r -> CollectionUtils.isNotEmpty(r)
                ? Futures.immediateFuture(r.stream().map(mapFunction).filter(entityFilter).collect(Collectors.toList()))
                : Futures.immediateFuture(Collections.emptyList()), ctx.getDbCallbackExecutor());
    }

    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        var query = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

}
