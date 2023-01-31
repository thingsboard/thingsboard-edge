///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { BaseData } from '@shared/models/base-data';
import { RoleId } from '@shared/models/id/role-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { GroupPermissionId } from '@shared/models/id/group-permission-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityType } from '@shared/models/entity-type.models';
import { Role } from '@shared/models/role.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface GroupPermission extends BaseData<GroupPermissionId> {
  tenantId?: TenantId;
  userGroupId: EntityGroupId;
  roleId: RoleId;
  entityGroupId?: EntityGroupId;
  entityGroupType?: EntityType;
  isPublic: boolean;
}

export interface GroupPermissionInfo extends GroupPermission {
  role: Role;
  entityGroupName: string;
  entityGroupOwnerId: EntityId;
  entityGroupOwnerName: string;
  userGroupName: string;
  userGroupOwnerId: EntityId;
  userGroupOwnerName: string;
  readOnly: boolean;
}

export interface GroupPermissionFullInfo extends GroupPermissionInfo {
  roleName?: string;
  roleTypeName?: string;
  entityGroupTypeName?: string;
  entityGroupOwnerFullName?: string;
  userGroupOwnerFullName?: string;
  sourceGroupPermission?: GroupPermission;
}

export function isGroupPermissionsEqual(gp1: GroupPermission, gp2: GroupPermission): boolean {
  return gp1?.roleId?.id === gp2?.roleId?.id && gp1.entityGroupId?.id === gp2.entityGroupId?.id;
}
