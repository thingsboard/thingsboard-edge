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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

public class EntitiesRelatedEntityIdAsyncLoaderTest {

    private TbContext ctx;

    private RelationService relationService;

    private RelationsQuery relationsQuery;

    private EntityId assetId;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        relationService = mock(RelationService.class);
        assetId = new AssetId(Uuids.timeBased());
        tenantId = new TenantId(Uuids.timeBased());

        when(ctx.getRelationService()).thenReturn(relationService);

        relationsQuery = new RelationsQuery();
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
    }

    @Test
    public void findEntityAsync_thenOK() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(EntitySearchDirection.FROM);

        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(tenantId, assetId));

        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdFuture(entityIdFuture, assetId);
    }

    @Test
    public void findEntityAsync_when_returnNull() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(EntitySearchDirection.FROM);

        List<EntityRelation> entityRelations = new ArrayList<>();
        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdFuture(entityIdFuture, null);
    }

    @Test
    public void findEntityAsync_thenFailure() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(null);

        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(tenantId, assetId));

        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdFuture(entityIdFuture, assetId);
    }

    @Test
    public void findEntitiesAsync_thenOK() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(EntitySearchDirection.FROM);

        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(tenantId, assetId));

        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, assetId);
    }

    @Test
    public void findEntitiesAsync_when_returnEmptyList() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(EntitySearchDirection.FROM);

        List<EntityRelation> entityRelations = new ArrayList<>();

        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, assetId);
    }

    @Test
    public void findEntitiesAsync_thenFailure() throws ExecutionException, InterruptedException {
        relationsQuery.setDirection(null);

        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(tenantId, assetId));

        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, tenantId, relationsQuery);
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, assetId);
    }

    private void verifyEntityIdFuture(ListenableFuture<EntityId> entityIdFuture, EntityId assetId) {
        withCallback(entityIdFuture,
                entityId -> {
                    assertThat(entityId).isEqualTo(assetId);
                },
                throwable -> {
                    assertThat(throwable).isInstanceOf(IllegalStateException.class);
                }, ctx.getDbCallbackExecutor());
    }

    private void verifyEntityIdListFuture(ListenableFuture<List<EntityId>> entityIdListFuture, List<EntityRelation> entityRelations, EntityId assetId) {
        withCallback(entityIdListFuture,
                entityIdList -> {
                    assertThat(entityIdList).isInstanceOf(List.class);
                    assertThat(entityIdList.size()).isEqualTo(entityRelations.size());
                    if (entityIdList.size() > 0) {
                        assertThat(entityIdList.get(0)).isEqualTo(assetId);
                    }
                },
                throwable -> {
                    assertThat(throwable).isInstanceOf(IllegalStateException.class);
                }, ctx.getDbCallbackExecutor());
    }

    private static EntityRelation createEntityRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return relation;
    }
}
