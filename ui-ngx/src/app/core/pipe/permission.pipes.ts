///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { isArray } from '@core/utils';
import { EntityGroupInfo } from '@shared/models/entity-group.models';

@Injectable({
  providedIn: 'root'
})
@Pipe({
  name: 'hasGenericPermission'
})
export class HasGenericPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(resource: Resource | Resource[], operation: Operation | Operation[]): boolean {
    return this.hasGenericPermission(resource, operation);
  }

  private hasGenericPermission(resource: Resource | Resource[], operation: Operation | Operation[]): boolean {
    if (isArray(resource)) {
      return this.hasGenericResourcesPermission(resource as Resource[], operation as Operation);
    } else if (isArray(operation)) {
      return this.hasGenericOperationsPermission(resource as Resource, operation as Operation[]);
    } else {
      return this.userPermissionsService.hasGenericPermission(resource as Resource, operation as Operation);
    }
  }

  private hasGenericResourcesPermission(resources: Resource[], operation: Operation): boolean {
    for (const resource of resources) {
      if (!this.hasGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }

  private hasGenericOperationsPermission(resource: Resource, operations: Operation[]): boolean {
    for (const operation of operations) {
      if (!this.hasGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }
}

@Injectable({
  providedIn: 'root'
})
@Pipe({
  name: 'hasEntityGroupPermission'
})
export class HasEntityGroupPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(entityGroup: EntityGroupInfo, operation: Operation): boolean {
    return this.userPermissionsService.hasEntityGroupPermission(operation, entityGroup);
  }
}

@Injectable({
  providedIn: 'root'
})
@Pipe({
  name: 'hasGroupEntityPermission'
})
export class HasGroupEntityPermissionPipe implements PipeTransform {

  constructor(private userPermissionsService: UserPermissionsService) {}

  transform(entityGroup: EntityGroupInfo, operation: Operation): boolean {
    return this.userPermissionsService.hasGroupEntityPermission(operation, entityGroup);
  }
}

