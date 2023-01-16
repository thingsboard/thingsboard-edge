///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { EdgeId } from '@shared/models/id/edge-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { BaseEventBody } from '@shared/models/event.models';
import { EventId } from '@shared/models/id/event-id';
import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';

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
  WIDGET_TYPE = 'WIDGET_TYPE'
}

export interface Edge extends BaseData<EdgeId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  secret: string;
  routingKey: string;
  label?: string;
  additionalInfo?: any;
  rootRuleChainId?: RuleChainId;
}

export interface EdgeInfo extends Edge {
  customerTitle: string;
  customerIsPublic: boolean;
}

export interface EdgeSearchQuery extends EntitySearchQuery {
  edgeTypes: Array<string>;
}

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
  TENANT = 'TENANT',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS'
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
  ENTITY_VIEW_REQUEST = 'ENTITY_VIEW_REQUEST'
}

export enum EdgeEventActionType {
  ADDED = 'ADDED',
  DELETED = 'DELETED',
  UPDATED = 'UPDATED',
  POST_ATTRIBUTES = 'POST_ATTRIBUTES',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED',
  CREDENTIALS_UPDATED = 'CREDENTIALS_UPDATED',
  ASSIGNED_TO_CUSTOMER = 'ASSIGNED_TO_CUSTOMER',
  UNASSIGNED_FROM_CUSTOMER = 'UNASSIGNED_FROM_CUSTOMER',
  RELATION_ADD_OR_UPDATE = 'RELATION_ADD_OR_UPDATE',
  RELATION_DELETED = 'RELATION_DELETED',
  RPC_CALL = 'RPC_CALL',
  ALARM_ACK = 'ALARM_ACK',
  ALARM_CLEAR = 'ALARM_CLEAR',
  ASSIGNED_TO_EDGE = 'ASSIGNED_TO_EDGE',
  UNASSIGNED_FROM_EDGE = 'UNASSIGNED_FROM_EDGE',
  CREDENTIALS_REQUEST = 'CREDENTIALS_REQUEST',
  ENTITY_MERGE_REQUEST = 'ENTITY_MERGE_REQUEST'
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
    [CloudEventActionType.ENTITY_VIEW_REQUEST, 'cloud-event.cloud-event-action-entity-view-request']
  ]
);

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
    [EdgeEventType.ADMIN_SETTINGS, 'edge-event.type-admin-settings']
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
    [EdgeEventActionType.ASSIGNED_TO_CUSTOMER, 'edge-event.action-type-assigned-to-customer'],
    [EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER, 'edge-event.action-type-unassigned-from-customer'],
    [EdgeEventActionType.RELATION_ADD_OR_UPDATE, 'edge-event.action-type-relation-add-or-update'],
    [EdgeEventActionType.RELATION_DELETED, 'edge-event.action-type-relation-deleted'],
    [EdgeEventActionType.RPC_CALL, 'edge-event.action-type-rpc-call'],
    [EdgeEventActionType.ALARM_ACK, 'edge-event.action-type-alarm-ack'],
    [EdgeEventActionType.ALARM_CLEAR, 'edge-event.action-type-alarm-clear'],
    [EdgeEventActionType.ASSIGNED_TO_EDGE, 'edge-event.action-type-assigned-to-edge'],
    [EdgeEventActionType.UNASSIGNED_FROM_EDGE, 'edge-event.action-type-unassigned-from-edge'],
    [EdgeEventActionType.CREDENTIALS_REQUEST, 'edge-event.action-type-credentials-request'],
    [EdgeEventActionType.ENTITY_MERGE_REQUEST, 'edge-event.action-type-entity-merge-request']
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
