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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public interface WidgetsBundleRepository extends JpaRepository<WidgetsBundleEntity, UUID>, ExportableEntityRepository<WidgetsBundleEntity> {

    WidgetsBundleEntity findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<WidgetsBundleEntity> findSystemWidgetsBundles(@Param("systemTenantId") UUID systemTenantId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :tenantId " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<WidgetsBundleEntity> findTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantId, :nullTenantId) " +
            "AND LOWER(wb.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantId(@Param("tenantId") UUID tenantId,
                                                                    @Param("nullTenantId") UUID nullTenantId,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    WidgetsBundleEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT externalId FROM WidgetsBundleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId = :systemTenantId " +
            "AND wb.id IN :widgetsBundleIds")
    List<WidgetsBundleEntity> findSystemWidgetsBundlesByIdIn(@Param("systemTenantId") UUID systemTenantId,
                                                             @Param("widgetsBundleIds") List<UUID> widgetsBundleIds);

    @Query("SELECT wb FROM WidgetsBundleEntity wb WHERE wb.tenantId IN (:tenantId, :nullTenantId) " +
            "AND wb.id IN :widgetsBundleIds")
    List<WidgetsBundleEntity> findAllTenantWidgetsBundlesByTenantIdAndIdIn(@Param("tenantId") UUID tenantId,
                                                                           @Param("nullTenantId") UUID nullTenantId,
                                                                           @Param("widgetsBundleIds") List<UUID> widgetsBundleIds);

}
