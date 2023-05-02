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

import { BaseData } from './base-data';
import { AuditLogId } from './id/audit-log-id';
import { CustomerId } from './id/customer-id';
import { EntityId } from './id/entity-id';
import { UserId } from './id/user-id';
import { TenantId } from './id/tenant-id';

export enum AuditLogMode {
  TENANT,
  ENTITY,
  USER,
  CUSTOMER
}

export enum ActionType {
  ADDED = 'ADDED',
  DELETED = 'DELETED',
  UPDATED = 'UPDATED',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  RPC_CALL = 'RPC_CALL',
  CREDENTIALS_UPDATED = 'CREDENTIALS_UPDATED',
  ASSIGNED_TO_CUSTOMER = 'ASSIGNED_TO_CUSTOMER',
  UNASSIGNED_FROM_CUSTOMER = 'UNASSIGNED_FROM_CUSTOMER',
  ACTIVATED = 'ACTIVATED',
  SUSPENDED = 'SUSPENDED',
  CREDENTIALS_READ = 'CREDENTIALS_READ',
  ATTRIBUTES_READ = 'ATTRIBUTES_READ',
  ADDED_TO_ENTITY_GROUP = 'ADDED_TO_ENTITY_GROUP',
  REMOVED_FROM_ENTITY_GROUP = 'REMOVED_FROM_ENTITY_GROUP',
  RELATION_ADD_OR_UPDATE = 'RELATION_ADD_OR_UPDATE',
  RELATION_DELETED = 'RELATION_DELETED',
  RELATIONS_DELETED = 'RELATIONS_DELETED',
  ALARM_ACK = 'ALARM_ACK',
  ALARM_CLEAR = 'ALARM_CLEAR',
  ALARM_ASSIGNED = 'ALARM_ASSIGNED',
  ALARM_UNASSIGNED = 'ALARM_UNASSIGNED',
  ADDED_COMMENT = 'ADDED_COMMENT',
  UPDATED_COMMENT = 'UPDATED_COMMENT',
  DELETED_COMMENT = 'DELETED_COMMENT',
  REST_API_RULE_ENGINE_CALL = 'REST_API_RULE_ENGINE_CALL',
  MADE_PUBLIC = 'MADE_PUBLIC',
  MADE_PRIVATE = 'MADE_PRIVATE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  LOCKOUT = 'LOCKOUT',
  ASSIGNED_FROM_TENANT = 'ASSIGNED_FROM_TENANT',
  ASSIGNED_TO_TENANT = 'ASSIGNED_TO_TENANT',
  PROVISION_SUCCESS = 'PROVISION_SUCCESS',
  PROVISION_FAILURE = 'PROVISION_FAILURE',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED',
  TIMESERIES_DELETED = 'TIMESERIES_DELETED',
  CHANGE_OWNER = 'CHANGE_OWNER',
  ASSIGNED_TO_EDGE = 'ASSIGNED_TO_EDGE',
  UNASSIGNED_FROM_EDGE = 'UNASSIGNED_FROM_EDGE'
}

export enum ActionStatus {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE'
}

export const actionTypeTranslations = new Map<ActionType, string>(
  [
    [ActionType.ADDED, 'audit-log.type-added'],
    [ActionType.DELETED, 'audit-log.type-deleted'],
    [ActionType.UPDATED, 'audit-log.type-updated'],
    [ActionType.ATTRIBUTES_UPDATED, 'audit-log.type-attributes-updated'],
    [ActionType.ATTRIBUTES_DELETED, 'audit-log.type-attributes-deleted'],
    [ActionType.RPC_CALL, 'audit-log.type-rpc-call'],
    [ActionType.CREDENTIALS_UPDATED, 'audit-log.type-credentials-updated'],
    [ActionType.ASSIGNED_TO_CUSTOMER, 'audit-log.type-assigned-to-customer'],
    [ActionType.UNASSIGNED_FROM_CUSTOMER, 'audit-log.type-unassigned-from-customer'],
    [ActionType.ACTIVATED, 'audit-log.type-activated'],
    [ActionType.SUSPENDED, 'audit-log.type-suspended'],
    [ActionType.CREDENTIALS_READ, 'audit-log.type-credentials-read'],
    [ActionType.ATTRIBUTES_READ, 'audit-log.type-attributes-read'],
    [ActionType.ADDED_TO_ENTITY_GROUP, 'audit-log.type-added-to-entity-group'],
    [ActionType.REMOVED_FROM_ENTITY_GROUP, 'audit-log.type-removed-from-entity-group'],
    [ActionType.RELATION_ADD_OR_UPDATE, 'audit-log.type-relation-add-or-update'],
    [ActionType.RELATION_DELETED, 'audit-log.type-relation-delete'],
    [ActionType.RELATIONS_DELETED, 'audit-log.type-relations-delete'],
    [ActionType.ALARM_ACK, 'audit-log.type-alarm-ack'],
    [ActionType.ALARM_CLEAR, 'audit-log.type-alarm-clear'],
    [ActionType.ALARM_ASSIGNED, 'audit-log.type-alarm-assign'],
    [ActionType.ALARM_UNASSIGNED, 'audit-log.type-alarm-unassign'],
    [ActionType.ADDED_COMMENT, 'audit-log.type-added-comment'],
    [ActionType.UPDATED_COMMENT, 'audit-log.type-updated-comment'],
    [ActionType.DELETED_COMMENT, 'audit-log.type-deleted-comment'],
    [ActionType.REST_API_RULE_ENGINE_CALL, 'audit-log.type-rest-api-rule-engine-call'],
    [ActionType.MADE_PUBLIC, 'audit-log.type-made-public'],
    [ActionType.MADE_PRIVATE, 'audit-log.type-made-private'],
    [ActionType.LOGIN, 'audit-log.type-login'],
    [ActionType.LOGOUT, 'audit-log.type-logout'],
    [ActionType.LOCKOUT, 'audit-log.type-lockout'],
    [ActionType.ASSIGNED_FROM_TENANT, 'audit-log.type-assigned-from-tenant'],
    [ActionType.ASSIGNED_TO_TENANT, 'audit-log.type-assigned-to-tenant'],
    [ActionType.PROVISION_SUCCESS, 'audit-log.type-provision-success'],
    [ActionType.PROVISION_FAILURE, 'audit-log.type-provision-failure'],
    [ActionType.TIMESERIES_UPDATED, 'audit-log.type-timeseries-updated'],
    [ActionType.TIMESERIES_DELETED, 'audit-log.type-timeseries-deleted'],
    [ActionType.CHANGE_OWNER, 'audit-log.type-owner-changed'],
    [ActionType.ASSIGNED_TO_EDGE, 'audit-log.type-assigned-to-edge'],
    [ActionType.UNASSIGNED_FROM_EDGE, 'audit-log.type-unassigned-from-edge']
  ]
);

export const actionStatusTranslations = new Map<ActionStatus, string>(
  [
    [ActionStatus.SUCCESS, 'audit-log.status-success'],
    [ActionStatus.FAILURE, 'audit-log.status-failure'],
  ]
);

export interface AuditLog extends BaseData<AuditLogId> {
  tenantId: TenantId;
  customerId: CustomerId;
  entityId: EntityId;
  entityName: string;
  userId: UserId;
  userName: string;
  actionType: ActionType;
  actionData: any;
  actionStatus: ActionStatus;
  actionFailureDetails: string;
}
