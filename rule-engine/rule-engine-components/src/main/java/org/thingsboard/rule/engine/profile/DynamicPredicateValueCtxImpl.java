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
package org.thingsboard.rule.engine.profile;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DynamicPredicateValueCtxImpl implements DynamicPredicateValueCtx {
    private final TenantId tenantId;
    private CustomerId customerId;
    private final DeviceId deviceId;
    private final TbContext ctx;

    public DynamicPredicateValueCtxImpl(TenantId tenantId, DeviceId deviceId, TbContext ctx) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.ctx = ctx;
        resetCustomer();
    }

    @Override
    public EntityKeyValue getTenantValue(String key) {
        return getValue(tenantId, key);
    }

    @Override
    public EntityKeyValue getCustomerValue(String key) {
        return customerId == null || customerId.isNullUid() ? null : getValue(customerId, key);
    }

    @Override
    public void resetCustomer() {
        Device device = ctx.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.customerId = device.getCustomerId();
        }
    }

    private EntityKeyValue getValue(EntityId entityId, String key) {
        try {
            Optional<AttributeKvEntry> entry = ctx.getAttributesService().find(tenantId, entityId, DataConstants.SERVER_SCOPE, key).get();
            if (entry.isPresent()) {
                return DeviceState.toEntityValue(entry.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to get attribute by key: {} for {}: [{}]", key, entityId.getEntityType(), entityId.getId());
        }
        return null;
    }
}
