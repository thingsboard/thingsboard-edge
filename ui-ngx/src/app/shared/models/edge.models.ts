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
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EdgeId } from '@shared/models/id/edge-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { BaseEventBody } from '@shared/models/event.models';
import { EventId } from '@shared/models/id/event-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityInfoData } from '@shared/models/entity.models';

export interface EdgeSettings {
  edgeId: string;
  tenantId: string;
  name: string;
  type: string;
  routingKey: string;
}

export interface CloudEvent extends BaseData<CloudEventId> {
  action: string;
  type: CloudEventType;
  entityBody: any;
  entityGroupId: EntityGroupId;
  entityId: EntityId;
  tenantId: TenantId;
}

export class CloudEventId implements HasUUID {
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}

export enum CloudEventType {
  DASHBOARD = 'DASHBOARD',
  ASSET = 'ASSET',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ENTITY_VIEW = 'ENTITY_VIEW',
  ALARM = 'ALARM',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_CHAIN_METADATA = 'RULE_CHAIN_METADATA',
  USER = 'USER',
  CUSTOMER = 'CUSTOMER',
  RELATION = 'RELATION',
  EDGE = 'EDGE',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  ENTITY_GROUP = 'ENTITY_GROUP'
}

export interface Edge extends BaseData<EdgeId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  secret: string;
  routingKey: string;
  cloudEndpoint: string;
  edgeLicenseKey: string;
  label?: string;
  additionalInfo?: any;
  rootRuleChainId?: RuleChainId;
}

export interface EdgeInfo extends Edge {
  ownerName?: string;
  groups?: EntityInfoData[];
}

export interface EdgeSearchQuery extends EntitySearchQuery {
  edgeTypes: Array<string>;
}

// PE MERGE
export enum EdgeEventType {
  DASHBOARD = 'DASHBOARD',
  ASSET = 'ASSET',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  ENTITY_VIEW = 'ENTITY_VIEW',
  ALARM = 'ALARM',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_CHAIN_METADATA = 'RULE_CHAIN_METADATA',
  EDGE = 'EDGE',
  USER = 'USER',
  CUSTOMER = 'CUSTOMER',
  RELATION = 'RELATION',
  GROUP_PERMISSIONS_REQUEST = 'GROUP_PERMISSIONS_REQUEST',
  TENANT = 'TENANT',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  ENTITY_GROUP = 'ENTITY_GROUP',
  SCHEDULER_EVENT = 'SCHEDULER_EVENT',
  WHITE_LABELING = 'WHITE_LABELING',
  LOGIN_WHITE_LABELING = 'LOGIN_WHITE_LABELING',
  CUSTOM_TRANSLATION = 'CUSTOM_TRANSLATION',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION',
  INTEGRATION = 'INTEGRATION',
  CONVERTER = 'CONVERTER',
}

export enum CloudEventActionType {
  ADDED = 'ADDED',
  DELETED = 'DELETED',
  UPDATED = 'UPDATED',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  TIMESERIES_DELETED = 'TIMESERIES_DELETED',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED',
  RPC_CALL = 'RPC_CALL',
  CREDENTIALS_UPDATED = 'CREDENTIALS_UPDATED',
  RELATION_ADD_OR_UPDATE = 'RELATION_ADD_OR_UPDATE',
  RELATION_DELETED = 'RELATION_DELETED',
  RELATIONS_DELETED = 'RELATIONS_DELETED',
  ALARM_ACK = 'ALARM_ACK',
  ALARM_CLEAR = 'ALARM_CLEAR',
  ATTRIBUTES_REQUEST = 'ATTRIBUTES_REQUEST',
  RULE_CHAIN_METADATA_REQUEST = 'RULE_CHAIN_METADATA_REQUEST',
  RELATION_REQUEST = 'RELATION_REQUEST',
  CREDENTIALS_REQUEST = 'CREDENTIALS_REQUEST',
  WIDGET_BUNDLE_TYPES_REQUEST = 'WIDGET_BUNDLE_TYPES_REQUEST',
  ENTITY_VIEW_REQUEST = 'ENTITY_VIEW_REQUEST',
  GROUP_ENTITIES_REQUEST = 'GROUP_ENTITIES_REQUEST',
  GROUP_PERMISSIONS_REQUEST = 'GROUP_PERMISSIONS_REQUEST',
  ADDED_TO_ENTITY_GROUP = 'ADDED_TO_ENTITY_GROUP',
  REMOVED_FROM_ENTITY_GROUP = 'REMOVED_FROM_ENTITY_GROUP'
}

