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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;

import java.util.List;

@ApiModel
@Data
public class ShareGroupRequest {

    @ApiModelProperty(position = 2, value = "In case 'allUserGroup' is set to true, " +
            "this property specifies the owner of the user group 'All'. Either Tenant or Customer Id.")
    private final EntityId ownerId;

    @ApiModelProperty(position = 1, required = true, value = "Indicate that the group should be shared with user group 'All' " +
            "that belongs to Tenant or Customer (see 'ownerId' property description).", name = "")
    private final boolean allUserGroup;

    @ApiModelProperty(position = 3, value = "In case 'allUserGroup' is set to false, " +
            "this property specifies the specific user group that the entity group should be shared with.")
    private final EntityGroupId userGroupId;

    @ApiModelProperty(position = 4, value = "Used if 'roleIds' property is not present. " +
            "if the value is 'true', creates role with read-only permissions. If the value is 'false', creates role with write permissions.")
    private final boolean readElseWrite;

    @ApiModelProperty(position = 4, value = "List of group role Ids that should be used to share the entity group with the user group. " +
            "If not set, the platform will create new role (see 'readElseWrite' property description)")
    private final List<RoleId> roleIds;

}
