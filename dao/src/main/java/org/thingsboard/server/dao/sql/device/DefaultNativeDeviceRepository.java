/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.UUID;

@Repository
@Slf4j
public class DefaultNativeDeviceRepository extends AbstractNativeRepository implements NativeDeviceRepository {

    private final String COUNT_QUERY = "SELECT count(id) FROM device;";

    public DefaultNativeDeviceRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        super(jdbcTemplate, transactionTemplate);
    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable) {
        String DEVICE_ID_INFO_QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
        return find(COUNT_QUERY, DEVICE_ID_INFO_QUERY, pageable, row -> {
            UUID id = (UUID) row.get("id");
            var tenantIdObj = row.get("tenantId");
            var customerIdObj = row.get("customerId");
            return new DeviceIdInfo(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), customerIdObj != null ? (UUID) customerIdObj : null, id);
        });
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(Pageable pageable) {
        String PROFILE_DEVICE_ID_INFO_QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, device_profile_id as profileId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
        return find(COUNT_QUERY, PROFILE_DEVICE_ID_INFO_QUERY, pageable, row -> {
            var tenantIdObj = row.get("tenantId");
            UUID tenantId = tenantIdObj != null ? ((UUID) tenantIdObj) : TenantId.SYS_TENANT_ID.getId();
            DeviceId id = new DeviceId((UUID) row.get("id"));
            CustomerId customerId = new CustomerId((UUID) row.get("customerId"));
            EntityId ownerId = !customerId.isNullUid() ? customerId : TenantId.fromUUID(tenantId);
            DeviceProfileId profileId = new DeviceProfileId((UUID) row.get("profileId"));
            return ProfileEntityIdInfo.create(tenantId, ownerId, profileId, id);
        });
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, Pageable pageable) {
        String PROFILE_DEVICE_ID_INFO_QUERY = String.format("SELECT tenant_id as tenantId, customer_id as customerId, device_profile_id as profileId, id as id FROM device WHERE tenant_id = '%s' ORDER BY created_time ASC LIMIT %%s OFFSET %%s", tenantId);
        return find(COUNT_QUERY, PROFILE_DEVICE_ID_INFO_QUERY, pageable, row -> {
            var tenantIdObj = row.get("tenantId");
            UUID tenantIdUuid = tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId();
            DeviceId id = new DeviceId((UUID) row.get("id"));
            CustomerId customerId = new CustomerId((UUID) row.get("customerId"));
            EntityId ownerId = !customerId.isNullUid() ? customerId : TenantId.fromUUID(tenantIdUuid);
            DeviceProfileId profileId = new DeviceProfileId((UUID) row.get("profileId"));
            return ProfileEntityIdInfo.create(tenantIdUuid, ownerId, profileId, id);
        });
    }

}
