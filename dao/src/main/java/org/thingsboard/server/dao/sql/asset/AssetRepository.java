/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@SqlDao
public interface AssetRepository extends CrudRepository<AssetEntity, String> {

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id")
    List<AssetEntity> findByTenantId(@Param("tenantId") String tenantId,
                                     @Param("textSearch") String textSearch,
                                     @Param("idOffset") String idOffset,
                                     Pageable pageable);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id")
    List<AssetEntity> findByTenantIdAndCustomerId(@Param("tenantId") String tenantId,
                                                  @Param("customerId") String customerId,
                                                  @Param("textSearch") String textSearch,
                                                  @Param("idOffset") String idOffset,
                                                  Pageable pageable);

    List<AssetEntity> findByTenantIdAndIdIn(String tenantId, List<String> assetIds);

    List<AssetEntity> findByTenantIdAndCustomerIdAndIdIn(String tenantId, String customerId, List<String> assetIds);

    AssetEntity findByTenantIdAndName(String tenantId, String name);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.type = :type " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id")
    List<AssetEntity> findByTenantIdAndType(@Param("tenantId") String tenantId,
                                            @Param("type") String type,
                                            @Param("textSearch") String textSearch,
                                            @Param("idOffset") String idOffset,
                                            Pageable pageable);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId AND a.type = :type " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id")
    List<AssetEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") String tenantId,
                                                         @Param("customerId") String customerId,
                                                         @Param("type") String type,
                                                         @Param("textSearch") String textSearch,
                                                         @Param("idOffset") String idOffset,
                                                         Pageable pageable);

    @Query("SELECT DISTINCT a.type FROM AssetEntity a WHERE a.tenantId = :tenantId")
    List<String> findTenantAssetTypes(@Param("tenantId") String tenantId);

}
