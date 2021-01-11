///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';

export enum RoleType {
  GENERIC = 'GENERIC',
  GROUP = 'GROUP'
}

export const roleTypeTranslationMap = new Map<RoleType, string>(
  [
    [RoleType.GENERIC, 'role.display-type.GENERIC'],
    [RoleType.GROUP, 'role.display-type.GROUP'],
  ]
);

export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  RPC_CALL = 'RPC_CALL',
  READ_CREDENTIALS = 'READ_CREDENTIALS',
  WRITE_CREDENTIALS = 'WRITE_CREDENTIALS',
  READ_ATTRIBUTES = 'READ_ATTRIBUTES',
  WRITE_ATTRIBUTES = 'WRITE_ATTRIBUTES',
  READ_TELEMETRY = 'READ_TELEMETRY',
  WRITE_TELEMETRY = 'WRITE_TELEMETRY',
  ADD_TO_GROUP = 'ADD_TO_GROUP',
  REMOVE_FROM_GROUP = 'REMOVE_FROM_GROUP',
  CHANGE_OWNER = 'CHANGE_OWNER',
  IMPERSONATE = 'IMPERSONATE',
  CLAIM_DEVICES = 'CLAIM_DEVICES',
  SHARE_GROUP = 'SHARE_GROUP',
  ASSIGN_TO_TENANT = 'ASSIGN_TO_TENANT'
}

const operationTypeTranslations = new Map<Operation, string>();
for (const key of Object.keys(Operation)) {
  operationTypeTranslations.set(Operation[key], `permission.operation.display-type.${key}`);
}
export const operationTypeTranslationMap = operationTypeTranslations;

export enum Resource {
  ALL = 'ALL',
  PROFILE = 'PROFILE',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  ALARM = 'ALARM',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET = 'ASSET',
  CUSTOMER = 'CUSTOMER',
  DASHBOARD = 'DASHBOARD',
  ENTITY_VIEW = 'ENTITY_VIEW',
  TENANT = 'TENANT',
  TENANT_PROFILE = 'TENANT_PROFILE',
  RULE_CHAIN = 'RULE_CHAIN',
  USER = 'USER',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  CONVERTER = 'CONVERTER',
  INTEGRATION = 'INTEGRATION',
  SCHEDULER_EVENT = 'SCHEDULER_EVENT',
  BLOB_ENTITY = 'BLOB_ENTITY',
  CUSTOMER_GROUP = 'CUSTOMER_GROUP',
  DEVICE_GROUP = 'DEVICE_GROUP',
  ASSET_GROUP = 'ASSET_GROUP',
  USER_GROUP = 'USER_GROUP',
  ENTITY_VIEW_GROUP = 'ENTITY_VIEW_GROUP',
  DASHBOARD_GROUP = 'DASHBOARD_GROUP',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION',
  WHITE_LABELING = 'WHITE_LABELING',
  AUDIT_LOG = 'AUDIT_LOG',
  API_USAGE_STATE = 'API_USAGE_STATE'
}

const resourceTypeTranslations = new Map<Resource, string>();
for (const key of Object.keys(Resource)) {
  resourceTypeTranslations.set(Resource[key], `permission.resource.display-type.${key}`);
}
export const resourceTypeTranslationMap = resourceTypeTranslations;

export const resourceByEntityType = new Map<EntityType, Resource>(
  [
    [EntityType.ALARM, Resource.ALARM],
    [EntityType.DEVICE, Resource.DEVICE],
    [EntityType.DEVICE_PROFILE, Resource.DEVICE_PROFILE],
    [EntityType.ASSET, Resource.ASSET],
    [EntityType.CUSTOMER, Resource.CUSTOMER],
    [EntityType.DASHBOARD, Resource.DASHBOARD],
    [EntityType.ENTITY_VIEW, Resource.ENTITY_VIEW],
    [EntityType.TENANT, Resource.TENANT],
    [EntityType.TENANT_PROFILE, Resource.TENANT_PROFILE],
    [EntityType.RULE_CHAIN, Resource.RULE_CHAIN],
    [EntityType.USER, Resource.USER],
    [EntityType.WIDGETS_BUNDLE, Resource.WIDGETS_BUNDLE],
    [EntityType.WIDGET_TYPE, Resource.WIDGET_TYPE],
    [EntityType.CONVERTER, Resource.CONVERTER],
    [EntityType.INTEGRATION, Resource.INTEGRATION],
    [EntityType.SCHEDULER_EVENT, Resource.SCHEDULER_EVENT],
    [EntityType.BLOB_ENTITY, Resource.BLOB_ENTITY],
    [EntityType.ROLE, Resource.ROLE],
    [EntityType.GROUP_PERMISSION, Resource.GROUP_PERMISSION]
  ]
);

export const groupResourceByGroupType = new Map<EntityType, Resource>(
  [
    [EntityType.CUSTOMER, Resource.CUSTOMER_GROUP],
    [EntityType.DEVICE, Resource.DEVICE_GROUP],
    [EntityType.ASSET, Resource.ASSET_GROUP],
    [EntityType.USER, Resource.USER_GROUP],
    [EntityType.ENTITY_VIEW, Resource.ENTITY_VIEW_GROUP],
    [EntityType.DASHBOARD, Resource.DASHBOARD_GROUP]
  ]
);

export const sharableGroupTypes = new Set<EntityType>(
  [
    EntityType.CUSTOMER,
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD
  ]
);

export const publicGroupTypes = new Set<EntityType>(
  [
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD
  ]
);

export interface MergedGroupPermissionInfo {
  entityType: EntityType;
  operations: Operation[];
}

export interface MergedGroupTypePermissionInfo {
  entityGroupIds: EntityGroupId[];
  hasGenericRead: boolean;
}

export interface MergedUserPermissions {
  genericPermissions: {[resource: string]: Operation[]};
  groupPermissions: {[entityGroupId: string]: MergedGroupPermissionInfo};
  readGroupPermissions: {[entityType: string]: MergedGroupTypePermissionInfo};
}

export interface AllowedPermissionsInfo {
  operationsByResource: {[resource: string]: Operation[]};
  allowedForGroupRoleOperations: Operation[];
  allowedForGroupOwnerOnlyOperations: Operation[];
  allowedForGroupOwnerOnlyGroupOperations: Operation[];
  allowedResources: Resource[];
  userPermissions: MergedUserPermissions;
  userOwnerId: EntityId;
}
