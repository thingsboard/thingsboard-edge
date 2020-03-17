/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;

import java.util.List;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
public interface EntityViewRepository extends CrudRepository<EntityViewEntity, String> {

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantId(@Param("tenantId") String tenantId,
                                          @Param("textSearch") String textSearch,
                                          @Param("idOffset") String idOffset,
                                          Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndType(@Param("tenantId") String tenantId,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 @Param("idOffset") String idOffset,
                                                 Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndCustomerId(@Param("tenantId") String tenantId,
                                                       @Param("customerId") String customerId,
                                                       @Param("searchText") String searchText,
                                                       @Param("idOffset") String idOffset,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND e.id > :idOffset ORDER BY e.id")
    List<EntityViewEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") String tenantId,
                                                              @Param("customerId") String customerId,
                                                              @Param("type") String type,
                                                              @Param("searchText") String searchText,
                                                              @Param("idOffset") String idOffset,
                                                              Pageable pageable);

    EntityViewEntity findByTenantIdAndName(String tenantId, String name);

    List<EntityViewEntity> findAllByTenantIdAndEntityId(String tenantId, String entityId);

    @Query("SELECT DISTINCT ev.type FROM EntityViewEntity ev WHERE ev.tenantId = :tenantId")
    List<String> findTenantEntityViewTypes(@Param("tenantId") String tenantId);

    List<EntityViewEntity> findEntityViewsByTenantIdAndIdIn(String tenantId, List<String> entityViewIds);
}
