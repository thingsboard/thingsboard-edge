/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.AUDIT_LOG;
import static org.thingsboard.server.common.data.ObjectType.EVENT;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.OAUTH2_CLIENT;
import static org.thingsboard.server.common.data.ObjectType.OAUTH2_DOMAIN;
import static org.thingsboard.server.common.data.ObjectType.OAUTH2_MOBILE;
import static org.thingsboard.server.common.data.ObjectType.RELATION;
import static org.thingsboard.server.common.data.ObjectType.TENANT;
import static org.thingsboard.server.common.data.ObjectType.TENANT_PROFILE;

@Slf4j
@DaoSqlTest
public class EntityDaoRegistryTest extends AbstractServiceTest {

    @Autowired
    EntityDaoRegistry entityDaoRegistry;

    @Autowired
    List<JpaRepository<?, ?>> repositories;

    @Test
    public void givenAllEntityTypes_whenGetDao_thenAllPresent() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = assertDoesNotThrow(() -> entityDaoRegistry.getDao(entityType));
            assertThat(dao).isNotNull();
        }
    }

    @Test
    public void givenAllDaos_whenFindById_thenOk() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = entityDaoRegistry.getDao(entityType);
            assertDoesNotThrow(() -> {
                dao.findById(TenantId.SYS_TENANT_ID, UUID.randomUUID());
            });
        }
    }

    @Test
    public void givenAllDaos_whenFindIdsByTenantIdAndIdOffset_thenOk() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = entityDaoRegistry.getDao(entityType);
            try {
                dao.findIdsByTenantIdAndIdOffset(TenantId.SYS_TENANT_ID, null, 10);
                dao.findIdsByTenantIdAndIdOffset(TenantId.SYS_TENANT_ID, UUID.randomUUID(), 10);
            } catch (Exception e) {
                String error = ExceptionUtils.getRootCauseMessage(e);
                if (error.contains("tenant_id")) {
                    log.debug("[{}] Ignoring not found tenant_id column", entityType);
                } else {
                    fail("findIdsByTenantIdAndIdOffset for " + entityType + " dao threw error: " + error);
                }
            }
        }
    }

    /*
     * Verifying that all the repositories are successfully bootstrapped, when using Lazy Jpa bootstrap mode
     * */
    @Test
    public void testJpaRepositories() {
        for (var repository : repositories) {
            repository.count();
        }
    }

    @Test
    public void givenAllTenantEntityDaos_whenFindAllByTenantId_thenOk() {
        Set<ObjectType> ignored = EnumSet.of(TENANT, TENANT_PROFILE, RELATION, EVENT, ATTRIBUTE_KV, LATEST_TS_KV, AUDIT_LOG,
                OAUTH2_CLIENT, OAUTH2_DOMAIN, OAUTH2_MOBILE);
        for (ObjectType type : ObjectType.values()) {
            if (ignored.contains(type)) {
                continue;
            }

            TenantEntityDao<?> dao = assertDoesNotThrow(() -> entityDaoRegistry.getTenantEntityDao(type));
            assertDoesNotThrow(() -> {
                dao.findAllByTenantId(tenantId, new PageLink(100));
            });
        }
    }

}
