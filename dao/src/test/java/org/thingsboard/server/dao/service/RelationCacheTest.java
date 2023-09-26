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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.CacheConstants.RELATIONS_CACHE;

@DaoSqlTest
public class RelationCacheTest extends AbstractServiceTest {

    private static final EntityId ENTITY_ID_FROM = new DeviceId(UUID.randomUUID());
    private static final EntityId ENTITY_ID_TO = new DeviceId(UUID.randomUUID());
    private static final String RELATION_TYPE = "Contains";

    @Autowired
    private RelationService relationService;
    @Autowired
    private CacheManager cacheManager;

    private RelationDao relationDao;

    @Before
    public void setup() throws Exception {
        relationDao = mock(RelationDao.class);
        ReflectionTestUtils.setField(unwrapRelationService(), "relationDao", relationDao);
    }

    @After
    public void cleanup() {
        cacheManager.getCache(RELATIONS_CACHE).clear();
    }

    private RelationService unwrapRelationService() throws Exception {
        if (AopUtils.isAopProxy(relationService) && relationService instanceof Advised) {
            Object target = ((Advised) relationService).getTargetSource().getTarget();
            return (RelationService) target;
        }
        return relationService;
    }

    @Test
    public void testFindRelationByFrom_Cached() throws ExecutionException, InterruptedException {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }

    @Test
    public void testDeleteRelations_EvictsCache() {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.deleteRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(2)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }
}
