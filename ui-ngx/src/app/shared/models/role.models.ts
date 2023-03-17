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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { RoleId } from '@shared/models/id/role-id';
import { Operation, Resource, RoleType } from '@shared/models/security.models';
import { ValidatorFn } from '@angular/forms';

export type SpecificRolePermissions = Operation[];
export type GenericRolePermissions = {
  [resource: string]: Operation[];
};

export type RolePermissions = SpecificRolePermissions & GenericRolePermissions;

export interface Role extends BaseData<RoleId>, ExportableEntity<RoleId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: RoleType;
  permissions: RolePermissions;
  additionalInfo?: any;
}

export function genericRolePermissionsValidator(required: boolean): ValidatorFn {
  return control => {
    const permissions: GenericRolePermissions = control.value;
    let requiredError = false;
    let invalidError = false;
    const resources = permissions ? Object.keys(permissions) : [];
    if (required && !resources.length) {
      requiredError = true;
    }
    if (!requiredError) {
      for (const resource of resources) {
        if (!Resource[resource]) {
          invalidError = true;
          break;
        }
        const operations = permissions[resource];
        if (!operations || !operations.length) {
          invalidError = true;
          break;
        }
      }
    }
    let errors = null;
    if (requiredError || invalidError) {
      errors = {};
      if (requiredError) {
        errors.required = true;
      }
      if (invalidError) {
        errors.invalid = true;
      }
    }
    return errors;
  }
}
