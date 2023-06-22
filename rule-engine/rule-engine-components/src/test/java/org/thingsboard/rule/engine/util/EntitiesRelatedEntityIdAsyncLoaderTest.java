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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

public class EntitiesRelatedEntityIdAsyncLoaderTest {

    private static final EntityId ASSET_ORIGINATOR_ID = new AssetId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final ListeningExecutor DB_EXECUTOR = new ListeningExecutor() {
        @Override
        public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
            try {
                return Futures.immediateFuture(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            command.run();
        }
    };

    private TbContext ctxMock;
    private RelationService relationServiceMock;

    private RelationsQuery relationsQuery;

    @BeforeEach
    void setUp() {
        ctxMock = mock(TbContext.class);
        relationServiceMock = mock(RelationService.class);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(
                EntityRelation.CONTAINS_TYPE, Collections.emptyList()
        );
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
    }

    @Test
    public void givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery() {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(null));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        verify(relationServiceMock, times(1)).findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery));
    }

    @Test
    public void givenRelationsQuery_whenFindEntitiesAsync_ShouldBuildCorrectEntityRelationsQuery() {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(null));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        verify(relationServiceMock, times(1)).findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery));
    }


    @Test
    public void givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 1");
        var device2 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 2");
        var device3 = new Device(new DeviceId(UUID.randomUUID()));
        device3.setName("Device 3");

        var entityRelationDevice1 = new EntityRelation();
        entityRelationDevice1.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice1.setTo(device1.getId());
        entityRelationDevice1.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice2 = new EntityRelation();
        entityRelationDevice2.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice2.setTo(device2.getId());
        entityRelationDevice2.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice3 = new EntityRelation();
        entityRelationDevice3.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice3.setTo(device3.getId());
        entityRelationDevice3.setType(EntityRelation.CONTAINS_TYPE);

        var expectedEntityRelationsList = List.of(entityRelationDevice1, entityRelationDevice2, entityRelationDevice3);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(expectedEntityRelationsList));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var deviceIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        assertNotNull(deviceIdFuture);

        var actualDeviceId = deviceIdFuture.get();
        assertNotNull(actualDeviceId);
        assertEquals(device1.getId(), actualDeviceId);
    }

    @Test
    public void givenSeveralEntitiesFound_whenFindEntitiesAsync_ShouldKeepAll() throws Exception {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 1");
        var device2 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 2");
        var device3 = new Asset(new AssetId(UUID.randomUUID()));
        device3.setName("Device 3");

        var entityRelationDevice1 = new EntityRelation();
        entityRelationDevice1.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice1.setTo(device1.getId());
        entityRelationDevice1.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice2 = new EntityRelation();
        entityRelationDevice2.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice2.setTo(device2.getId());
        entityRelationDevice2.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice3 = new EntityRelation();
        entityRelationDevice3.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice3.setTo(device3.getId());
        entityRelationDevice3.setType(EntityRelation.CONTAINS_TYPE);

        var expectedEntityRelationsList = List.of(entityRelationDevice1, entityRelationDevice2, entityRelationDevice3);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(expectedEntityRelationsList));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var relatedEntityIdsFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        assertNotNull(relatedEntityIdsFuture);

        var actualRelatedEntityIdsFuture = relatedEntityIdsFuture.get();
        assertNotNull(actualRelatedEntityIdsFuture);
        List<EntityId> expectedEntityIdsList = List.of(device1.getId(), device2.getId(), device3.getId());
        assertTrue(actualRelatedEntityIdsFuture.containsAll(expectedEntityIdsList));
    }


    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenOK() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenReturnNull() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, null);
    }

    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenFailure() {
        // GIVEN
        relationsQuery.setDirection(null);
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    @Test
    public void givenRelationQuery_whenFindEntitiesAsync_thenOK() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, ASSET_ORIGINATOR_ID);
    }

    @Test
    public void givenRelationQuery_whenFindEntitiesAsync_thenReturnEmptyList() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, ASSET_ORIGINATOR_ID);
    }

    @Test
    public void givenRelationQuery_whenFindEntitiesAsync_thenFailure() {
        // GIVEN
        relationsQuery.setDirection(null);

        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<List<EntityId>> entityIdListFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctxMock, TENANT_ID, relationsQuery);
        verifyEntityIdListFuture(entityIdListFuture, entityRelations, ASSET_ORIGINATOR_ID);
    }

    private void verifyEntityIdFuture(ListenableFuture<EntityId> entityIdFuture, EntityId assetId) {
        withCallback(entityIdFuture,
                entityId -> assertThat(entityId).isEqualTo(assetId),
                throwable -> assertThat(throwable).isInstanceOf(IllegalStateException.class), ctxMock.getDbCallbackExecutor());
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
                }, ctxMock.getDbCallbackExecutor());
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