// PE MERGE
export enum EdgeEventActionType {
  ADDED = 'ADDED',
  DELETED = 'DELETED',
  UPDATED = 'UPDATED',
  POST_ATTRIBUTES = 'POST_ATTRIBUTES',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED',
  CREDENTIALS_UPDATED = 'CREDENTIALS_UPDATED',
  RELATION_ADD_OR_UPDATE = 'RELATION_ADD_OR_UPDATE',
  RELATION_DELETED = 'RELATION_DELETED',
  RPC_CALL = 'RPC_CALL',
  ALARM_ACK = 'ALARM_ACK',
  ALARM_CLEAR = 'ALARM_CLEAR',
  ASSIGNED_TO_EDGE = 'ASSIGNED_TO_EDGE',
  UNASSIGNED_FROM_EDGE = 'UNASSIGNED_FROM_EDGE',
  CREDENTIALS_REQUEST = 'CREDENTIALS_REQUEST',
  ENTITY_MERGE_REQUEST = 'ENTITY_MERGE_REQUEST',
  ADDED_TO_ENTITY_GROUP = 'ADDED_TO_ENTITY_GROUP',
  REMOVED_FROM_ENTITY_GROUP = 'REMOVED_FROM_ENTITY_GROUP',
  CHANGE_OWNER = 'CHANGE_OWNER',
  RELATIONS_DELETED = 'RELATIONS_DELETED'
}

export enum EdgeEventStatus {
  DEPLOYED = 'DEPLOYED',
  PENDING = 'PENDING'
}

export const cloudEventTypeTranslations = new Map<CloudEventType, string>(
  [
    [CloudEventType.DASHBOARD, 'cloud-event.cloud-event-type-dashboard'],
    [CloudEventType.ASSET, 'cloud-event.cloud-event-type-asset'],
    [CloudEventType.DEVICE, 'cloud-event.cloud-event-type-device'],
    [CloudEventType.DEVICE_PROFILE, 'cloud-event.cloud-event-type-device-profile'],
    [CloudEventType.ENTITY_VIEW, 'cloud-event.cloud-event-type-entity-view'],
    [CloudEventType.ENTITY_GROUP, 'cloud-event.cloud-event-type-entity-group'],
    [CloudEventType.ALARM, 'cloud-event.cloud-event-type-alarm'],
    [CloudEventType.RULE_CHAIN, 'cloud-event.cloud-event-type-rule-chain'],
    [CloudEventType.RULE_CHAIN_METADATA, 'cloud-event.cloud-event-type-rule-chain-metadata'],
    [CloudEventType.EDGE, 'cloud-event.cloud-event-type-edge'],
    [CloudEventType.USER, 'cloud-event.cloud-event-type-user'],
    [CloudEventType.CUSTOMER, 'cloud-event.cloud-event-type-customer'],
    [CloudEventType.RELATION, 'cloud-event.cloud-event-type-relation'],
    [CloudEventType.WIDGETS_BUNDLE, 'cloud-event.cloud-event-type-widgets-bundle']
  ]
);

