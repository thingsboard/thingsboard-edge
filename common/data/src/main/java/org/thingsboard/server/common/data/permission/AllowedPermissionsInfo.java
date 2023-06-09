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
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.Set;

@ApiModel
@Data
@AllArgsConstructor
public class AllowedPermissionsInfo {

    @ApiModelProperty(position = 4, value = "Static map (vocabulary) of allowed operations by resource type")
    private Map<Resource, Set<Operation>> operationsByResource;
    @ApiModelProperty(position = 5, value = "Static set (vocabulary) of allowed operations for group roles")
    private Set<Operation> allowedForGroupRoleOperations;
    @ApiModelProperty(position = 6, value = "Static set (vocabulary) of allowed operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyOperations;
    @ApiModelProperty(position = 7, value = "Static set (vocabulary) of allowed group operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyGroupOperations;
    @ApiModelProperty(position = 3, value = "Static set (vocabulary) of all possibly allowed resources. Static and depends only on the authority of the user")
    private Set<Resource> allowedResources;
    @ApiModelProperty(position = 2, value = "JSON object with merged permission for all generic and group roles assigned to all user groups the user belongs to")
    private MergedUserPermissions userPermissions;
    @ApiModelProperty(position = 1, value = "Owner Id of the user (Tenant or Customer)")
    private EntityId userOwnerId;

}
