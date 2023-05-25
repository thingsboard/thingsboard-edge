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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;

import java.util.List;
import java.util.UUID;

public interface RuleNodeRepository extends JpaRepository<RuleNodeEntity, UUID> {

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.ruleChainId in " +
            "(select id from RuleChainEntity rc WHERE rc.tenantId = :tenantId) " +
            "AND r.type = :ruleType AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    List<RuleNodeEntity> findRuleNodesByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("ruleType") String ruleType,
                                                        @Param("searchText") String searchText);

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.type = :ruleType AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    Page<RuleNodeEntity> findAllRuleNodesByType(@Param("ruleType") String ruleType,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.type = :ruleType AND r.configurationVersion < :version AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    Page<RuleNodeEntity> findAllRuleNodesByTypeAndVersionLessThan(@Param("ruleType") String ruleType,
                                                                  @Param("version") int version,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    List<RuleNodeEntity> findRuleNodesByRuleChainIdAndExternalIdIn(UUID ruleChainId, List<UUID> externalIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM RuleNodeEntity e where e.id in :ids")
    void deleteByIdIn(@Param("ids") List<UUID> ids);

}
