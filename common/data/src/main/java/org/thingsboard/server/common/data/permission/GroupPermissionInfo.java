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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.role.Role;

@Data
public class GroupPermissionInfo extends GroupPermission {

    private static final long serialVersionUID = 2807343092519543363L;

    @ApiModelProperty(position = 9, value = "Represent set of permissions.")
    private Role role;

    @ApiModelProperty(position = 10, value = "Entity Group Name.")
    private String entityGroupName;
    @ApiModelProperty(position = 11, value = "Entity Group Owner Id (Tenant or Customer).")
    private EntityId entityGroupOwnerId;
    @ApiModelProperty(position = 12, value = "Name of the entity group owner (Tenant or Customer title).")
    private String entityGroupOwnerName;

    @ApiModelProperty(position = 13, value = "User Group Name.")
    private String userGroupName;
    @ApiModelProperty(position = 14, value = "User Group Owner Id (Tenant or Customer).")
    private EntityId userGroupOwnerId;
    @ApiModelProperty(position = 15, value = "Name of the user group owner (Tenant or Customer title).")
    private String userGroupOwnerName;

    @ApiModelProperty(position = 16, value = "Shortcut to check if read operations allowed.")
    private boolean isReadOnly;

    public GroupPermissionInfo() {
        super();
    }

    public GroupPermissionInfo(GroupPermission groupPermission) {
        super(groupPermission);
    }

}
