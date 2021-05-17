/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.firmware;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.dao.model.sql.FirmwareInfoEntity;

import java.util.UUID;

public interface FirmwareInfoRepository extends CrudRepository<FirmwareInfoEntity, UUID> {
    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND LOWER(f.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<FirmwareInfoEntity> findAllByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND f.deviceProfileId = :deviceProfileId " +
            "AND f.type = :type " +
            "AND ((f.data IS NOT NULL AND :hasData = true) OR (f.data IS NULL AND :hasData = false ))" +
            "AND LOWER(f.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<FirmwareInfoEntity> findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(@Param("tenantId") UUID tenantId,
                                                                                  @Param("deviceProfileId") UUID deviceProfileId,
                                                                                  @Param("type") FirmwareType type,
                                                                                  @Param("hasData") boolean hasData,
                                                                                  @Param("searchText") String searchText,
                                                                                  Pageable pageable);

    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE f.id = :id")
    FirmwareInfoEntity findFirmwareInfoById(@Param("id") UUID id);

    @Query(value = "SELECT exists(SELECT * " +
            "FROM device_profile AS dp " +
            "LEFT JOIN device AS d ON dp.id = d.device_profile_id " +
            "WHERE dp.id = :deviceProfileId AND " +
            "(('FIRMWARE' = :type AND (dp.firmware_id = :firmwareId OR d.firmware_id = :firmwareId)) " +
            "OR ('SOFTWARE' = :type AND (dp.software_id = :firmwareId or d.software_id = :firmwareId))))", nativeQuery = true)
    boolean isFirmwareUsed(@Param("firmwareId") UUID firmwareId, @Param("deviceProfileId") UUID deviceProfileId, @Param("type") String type);

    @Query(value =
            "SELECT * FROM firmware " +
                    "WHERE id = " +
                    "(SELECT COALESCE(d.firmware_id, g.firmware_id, dp.firmware_id) " +
                    "FROM (SELECT d.firmware_id FROM device d WHERE d.id = :deviceId LIMIT 1) d " +
                    "FULL JOIN " +
                    "(SELECT dgf.firmware_id " +
                    "FROM device_group_firmware dgf " +
                    "INNER JOIN firmware f ON dgf.firmware_id = f.id AND dgf.firmware_type = 'FIRMWARE' AND f.device_profile_id = " +
                    "(SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId LIMIT 1) " +
                    "INNER JOIN relation r " +
                    "ON dgf.group_id = r.from_id AND r.to_type = 'DEVICE' AND " +
                    "r.relation_type_group = 'FROM_ENTITY_GROUP' AND r.to_id = :deviceId " +
                    "ORDER BY dgf.firmware_update_time DESC LIMIT 1) g ON true " +
                    "FULL JOIN " +
                    "(SELECT dp.firmware_id FROM device_profile dp " +
                    "WHERE id = (SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId)) dp ON true)",
            nativeQuery = true)
    FirmwareInfoEntity findFirmwareByDeviceId(@Param("deviceId") UUID deviceId);

    @Query(value =
            "SELECT * FROM firmware " +
                    "WHERE id = " +
                    "(SELECT COALESCE(d.software_id, g.software_id, dp.software_id) " +
                    "FROM (SELECT d.software_id FROM device d WHERE d.id = :deviceId LIMIT 1) d " +
                    "FULL JOIN " +
                    "(SELECT dgf.firmware_id software_id " +
                    "FROM device_group_firmware dgf " +
                    "INNER JOIN firmware f ON dgf.firmware_id = f.id AND dgf.firmware_type = 'SOFTWARE' AND f.device_profile_id = " +
                    "(SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId LIMIT 1) " +
                    "INNER JOIN relation r " +
                    "ON dgf.group_id = r.from_id AND r.to_type = 'DEVICE' AND " +
                    "r.relation_type_group = 'FROM_ENTITY_GROUP' AND r.to_id = :deviceId " +
                    "ORDER BY dgf.firmware_update_time DESC LIMIT 1) g ON true " +
                    "FULL JOIN " +
                    "(SELECT dp.software_id FROM device_profile dp " +
                    "WHERE id = (SELECT d.device_profile_id FROM device d WHERE d.id = :deviceId)) dp ON true)",
            nativeQuery = true)
    FirmwareInfoEntity findSoftwareByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f " +
            "WHERE f.deviceProfileId IN (SELECT d.deviceProfileId FROM DeviceEntity d " +
            "WHERE d.id IN (SELECT r.toId FROM RelationEntity r " +
            "WHERE r.fromId = :groupId AND r.fromType = 'ENTITY_GROUP' AND r.relationTypeGroup = 'FROM_ENTITY_GROUP')) " +
            "AND f.type = :type " +
            "AND f.data IS NOT NULL " +
            "AND LOWER(f.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<FirmwareInfoEntity> findAllByTenantIdAndDeviceGroupAndTypeAndHasData(@Param("groupId") UUID groupId,
                                                                              @Param("type") FirmwareType type,
                                                                              @Param("searchText") String searchText,
                                                                              Pageable pageable);
}
