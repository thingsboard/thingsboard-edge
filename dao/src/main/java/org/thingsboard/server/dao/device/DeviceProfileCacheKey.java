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
package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class DeviceProfileCacheKey implements Serializable {

    private static final long serialVersionUID = 8220455917177676472L;

    private final TenantId tenantId;
    private final String name;
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;
    private final String provisionDeviceKey;

    private DeviceProfileCacheKey(TenantId tenantId, String name, DeviceProfileId deviceProfileId, boolean defaultProfile, String provisionDeviceKey) {
        this.tenantId = tenantId;
        this.name = name;
        this.deviceProfileId = deviceProfileId;
        this.defaultProfile = defaultProfile;
        this.provisionDeviceKey = provisionDeviceKey;
    }

    public static DeviceProfileCacheKey fromName(TenantId tenantId, String name) {
        return new DeviceProfileCacheKey(tenantId, name, null, false, null);
    }

    public static DeviceProfileCacheKey fromId(DeviceProfileId id) {
        return new DeviceProfileCacheKey(null, null, id, false, null);
    }

    public static DeviceProfileCacheKey defaultProfile(TenantId tenantId) {
        return new DeviceProfileCacheKey(tenantId, null, null, true, null);
    }

    public static DeviceProfileCacheKey fromProvisionDeviceKey(String provisionDeviceKey) {
        return new DeviceProfileCacheKey(null, null, null, false, provisionDeviceKey);
    }

    /**
     * IMPORTANT: Method toString() has to return unique value, if you add additional field to this class, please also refactor toString().
     */
    @Override
    public String toString() {
        if (deviceProfileId != null) {
            return deviceProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else if (StringUtils.isNotEmpty(provisionDeviceKey)) {
            return provisionDeviceKey;
        }
        return tenantId + "_" + name;
    }
}
