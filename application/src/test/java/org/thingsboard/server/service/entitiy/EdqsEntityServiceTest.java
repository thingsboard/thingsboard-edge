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
package org.thingsboard.server.service.entitiy;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api_enabled=true",
        "queue.edqs.mode=local"
})
public class EdqsEntityServiceTest extends EntityServiceTest {

    @Autowired
    private EdqsApiService edqsApiService;

    @MockBean
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void beforeEach() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsApiService.isEnabled());
    }

    // sql implementation has a bug with data duplication, edqs implementation returns correct value
    @Override
    @Test
    public void testCountHierarchicalEntitiesByMultiRootQuery() throws InterruptedException {
        List<Asset> buildings = new ArrayList<>();
        List<Asset> apartments = new ArrayList<>();
        Map<String, Map<UUID, String>> entityNameByTypeMap = new HashMap<>();
        Map<UUID, UUID> childParentRelationMap = new HashMap<>();
        createMultiRootHierarchy(buildings, apartments, entityNameByTypeMap, childParentRelationMap);

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setMultiRoot(true);
        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(buildings.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 63);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 27);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 3);

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, MergedUserPermissions permissions, EntityDataQuery query, long expectedResultSize) {
        return await().atMost(15, TimeUnit.SECONDS).until(() -> findByQuery(customerId, permissions, query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected List<EntityData> findByQueryAndCheckTelemetry(EntityDataQuery query, EntityKeyType entityKeyType, String key, List<String> expectedTelemetries) {
        return await().atMost(15, TimeUnit.SECONDS).until(() -> findEntitiesTelemetry(query, entityKeyType, key, expectedTelemetries),
                loadedEntities -> loadedEntities.stream().map(entityData -> entityData.getLatest().get(entityKeyType).get(key).getValue()).toList().containsAll(expectedTelemetries));
    }

    @Override
    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, countQuery, expectedResult);
    }

    @Override
    protected long countByQueryAndCheck(CustomerId customerId, MergedUserPermissions permissions, EntityCountQuery query, int expectedResult) {
        return await().atMost(15, TimeUnit.SECONDS).until(() -> countByQuery(customerId, permissions, query),
                result -> result == expectedResult);
    }

}
