///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData } from '@shared/models/base-data';
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

export interface Role extends BaseData<RoleId> {
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
