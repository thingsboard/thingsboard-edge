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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.role.Role;

@Data
public class GroupPermissionInfo extends GroupPermission {

    private static final long serialVersionUID = 2807343092519543363L;

    @Schema(description = "Represent set of permissions.")
    private Role role;

    @Schema(description = "Entity Group Name.")
    private String entityGroupName;
    @Schema(description = "Entity Group Owner Id (Tenant or Customer).")
    private EntityId entityGroupOwnerId;
    @Schema(description = "Name of the entity group owner (Tenant or Customer title).")
    private String entityGroupOwnerName;

    @Schema(description = "User Group Name.")
    private String userGroupName;
    @Schema(description = "User Group Owner Id (Tenant or Customer).")
    private EntityId userGroupOwnerId;
    @Schema(description = "Name of the user group owner (Tenant or Customer title).")
    private String userGroupOwnerName;

    @Schema(description = "Shortcut to check if read operations allowed.")
    private boolean isReadOnly;

    public GroupPermissionInfo() {
        super();
    }

    public GroupPermissionInfo(GroupPermission groupPermission) {
        super(groupPermission);
    }

}
