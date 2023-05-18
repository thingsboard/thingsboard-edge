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
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, ExportableEntityRepository<DeviceEntity> {

    String COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL = "SELECT count(d.*) FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'FIRMWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")";
    String COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL = "SELECT count(d.*) FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'SOFTWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")";

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))" +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%'))) ")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId AND (d.customerId is null OR d.customerId = '13814000-1dd2-11b2-8080-808080808080')")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId AND d.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantDeviceTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%'))) ")
    Page<DeviceEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%'))) ")
    Page<DeviceEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.type = :type " +
            "AND (LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%'))) ")
    Page<DeviceEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("type") String type,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByIdIn(List<UUID> deviceIds);

    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    Long countByDeviceProfileId(UUID deviceProfileId);

    /**
     * Count devices by tenantId.
     * Custom query applied because default QueryDSL produces slow count(id).
     * <p>
     * There is two way to count devices.
     * OPTIMAL: count(*)
     *   - returns _row_count_ and use index-only scan (super fast).
     * SLOW: count(id)
     *   - returns _NON_NULL_id_count and performs table scan to verify isNull for each id in filtered rows.
     * */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.id FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    Page<UUID> findIdsByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType, Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByEntityGroupIdAndDeviceProfileIdAndFirmwareIdIsNull(@Param("groupId") UUID groupId,
                                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                                @Param("textSearch") String textSearch,
                                                                                Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DeviceEntity> findByEntityGroupIdAndDeviceProfileIdAndSoftwareIdIsNull(@Param("groupId") UUID groupId,
                                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                                @Param("textSearch") String textSearch,
                                                                                Pageable pageable);

    @Query(value = "SELECT d.* FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'FIRMWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")",

            countQuery = COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL,
            nativeQuery = true)
    Page<DeviceEntity> findByDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                Pageable pageable);

    @Query(value = "SELECT d.* FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'SOFTWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")",

            countQuery = COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL,
            nativeQuery = true)
    Page<DeviceEntity> findByDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                Pageable pageable);

    @Query("SELECT count(*) FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = (SELECT op.deviceProfileId FROM OtaPackageInfoEntity op WHERE op.id = :otaPackageId) " +
            "AND d.firmwareId = null")
    Long countByEntityGroupIdAndFirmwareIdIsNull(@Param("groupId") UUID groupId,
                                                 @Param("otaPackageId") UUID otaPackageId);

    @Query("SELECT count(*) FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = (SELECT op.deviceProfileId FROM OtaPackageInfoEntity op WHERE op.id = :otaPackageId) " +
            "AND d.softwareId = null")
    Long countByEntityGroupIdAndSoftwareIdIsNull(@Param("groupId") UUID groupId,
                                                 @Param("otaPackageId") UUID otaPackageId);

    @Query(value = COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL,
            nativeQuery = true)
    Long countByDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL,
            nativeQuery = true)
    Long countByDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT externalId FROM DeviceEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