export const cloudEventActionTypeTranslations = new Map<string, string>(
  [
    [CloudEventActionType.ADDED, 'cloud-event.cloud-event-action-added'],
    [CloudEventActionType.DELETED, 'cloud-event.cloud-event-action-deleted'],
    [CloudEventActionType.UPDATED, 'cloud-event.cloud-event-action-updated'],
    [CloudEventActionType.ATTRIBUTES_UPDATED, 'cloud-event.cloud-event-action-attributes-updated'],
    [CloudEventActionType.ATTRIBUTES_DELETED, 'cloud-event.cloud-event-action-attributes-deleted'],
    [CloudEventActionType.TIMESERIES_DELETED, 'cloud-event.cloud-event-action-timeseries-deleted'],
    [CloudEventActionType.TIMESERIES_UPDATED, 'cloud-event.cloud-event-action-timeseries-updated'],
    [CloudEventActionType.RPC_CALL, 'cloud-event.cloud-event-action-rpc-call'],
    [CloudEventActionType.CREDENTIALS_UPDATED, 'cloud-event.cloud-event-action-credentials-updated'],
    [CloudEventActionType.RELATION_ADD_OR_UPDATE, 'cloud-event.cloud-event-action-relation-add-or-update'],
    [CloudEventActionType.RELATION_DELETED, 'cloud-event.cloud-event-action-relation-deleted'],
    [CloudEventActionType.RELATIONS_DELETED, 'cloud-event.cloud-event-action-relations-deleted'],
    [CloudEventActionType.ALARM_ACK, 'cloud-event.cloud-event-action-alarm-ack'],
    [CloudEventActionType.ALARM_CLEAR, 'cloud-event.cloud-event-action-alarm-clear'],
    [CloudEventActionType.ATTRIBUTES_REQUEST, 'cloud-event.cloud-event-action-attributes-request'],
    [CloudEventActionType.RULE_CHAIN_METADATA_REQUEST, 'cloud-event.cloud-event-action-rule-chain-metadata-request'],
    [CloudEventActionType.RELATION_REQUEST, 'cloud-event.cloud-event-action-relation-request'],
    [CloudEventActionType.CREDENTIALS_REQUEST, 'cloud-event.cloud-event-action-credentials-request'],
    [CloudEventActionType.WIDGET_BUNDLE_TYPES_REQUEST, 'cloud-event.cloud-event-action-widget-bundle-types-request'],
    [CloudEventActionType.ENTITY_VIEW_REQUEST, 'cloud-event.cloud-event-action-entity-view-request'],
    [CloudEventActionType.GROUP_PERMISSIONS_REQUEST, 'cloud-event.cloud-event-action-group-permissions-request'],
    [CloudEventActionType.GROUP_ENTITIES_REQUEST, 'cloud-event.cloud-event-action-group-entities-request'],
    [CloudEventActionType.ADDED_TO_ENTITY_GROUP, 'cloud-event.cloud-event-action-added-to-entity-group'],
    [CloudEventActionType.REMOVED_FROM_ENTITY_GROUP, 'cloud-event.cloud-event-action-removed-from-entity-group']
  ]
);

// PE MERGE
export const edgeEventTypeTranslations = new Map<EdgeEventType, string>(
  [
    [EdgeEventType.DASHBOARD, 'edge-event.type-dashboard'],
    [EdgeEventType.ASSET, 'edge-event.type-asset'],
    [EdgeEventType.DEVICE, 'edge-event.type-device'],
    [EdgeEventType.DEVICE_PROFILE, 'edge-event.type-device-profile'],
    [EdgeEventType.ASSET_PROFILE, 'edge-event.type-asset-profile'],
    [EdgeEventType.ENTITY_VIEW, 'edge-event.type-entity-view'],
    [EdgeEventType.ALARM, 'edge-event.type-alarm'],
    [EdgeEventType.RULE_CHAIN, 'edge-event.type-rule-chain'],
    [EdgeEventType.RULE_CHAIN_METADATA, 'edge-event.type-rule-chain-metadata'],
    [EdgeEventType.EDGE, 'edge-event.type-edge'],
    [EdgeEventType.USER, 'edge-event.type-user'],
    [EdgeEventType.CUSTOMER, 'edge-event.type-customer'],
    [EdgeEventType.RELATION, 'edge-event.type-relation'],
    [EdgeEventType.TENANT, 'edge-event.type-tenant'],
    [EdgeEventType.WIDGETS_BUNDLE, 'edge-event.type-widgets-bundle'],
    [EdgeEventType.WIDGET_TYPE, 'edge-event.type-widgets-type'],
    [EdgeEventType.ADMIN_SETTINGS, 'edge-event.type-admin-settings'],
    [EdgeEventType.ENTITY_GROUP, 'edge-event.type-entity-group'],
    [EdgeEventType.SCHEDULER_EVENT, 'edge-event.type-scheduler-event'],
    [EdgeEventType.TENANT, 'edge-event.type-tenant'],
    [EdgeEventType.WHITE_LABELING, 'edge-event.type-white-labeling'],
    [EdgeEventType.LOGIN_WHITE_LABELING, 'edge-event.type-login-white-labeling'],
    [EdgeEventType.CUSTOM_TRANSLATION, 'edge-event.type-custom-translation'],
    [EdgeEventType.ROLE, 'edge-event.type-role'],
    [EdgeEventType.GROUP_PERMISSION, 'edge-event.type-group-permission'],
    [EdgeEventType.INTEGRATION, 'edge-event.type-integration'],
    [EdgeEventType.CONVERTER, 'edge-event.type-converter']
  ]
);

