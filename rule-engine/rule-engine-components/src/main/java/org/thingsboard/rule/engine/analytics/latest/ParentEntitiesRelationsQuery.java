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
package org.thingsboard.rule.engine.analytics.latest;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParentEntitiesRelationsQuery implements ParentEntitiesQuery {

    private EntityId rootEntityId;
    private RelationsQuery relationsQuery;
    private RelationsQuery childRelationsQuery;
    private boolean includeRootEntity;

    @Override
    public ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx) {
        ListenableFuture<List<EntityId>> parentEntities = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, rootEntityId, relationsQuery,
                entityId -> ctx.getPeContext().isLocalEntity(entityId));
        if (includeRootEntity) {
            return Futures.transform(parentEntities, entityIds -> {
                List<EntityId> newEntityIds = new ArrayList<>(entityIds);
                if (!newEntityIds.contains(rootEntityId)) {
                    newEntityIds.add(rootEntityId);
                }
                return newEntityIds;
            }, ctx.getDbCallbackExecutor());
        }
        return parentEntities;
    }

    @Override
    public ListenableFuture<List<EntityId>> getChildEntitiesAsync(TbContext ctx, EntityId parentEntityId) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, parentEntityId, childRelationsQuery);
    }

    @Override
    public boolean useParentEntitiesOnlyForSimpleAggregation() {
        return true;
    }

}
