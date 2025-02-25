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
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.edqs.DataPoint;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToString(callSuper = true)
public class DeviceData extends ProfileAwareData<DeviceFields> {

    private final Map<Integer, DataPoint> clientAttrMap;
    private final Map<Integer, DataPoint> sharedAttrMap;

    public DeviceData(UUID entityId) {
        super(entityId);
        this.clientAttrMap = new ConcurrentHashMap<>();
        this.sharedAttrMap = new ConcurrentHashMap<>();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

    @Override
    public DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType) {
        return switch (entityKeyType) {
            case ATTRIBUTE -> getAttributeDataPoint(keyId);
            case SERVER_ATTRIBUTE -> serverAttrMap.get(keyId);
            case CLIENT_ATTRIBUTE -> clientAttrMap.get(keyId);
            case SHARED_ATTRIBUTE -> sharedAttrMap.get(keyId);
            default -> throw new RuntimeException(entityKeyType + " not implemented");
        };
    }

    @Override
    public boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value) {
        return switch (scope) {
            case SERVER_SCOPE -> serverAttrMap.put(keyId, value) == null;
            case CLIENT_SCOPE -> clientAttrMap.put(keyId, value) == null;
            case SHARED_SCOPE -> sharedAttrMap.put(keyId, value) == null;
        };
    }

    @Override
    public boolean removeAttr(Integer keyId, AttributeScope scope) {
        return switch (scope) {
            case SERVER_SCOPE -> serverAttrMap.remove(keyId) != null;
            case CLIENT_SCOPE -> clientAttrMap.remove(keyId) != null;
            case SHARED_SCOPE -> sharedAttrMap.remove(keyId) != null;
        };
    }

    private DataPoint getAttributeDataPoint(Integer keyId) {
        DataPoint dp = serverAttrMap.get(keyId);
        if (dp == null) {
            dp = sharedAttrMap.get(keyId);
            if (dp == null) {
                dp = clientAttrMap.get(keyId);
            }
        }
        return dp;
    }

}
