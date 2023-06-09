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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetTypeRepository extends JpaRepository<WidgetTypeDetailsEntity, UUID> {

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.id = :widgetTypeId")
    WidgetTypeEntity findWidgetTypeById(@Param("widgetTypeId") UUID widgetTypeId);

    @Query("SELECT wt FROM WidgetTypeEntity wt WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias")
    List<WidgetTypeEntity> findWidgetTypesByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                   @Param("bundleAlias") String bundleAlias);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity(wtd) FROM WidgetTypeDetailsEntity wtd " +
            "WHERE wtd.tenantId = :tenantId AND wtd.bundleAlias = :bundleAlias")
    List<WidgetTypeInfoEntity> findWidgetTypesInfosByTenantIdAndBundleAlias(@Param("tenantId") UUID tenantId,
                                                                            @Param("bundleAlias") String bundleAlias);

    List<WidgetTypeDetailsEntity> findByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    @Query("SELECT wt FROM WidgetTypeEntity wt " +
            "WHERE wt.tenantId = :tenantId AND wt.bundleAlias = :bundleAlias AND wt.alias = :alias")
    WidgetTypeEntity findWidgetTypeByTenantIdAndBundleAliasAndAlias(@Param("tenantId") UUID tenantId,
                                                          @Param("bundleAlias") String bundleAlias,
                                                          @Param("alias") String alias);
}
