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
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface DeviceInfoRepository extends JpaRepository<DeviceInfoEntity, UUID> {

    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND (" +
            "((:customerId IS NULL AND (:includeCustomers) IS TRUE)) " +
            "OR ((:customerId IS NULL AND (:includeCustomers) IS FALSE) AND (d.customerId IS NULL OR d.customerId = '13814000-1dd2-11b2-8080-808080808080')) " +
            "OR (:customerId IS NOT NULL AND d.customerId = uuid(:customerId)) " +
            ") " +
            "AND (:deviceProfileId IS NULL OR d.deviceProfileId = uuid(:deviceProfileId)) " +
            "AND ((:filterByActive) IS FALSE OR d.active = :deviceActive) " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.type) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.ownerName) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByFilter(@Param("tenantId") UUID tenantId,
                                                   @Param("includeCustomers") boolean includeCustomers,
                                                   @Param("customerId") String customerId,
                                                   @Param("deviceProfileId") String deviceProfileId,
                                                   @Param("filterByActive") boolean filterByActive,
                                                   @Param("deviceActive") boolean active,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select d.id, d.created_time, d.additional_info, d.customer_id, d.device_profile_id, " +
            "d.device_data, d.type, d.name, d.label, d.tenant_id, d.firmware_id, d.software_id, d.external_id, d.groups, " +
            "c.title as owner_name, d.active as active from device_info_view d " +
            "LEFT JOIN customer c on c.id = d.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:deviceProfileId IS NULL OR e.device_profile_id = uuid(:deviceProfileId)) " +
            "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(e.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(e.type) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(e.owner_name) LIKE LOWER(CONCAT('%', :textSearch, '%')))",
            countQuery = "SELECT count(e.id) FROM device_info_view e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:deviceProfileId IS NULL OR e.device_profile_id = uuid(:deviceProfileId)) " +
                    "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
                    "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
                    "OR LOWER(e.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
                    "OR LOWER(e.type) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
                    "OR LOWER(e.owner_name) LIKE LOWER(CONCAT('%', :textSearch, '%')))",
            nativeQuery = true)
    Page<DeviceInfoEntity> findDeviceInfosByFilterIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("deviceProfileId") String deviceProfileId,
                                                        @Param("filterByActive") boolean filterByActive,
                                                        @Param("deviceActive") boolean active,
                                                        @Param("textSearch") String textSearch, Pageable pageable);
}
