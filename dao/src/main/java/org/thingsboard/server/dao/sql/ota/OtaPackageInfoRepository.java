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
package org.thingsboard.server.dao.sql.ota;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.sql.OtaPackageInfoEntity;

import java.util.UUID;

public interface OtaPackageInfoRepository extends JpaRepository<OtaPackageInfoEntity, UUID> {
    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, CASE WHEN (f.data IS NOT NULL OR f.url IS NOT NULL)  THEN true ELSE false END) FROM OtaPackageEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND LOWER(f.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<OtaPackageInfoEntity> findAllByTenantId(@Param("tenantId") UUID tenantId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, true) FROM OtaPackageEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND f.deviceProfileId = :deviceProfileId " +
            "AND f.type = :type " +
            "AND (f.data IS NOT NULL OR f.url IS NOT NULL) " +
            "AND LOWER(f.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<OtaPackageInfoEntity> findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(@Param("tenantId") UUID tenantId,
                                                                                    @Param("deviceProfileId") UUID deviceProfileId,
                                                                                    @Param("type") OtaPackageType type,
                                                                                    @Param("searchText") String searchText,
                                                                                    Pageable pageable);

    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, CASE WHEN (f.data IS NOT NULL OR f.url IS NOT NULL)  THEN true ELSE false END) FROM OtaPackageEntity f WHERE f.id = :id")
    OtaPackageInfoEntity findOtaPackageInfoById(@Param("id") UUID id);

    @Query(value = "SELECT exists(SELECT * " +
            "FROM device_profile AS dp " +
            "LEFT JOIN device AS d ON dp.id = d.device_profile_id " +
            "WHERE dp.id = :deviceProfileId AND " +
            "(('FIRMWARE' = :type AND (dp.firmware_id = :otaPackageId OR d.firmware_id = :otaPackageId)) " +
            "OR ('SOFTWARE' = :type AND (dp.software_id = :otaPackageId or d.software_id = :otaPackageId))))", nativeQuery = true)
    boolean isOtaPackageUsed(@Param("otaPackageId") UUID otaPackageId, @Param("deviceProfileId") UUID deviceProfileId, @Param("type") String type);

    @Query(value =
            "SELECT * FROM ota_package " +
                    "WHERE id = " +
                    "(SELECT COALESCE(d.firmware_id, " +
                    "(SELECT dgop.ota_package_id FROM device_group_ota_package dgop " +
                    "INNER JOIN ota_package ota ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'FIRMWARE' AND ota.device_profile_id = (SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId LIMIT 1) " +
                    "INNER JOIN relation r ON dgop.group_id = r.from_id AND r.to_type = 'DEVICE' AND r.relation_type_group = 'FROM_ENTITY_GROUP' AND r.to_id = :deviceId ORDER BY dgop.ota_package_update_time DESC LIMIT 1), " +
                    "(SELECT dp.firmware_id FROM device_profile dp where dp.id = d.device_profile_id)) " +
                    "FROM device d WHERE d.id = :deviceId)",
            nativeQuery = true)
    OtaPackageInfoEntity findFirmwareByDeviceId(@Param("deviceId") UUID deviceId);

    @Query(value =
            "SELECT * FROM ota_package " +
                    "WHERE id = " +
                    "(SELECT COALESCE(d.software_id, " +
                    "(SELECT dgop.ota_package_id FROM device_group_ota_package dgop " +
                    "INNER JOIN ota_package ota ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'SOFTWARE' AND ota.device_profile_id = (SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId LIMIT 1) " +
                    "INNER JOIN relation r ON dgop.group_id = r.from_id AND r.to_type = 'DEVICE' AND r.relation_type_group = 'FROM_ENTITY_GROUP' AND r.to_id = :deviceId ORDER BY dgop.ota_package_update_time DESC LIMIT 1), " +
                    "(SELECT dp.software_id FROM device_profile dp where dp.id = d.device_profile_id)) " +
                    "FROM device d WHERE d.id = :deviceId)",
            nativeQuery = true)
    OtaPackageInfoEntity findSoftwareByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("SELECT new OtaPackageInfoEntity(ota.id, ota.createdTime, ota.tenantId, ota.deviceProfileId, ota.type, ota.title, ota.version, ota.tag, ota.url, ota.fileName, ota.contentType, ota.checksumAlgorithm, ota.checksum, ota.dataSize, ota.additionalInfo, true) FROM OtaPackageEntity ota " +
            "WHERE ota.deviceProfileId IN (SELECT d.deviceProfileId FROM DeviceEntity d " +
            "WHERE d.id IN (SELECT r.toId FROM RelationEntity r " +
            "WHERE r.fromId = :groupId AND r.fromType = 'ENTITY_GROUP' AND r.relationTypeGroup = 'FROM_ENTITY_GROUP')) " +
            "AND ota.type = :type " +
            "AND (ota.data IS NOT NULL OR ota.url IS NOT NULL) " +
            "AND LOWER(ota.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<OtaPackageInfoEntity> findAllByTenantIdAndDeviceGroupAndTypeAndHasData(@Param("groupId") UUID groupId,
                                                                                @Param("type") OtaPackageType type,
                                                                                @Param("searchText") String searchText,
                                                                                Pageable pageable);
}
