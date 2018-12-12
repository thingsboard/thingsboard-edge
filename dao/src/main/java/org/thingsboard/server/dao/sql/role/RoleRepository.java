/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.role;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface RoleRepository extends CrudRepository<RoleEntity, String> {

    RoleEntity findByTenantIdAndCustomerIdAndName(String tenantId, String customerId, String name);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId " +
            "AND LOWER(r.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND r.id > :idOffset ORDER BY r.id")
    List<RoleEntity> findByTenantIdAndCustomerId(@Param("tenantId") String tenantId,
                                                  @Param("customerId") String customerId,
                                                  @Param("textSearch") String textSearch,
                                                  @Param("idOffset") String idOffset,
                                                  Pageable pageable);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId AND r.type = :type " +
            "AND LOWER(r.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND r.id > :idOffset ORDER BY r.id")
    List<RoleEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") String tenantId,
                                                         @Param("customerId") String customerId,
                                                         @Param("type") RoleType type,
                                                         @Param("textSearch") String textSearch,
                                                         @Param("idOffset") String idOffset,
                                                         Pageable pageable);


}
