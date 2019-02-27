/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.permission;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.*;

public final class MergedUserPermissions {

    @Getter
    private final Map<Resource, Set<Operation>> genericPermissions;
    @Getter
    private final Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions;
    @Getter
    private final Map<EntityType, MergedGroupTypePermissionInfo> readGroupPermissions;

    public MergedUserPermissions(Map<Resource, Set<Operation>> genericPermissions, Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions) {
        this.genericPermissions = genericPermissions;
        this.groupPermissions = groupPermissions;
        this.readGroupPermissions = new HashMap<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            Resource resource = Resource.groupResourceFromGroupType(groupType);
            boolean hasGenericRead = hasGenericPermission(resource, Operation.READ);
            MergedGroupTypePermissionInfo groupTypePermissionInfo = new MergedGroupTypePermissionInfo(new ArrayList<>(), hasGenericRead);
            this.readGroupPermissions.put(groupType, groupTypePermissionInfo);
        }
        this.groupPermissions.forEach((id, info) -> {
            if (checkOperation(info.getOperations(), Operation.READ)) {
                this.readGroupPermissions.get(info.getEntityType()).getEntityGroupIds().add(id);
            }
        });
    }

    public boolean hasGenericPermission(Resource resource, Operation operation) {
        return hasGenericResourcePermission(resource, operation) || hasGenericAllPermission(operation);
    }

    private boolean hasGenericAllPermission(Operation operation) {
        Set<Operation> operations = genericPermissions.get(Resource.ALL);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean hasGenericResourcePermission(Resource resource, Operation operation) {
        Set<Operation> operations = genericPermissions.get(resource);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean checkOperation(Set<Operation> operations, Operation operation) {
        return operations.contains(Operation.ALL) || operations.contains(operation);
    }

    public boolean hasGroupPermissions(EntityGroupId entityGroupId, Operation operation) {
        MergedGroupPermissionInfo permissionInfo = groupPermissions.get(entityGroupId);
        return permissionInfo != null && checkOperation(permissionInfo.getOperations(), operation);
    }
}