export const edgeEventActionTypeTranslations = new Map<EdgeEventActionType, string>(
  [
    [EdgeEventActionType.ADDED, 'edge-event.action-type-added'],
    [EdgeEventActionType.DELETED, 'edge-event.action-type-deleted'],
    [EdgeEventActionType.UPDATED, 'edge-event.action-type-updated'],
    [EdgeEventActionType.POST_ATTRIBUTES, 'edge-event.action-type-post-attributes'],
    [EdgeEventActionType.ATTRIBUTES_UPDATED, 'edge-event.action-type-attributes-updated'],
    [EdgeEventActionType.ATTRIBUTES_DELETED, 'edge-event.action-type-attributes-deleted'],
    [EdgeEventActionType.TIMESERIES_UPDATED, 'edge-event.action-type-timeseries-updated'],
    [EdgeEventActionType.CREDENTIALS_UPDATED, 'edge-event.action-type-credentials-updated'],
    [EdgeEventActionType.RELATION_ADD_OR_UPDATE, 'edge-event.action-type-relation-add-or-update'],
    [EdgeEventActionType.RELATION_DELETED, 'edge-event.action-type-relation-deleted'],
    [EdgeEventActionType.RPC_CALL, 'edge-event.action-type-rpc-call'],
    [EdgeEventActionType.ALARM_ACK, 'edge-event.action-type-alarm-ack'],
    [EdgeEventActionType.ALARM_CLEAR, 'edge-event.action-type-alarm-clear'],
    [EdgeEventActionType.ASSIGNED_TO_EDGE, 'edge-event.action-type-assigned-to-edge'],
    [EdgeEventActionType.UNASSIGNED_FROM_EDGE, 'edge-event.action-type-unassigned-from-edge'],
    [EdgeEventActionType.CREDENTIALS_REQUEST, 'edge-event.action-type-credentials-request'],
    [EdgeEventActionType.ENTITY_MERGE_REQUEST, 'edge-event.action-type-entity-merge-request'],
    [EdgeEventActionType.ADDED_TO_ENTITY_GROUP, 'edge-event.action-type-added-to-entity-group'],
    [EdgeEventActionType.REMOVED_FROM_ENTITY_GROUP, 'edge-event.action-type-removed-from-entity-group'],
    [EdgeEventActionType.CHANGE_OWNER, 'edge-event.action-type-change-owner'],
    [EdgeEventActionType.RELATIONS_DELETED, 'edge-event.action-type-relations-deleted']
  ]
);

export const bodyContentEdgeEventActionTypes: EdgeEventActionType[] = [
  EdgeEventActionType.POST_ATTRIBUTES,
  EdgeEventActionType.ATTRIBUTES_UPDATED,
  EdgeEventActionType.ATTRIBUTES_DELETED,
  EdgeEventActionType.TIMESERIES_UPDATED,
  EdgeEventActionType.RPC_CALL
];

export const edgeEventStatusColor = new Map<EdgeEventStatus, string>(
  [
    [EdgeEventStatus.DEPLOYED, '#000000'],
    [EdgeEventStatus.PENDING, '#9e9e9e']
  ]
);

export interface EdgeEvent extends BaseData<EventId> {
  tenantId: TenantId;
  entityId: string;
  edgeId: EdgeId;
  action: EdgeEventActionType;
  type: EdgeEventType;
  uid: string;
  body: string;
}

export interface EdgeInstallInstructions {
  dockerInstallInstructions: string;
}

export const edgeEntityGroupTypes: EntityType[] = [
  EntityType.USER,
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD
];

