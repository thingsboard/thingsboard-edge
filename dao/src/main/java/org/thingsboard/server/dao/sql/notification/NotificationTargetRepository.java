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
package org.thingsboard.server.dao.sql.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.NotificationTargetEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationTargetRepository extends JpaRepository<NotificationTargetEntity, UUID> {

    @Query("SELECT t FROM NotificationTargetEntity t WHERE t.tenantId = :tenantId " +
            "AND (:searchText = '' OR lower(t.name) LIKE lower(concat('%', :searchText, '%')))")
    Page<NotificationTargetEntity> findByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                               @Param("searchText") String searchText,
                                                               Pageable pageable);

    @Query(value = "SELECT * FROM notification_target t WHERE t.tenant_id = :tenantId " +
            "AND (:searchText = '' OR lower(t.name) LIKE lower(concat('%', :searchText, '%'))) " +
            "AND (cast(t.configuration as json) ->> 'type' <> 'PLATFORM_USERS' OR " +
            "cast(t.configuration as json) -> 'usersFilter' ->> 'type' IN :usersFilterTypes)", nativeQuery = true)
    Page<NotificationTargetEntity> findByTenantIdAndSearchTextAndUsersFilterTypeIfPresent(@Param("tenantId") UUID tenantId,
                                                                                          @Param("searchText") String searchText,
                                                                                          @Param("usersFilterTypes") List<String> usersFilterTypes,
                                                                                          Pageable pageable);

    List<NotificationTargetEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);

    @Transactional
    void deleteByTenantId(UUID tenantId);

}
