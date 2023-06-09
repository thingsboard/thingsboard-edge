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
package org.thingsboard.server.common.data.permission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupPermission extends BaseData<GroupPermissionId> implements HasName, TenantEntity {

    private static final long serialVersionUID = 5582010124562018986L;

    public static final Map<Resource, List<Operation>> ALL_PERMISSIONS = new HashMap<>();
    static {
        ALL_PERMISSIONS.put(Resource.ALL, Collections.singletonList(Operation.ALL));
    }

    public static final List<Operation> READ_ONLY_GROUP_PERMISSIONS = Arrays.asList(
            Operation.READ,
            Operation.RPC_CALL,
            Operation.READ_CREDENTIALS,
            Operation.READ_ATTRIBUTES,
            Operation.READ_TELEMETRY
    );

    public static final List<Operation> WRITE_GROUP_PERMISSIONS = Collections.singletonList(Operation.ALL);

    public static final Map<Resource, List<Operation>> READ_ONLY_USER_PERMISSIONS = new HashMap<>();
    static {
        READ_ONLY_USER_PERMISSIONS.put(Resource.ALL, READ_ONLY_GROUP_PERMISSIONS);
        READ_ONLY_USER_PERMISSIONS.put(Resource.PROFILE, Arrays.asList(Operation.ALL));
    }

    public static final Map<Resource, List<Operation>> PUBLIC_USER_PERMISSIONS = new HashMap<>();
    static {
        PUBLIC_USER_PERMISSIONS.put(Resource.DASHBOARD, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.WIDGETS_BUNDLE, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.WIDGET_TYPE, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.ALARM, Arrays.asList(
                Operation.READ
        ));
    }

    public static final List<Operation> PUBLIC_USER_ENTITY_GROUP_PERMISSIONS =
            Arrays.asList(Operation.READ, Operation.RPC_CALL, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY);

    private TenantId tenantId;
    @ApiModelProperty(position = 4, value = "JSON object with the User Group Id. Represent user group that will have permissions to perform operations against corresponding Entity Group.")
    private EntityGroupId userGroupId;
    @ApiModelProperty(position = 5, value = "JSON object with the Role Id. Represent set of permissions.")
    private RoleId roleId;
    @ApiModelProperty(position = 6, value = "JSON object with the Entity Group Id. Represent entity (device, asset, etc.) group.")
    private EntityGroupId entityGroupId;
    @ApiModelProperty(position = 7, value = "Type of the entities in the group: DEVICE, ASSET, CUSTOMER, etc.")
    private EntityType entityGroupType;
    @ApiModelProperty(position = 8, value = "Public or Private permissions. Private by default.", example = "false")
    private boolean isPublic;

    public GroupPermission() {
        super();
    }

    public GroupPermission(GroupPermissionId id) {
        super(id);
    }

    public GroupPermission(GroupPermission groupPermission) {
        super(groupPermission);
        this.tenantId = groupPermission.getTenantId();
        this.userGroupId = groupPermission.getUserGroupId();
        this.roleId = groupPermission.getRoleId();
        this.entityGroupId = groupPermission.getEntityGroupId();
        this.entityGroupType = groupPermission.getEntityGroupType();
        this.isPublic = groupPermission.isPublic();
    }

    @ApiModelProperty(position = 3, value = "JSON object with the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @ApiModelProperty(position = 8, value = "Name of the Group Permissions. Auto-generated", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getName() {
        if (entityGroupId != null && entityGroupType != null) {
            return String.format("GROUP_[%s]_[%s]_[%s]_[%s]", userGroupId != null ?  userGroupId.toString() : "", roleId.toString(), entityGroupId.toString(), entityGroupType.name());
        } else {
            return String.format("GENERIC_[%s]_[%s]", userGroupId != null ?  userGroupId.toString() : "", roleId.toString());
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Group Permission Id. " +
            "Specify this field to update the Group Permission. " +
            "Referencing non-existing Group Permission Id will cause error. " +
            "Omit this field to create new Group Permission." )
    @Override
    public GroupPermissionId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the group permission creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
