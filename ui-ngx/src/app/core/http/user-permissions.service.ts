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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  AllowedPermissionsInfo,
  groupResourceByGroupType,
  MergedUserPermissions,
  Operation,
  Resource,
  resourceByEntityType
} from '@shared/models/security.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup, EntityGroupInfo } from '@shared/models/entity-group.models';
import { isArray } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class UserPermissionsService {

  private operationsByResource: {[resource: string]: Operation[]};
  private allowedGroupRoleOperations: Operation[];
  private allowedGroupOwnerOnlyOperations: Operation[];
  private allowedGroupOwnerOnlyGroupOperations: Operation[];
  private allowedResources: Resource[];
  private userPermissions: MergedUserPermissions;
  private userOwnerId: EntityId;

  constructor(
    private http: HttpClient
  ) {
  }

  public loadPermissionsInfo(): Observable<AllowedPermissionsInfo> {
    return this.http.get<AllowedPermissionsInfo>('/api/permissions/allowedPermissions').pipe(
      tap((allowedPermissionsInfo) => {
        this.operationsByResource = allowedPermissionsInfo.operationsByResource;
        this.allowedGroupRoleOperations = allowedPermissionsInfo.allowedForGroupRoleOperations;
        this.allowedGroupOwnerOnlyOperations = allowedPermissionsInfo.allowedForGroupOwnerOnlyOperations;
        this.allowedGroupOwnerOnlyGroupOperations = allowedPermissionsInfo.allowedForGroupOwnerOnlyGroupOperations;
        this.allowedResources = allowedPermissionsInfo.allowedResources;
        this.userPermissions = allowedPermissionsInfo.userPermissions;
        this.userOwnerId = allowedPermissionsInfo.userOwnerId;
      })
    );
  }

  public getOperationsByResource(resource: Resource): Operation[] {
    if (resource && this.operationsByResource && this.operationsByResource[resource]) {
      return this.operationsByResource[resource];
    } else {
      return [];
    }
  }

  public getAllowedGroupRoleOperations(): Operation[] {
    return this.allowedGroupRoleOperations ? this.allowedGroupRoleOperations : [];
  }

  public getAllowedResources(): Resource[] {
    return this.allowedResources ? this.allowedResources : [];
  }

  public hasReadGroupsPermission(entityType: EntityType): boolean {
    if (this.userPermissions) {
      const readGroupPermissions = this.userPermissions.readGroupPermissions;
      const groupTypePermissionInfo = readGroupPermissions[entityType];
      return groupTypePermissionInfo.hasGenericRead || groupTypePermissionInfo.entityGroupIds.length > 0;
    } else {
      return false;
    }
  }

  public hasReadGenericPermission(resource: Resource): boolean {
    if (this.userPermissions) {
      return this.hasGenericPermission(resource, Operation.READ);
    } else {
      return false;
    }
  }

  public hasGenericPermission(resource: Resource, operation: Operation): boolean {
    if (this.userPermissions) {
      return this.hasGenericResourcePermission(resource, operation) || this.hasGenericAllPermission(operation);
    } else {
      return false;
    }
  }

  public hasGenericPermissionByEntityGroupType(operation: Operation, groupType: EntityType): boolean {
    if (!groupType) {
      return false;
    }
    const resource = resourceByEntityType.get(groupType);
    return this.hasGenericPermission(resource, operation);
  }

  public hasGenericEntityGroupTypePermission(operation: Operation, groupType: EntityType): boolean {
    if (!groupType) {
      return false;
    }
    const resource = groupResourceByGroupType.get(groupType);
    return this.hasGenericPermission(resource, operation);
  }

  public hasGenericEntityGroupPermission(operation: Operation, entityGroup: EntityGroup): boolean {
    if (!entityGroup) {
      return false;
    }
    return this.hasGenericEntityGroupTypePermission(operation, entityGroup.type);
  }

  public hasEntityGroupPermission(operation: Operation, entityGroup: EntityGroupInfo): boolean {
    return this.checkEntityGroupPermission(operation, entityGroup, true);
  }

  public hasGroupEntityPermission(operation: Operation, entityGroup: EntityGroupInfo): boolean {
    return this.checkEntityGroupPermission(operation, entityGroup, false);
  }

  public isDirectlyOwnedGroup(entityGroup: EntityGroupInfo) {
    if (this.userOwnerId && entityGroup && entityGroup.ownerId) {
      return this.idsEqual(this.userOwnerId, entityGroup.ownerId);
    } else {
      return false;
    }
  }

  public isOwnedGroup(entityGroup: EntityGroupInfo): boolean {
    if (!entityGroup) {
      return false;
    }
    return this.isCurrentUserOwner(entityGroup);
  }

  public getUserOwnerId(): EntityId {
    return this.userOwnerId;
  }

  public isDirectOwner(ownerId: EntityId): boolean {
    if (this.userOwnerId && ownerId) {
      return this.idsEqual(this.userOwnerId, ownerId);
    } else {
      return false;
    }
  }

  public hasResourcesGenericPermission(resource: Resource | Resource[], operation: Operation | Operation[]): boolean {
    if (isArray(resource)) {
      return this.hasGenericResourcesPermission(resource as Resource[], operation);
    } else if (isArray(operation)) {
      return this.hasGenericOperationsPermission(resource, operation as Operation[]);
    } else {
      return this.hasGenericPermission(resource as Resource, operation as Operation);
    }
  }

  private hasGenericResourcesPermission(resources: Resource[], operation: Operation | Operation[]): boolean {
    for (const resource of resources) {
      if (!this.hasResourcesGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }

  private hasGenericOperationsPermission(resource: Resource | Resource[], operations: Operation[]): boolean {
    for (const operation of operations) {
      if (!this.hasResourcesGenericPermission(resource, operation)) {
        return false;
      }
    }
    return true;
  }

  private hasGenericAllPermission(operation: Operation): boolean {
    const operations = this.userPermissions.genericPermissions[Resource.ALL];
    if (operations) {
      return this.checkOperation(operations, operation);
    } else {
      return false;
    }
  }

  private hasGenericResourcePermission(resource: Resource, operation: Operation): boolean {
    const operations = this.userPermissions.genericPermissions[resource];
    if (operations) {
      return this.checkOperation(operations, operation);
    } else {
      return false;
    }
  }

  private checkOperation(operations: Operation[], operation: Operation): boolean {
    return operations.indexOf(Operation.ALL) > -1 || operations.indexOf(operation) > -1;
  }

  private checkEntityGroupPermission(operation: Operation, entityGroup: EntityGroupInfo, isGroup: boolean): boolean {
    if (!entityGroup) {
      return false;
    }
    let resource: Resource;
    if (isGroup) {
      resource = groupResourceByGroupType.get(entityGroup.type);
    } else {
      resource = resourceByEntityType.get(entityGroup.type);
    }
    if (this.isCurrentUserOwner(entityGroup)) {
      if (this.hasGenericPermission(resource, operation)) {
        return true;
      }
    }
    return this.hasGroupPermissions(entityGroup, operation, isGroup);
  }

  private hasGroupPermissions(entityGroup: EntityGroupInfo, operation: Operation, isGroup: boolean): boolean {
    if (!this.allowedGroupRoleOperations || this.allowedGroupRoleOperations.indexOf(operation) === -1) {
      return false;
    }
    if (isGroup) {
      if (this.allowedGroupOwnerOnlyGroupOperations && this.allowedGroupOwnerOnlyGroupOperations.indexOf(operation) > -1) {
        return false;
      }
    } else {
      if (this.allowedGroupOwnerOnlyOperations && this.allowedGroupOwnerOnlyOperations.indexOf(operation) > -1) {
        if (!this.isCurrentUserOwner(entityGroup)) {
          return false;
        }
      }
    }
    if (this.userPermissions && this.userPermissions.groupPermissions) {
      const permissionInfo = this.userPermissions.groupPermissions[entityGroup.id.id];
      return permissionInfo && this.checkOperation(permissionInfo.operations, operation);
    }
    return false;
  }

  private isCurrentUserOwner(entityGroup: EntityGroupInfo): boolean {
    const groupOwnerIds = entityGroup.ownerIds;
    if (this.userOwnerId && groupOwnerIds) {
      return this.containsId(groupOwnerIds, this.userOwnerId);
    } else {
      return false;
    }
  }

  private containsId(idsArray: EntityId[], id: EntityId): boolean {
    for (const arrayId of idsArray) {
      if (this.idsEqual(arrayId, id)) {
        return true;
      }
    }
    return false;
  }

  private idsEqual(id1: EntityId, id2: EntityId): boolean {
    return id1.id === id2.id && id1.entityType === id2.entityType;
  }

}
