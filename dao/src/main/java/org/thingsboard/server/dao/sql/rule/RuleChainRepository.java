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
package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;

import java.util.List;
import java.util.UUID;

public interface RuleChainRepository extends JpaRepository<RuleChainEntity, UUID>, ExportableEntityRepository<RuleChainEntity> {

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND LOWER(rc.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND rc.type = :type " +
            "AND LOWER(rc.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                @Param("type") RuleChainType type,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(rc.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                  @Param("edgeId") UUID edgeId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE_AUTO_ASSIGN_RULE_CHAIN' " +
            "AND re.relationType = 'Contains' AND re.fromId = :tenantId AND re.fromType = 'TENANT' " +
            "AND LOWER(rc.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findAutoAssignByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);


    RuleChainEntity findByTenantIdAndTypeAndRootIsTrue(UUID tenantId, RuleChainType ruleChainType);

    Long countByTenantId(UUID tenantId);

    List<RuleChainEntity> findByTenantIdAndTypeAndName(UUID tenantId, RuleChainType type, String name);

    @Query("SELECT externalId FROM RuleChainEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
