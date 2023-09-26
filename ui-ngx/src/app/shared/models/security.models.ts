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
  ASSET_PROFILE = 'ASSET_PROFILE',
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
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  EDGE = 'EDGE',
  EDGE_GROUP = 'EDGE_GROUP',
  OTA_PACKAGE = 'OTA_PACKAGE',
  QUEUE = 'QUEUE',
  VERSION_CONTROL = 'VERSION_CONTROL',
  NOTIFICATION = 'NOTIFICATION'
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
    [EntityType.ASSET_PROFILE, Resource.ASSET_PROFILE],
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
    [EntityType.GROUP_PERMISSION, Resource.GROUP_PERMISSION],
    [EntityType.TB_RESOURCE, Resource.TB_RESOURCE],
    [EntityType.EDGE, Resource.EDGE],
    [EntityType.OTA_PACKAGE, Resource.OTA_PACKAGE],
    [EntityType.QUEUE, Resource.QUEUE]
  ]
);

export const groupResourceByGroupType = new Map<EntityType, Resource>(
  [
    [EntityType.CUSTOMER, Resource.CUSTOMER_GROUP],
    [EntityType.DEVICE, Resource.DEVICE_GROUP],
    [EntityType.ASSET, Resource.ASSET_GROUP],
    [EntityType.USER, Resource.USER_GROUP],
    [EntityType.ENTITY_VIEW, Resource.ENTITY_VIEW_GROUP],
    [EntityType.DASHBOARD, Resource.DASHBOARD_GROUP],
    [EntityType.EDGE, Resource.EDGE_GROUP],
  ]
);

export const sharableGroupTypes = new Set<EntityType>(
  [
    EntityType.CUSTOMER,
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD,
    EntityType.EDGE
  ]
);

export const publicGroupTypes = new Set<EntityType>(
  [
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD,
    EntityType.EDGE
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
