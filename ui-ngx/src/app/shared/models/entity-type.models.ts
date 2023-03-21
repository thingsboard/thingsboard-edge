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

import { TenantId } from './id/tenant-id';
import { BaseData, HasId } from '@shared/models/base-data';

export enum EntityType {
  TENANT = 'TENANT',
  TENANT_PROFILE = 'TENANT_PROFILE',
  CUSTOMER = 'CUSTOMER',
  USER = 'USER',
  DASHBOARD = 'DASHBOARD',
  ASSET = 'ASSET',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  ALARM = 'ALARM',
  ENTITY_GROUP = 'ENTITY_GROUP',
  CONVERTER = 'CONVERTER',
  INTEGRATION = 'INTEGRATION',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
  SCHEDULER_EVENT = 'SCHEDULER_EVENT',
  BLOB_ENTITY = 'BLOB_ENTITY',
  ENTITY_VIEW = 'ENTITY_VIEW',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION',
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  EDGE = 'EDGE',
  OTA_PACKAGE = 'OTA_PACKAGE',
  RPC = 'RPC',
  QUEUE = 'QUEUE',
  NOTIFICATION = 'NOTIFICATION',
  NOTIFICATION_REQUEST = 'NOTIFICATION_REQUEST',
  NOTIFICATION_RULE = 'NOTIFICATION_RULE',
  NOTIFICATION_TARGET = 'NOTIFICATION_TARGET',
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE'
}

export enum AliasEntityType {
  CURRENT_CUSTOMER = 'CURRENT_CUSTOMER',
  CURRENT_TENANT = 'CURRENT_TENANT',
  CURRENT_USER = 'CURRENT_USER',
  CURRENT_USER_OWNER = 'CURRENT_USER_OWNER'
}

export interface EntityTypeTranslation {
  type?: string;
  typePlural?: string;
  list?: string;
  nameStartsWith?: string;
  details?: string;
  add?: string;
  noEntities?: string;
  selectedEntities?: string;
  search?: string;
  selectGroupToAdd?: string;
  selectGroupToMove?: string;
  removeFromGroup?: string;
  group?: string;
  groupList?: string;
  groupNameStartsWith?: string;
}

export interface EntityTypeResource<T> {
  helpLinkId: string;
  helpLinkIdForEntity?(entity: T): string;
}

export const entityTypeTranslations = new Map<EntityType | AliasEntityType, EntityTypeTranslation>(
  [
    [
      EntityType.TENANT,
      {
        type: 'entity.type-tenant',
        typePlural: 'entity.type-tenants',
        list: 'entity.list-of-tenants',
        nameStartsWith: 'entity.tenant-name-starts-with',
        details: 'tenant.tenant-details',
        add: 'tenant.add',
        noEntities: 'tenant.no-tenants-text',
        search: 'tenant.search',
        selectedEntities: 'tenant.selected-tenants'
      }
    ],
    [
      EntityType.TENANT_PROFILE,
      {
        type: 'entity.type-tenant-profile',
        typePlural: 'entity.type-tenant-profiles',
        list: 'entity.list-of-tenant-profiles',
        nameStartsWith: 'entity.tenant-profile-name-starts-with',
        details: 'tenant-profile.tenant-profile-details',
        add: 'tenant-profile.add',
        noEntities: 'tenant-profile.no-tenant-profiles-text',
        search: 'tenant-profile.search',
        selectedEntities: 'tenant-profile.selected-tenant-profiles'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        type: 'entity.type-customer',
        typePlural: 'entity.type-customers',
        list: 'entity.list-of-customers',
        nameStartsWith: 'entity.customer-name-starts-with',
        details: 'customer.customer-details',
        add: 'customer.add',
        noEntities: 'customer.no-customers-text',
        search: 'customer.search',
        selectedEntities: 'customer.selected-customers',
        selectGroupToAdd: 'customer.select-group-to-add',
        selectGroupToMove: 'customer.select-group-to-move',
        removeFromGroup: 'customer.remove-customers-from-group',
        group: 'customer.group',
        groupList: 'customer.list-of-groups',
        groupNameStartsWith: 'customer.group-name-starts-with'
      }
    ],
    [
      EntityType.USER,
      {
        type: 'entity.type-user',
        typePlural: 'entity.type-users',
        list: 'entity.list-of-users',
        nameStartsWith: 'entity.user-name-starts-with',
        details: 'user.user-details',
        add: 'user.add',
        noEntities: 'user.no-users-text',
        search: 'user.search',
        selectedEntities: 'user.selected-users',
        selectGroupToAdd: 'user.select-group-to-add',
        selectGroupToMove: 'user.select-group-to-move',
        removeFromGroup: 'user.remove-users-from-group',
        group: 'user.group',
        groupList: 'user.list-of-groups',
        groupNameStartsWith: 'user.group-name-starts-with'
      }
    ],
    [
      EntityType.DEVICE,
      {
        type: 'entity.type-device',
        typePlural: 'entity.type-devices',
        list: 'entity.list-of-devices',
        nameStartsWith: 'entity.device-name-starts-with',
        details: 'device.device-details',
        add: 'device.add',
        noEntities: 'device.no-devices-text',
        search: 'device.search',
        selectedEntities: 'device.selected-devices',
        selectGroupToAdd: 'device.select-group-to-add',
        selectGroupToMove: 'device.select-group-to-move',
        removeFromGroup: 'device.remove-devices-from-group',
        group: 'device.group',
        groupList: 'device.list-of-groups',
        groupNameStartsWith: 'device.group-name-starts-with'
      }
    ],
    [
      EntityType.DEVICE_PROFILE,
      {
        type: 'entity.type-device-profile',
        typePlural: 'entity.type-device-profiles',
        list: 'entity.list-of-device-profiles',
        nameStartsWith: 'entity.device-profile-name-starts-with',
        details: 'device-profile.device-profile-details',
        add: 'device-profile.add',
        noEntities: 'device-profile.no-device-profiles-text',
        search: 'device-profile.search',
        selectedEntities: 'device-profile.selected-device-profiles'
      }
    ],
    [
      EntityType.ASSET_PROFILE,
      {
        type: 'entity.type-asset-profile',
        typePlural: 'entity.type-asset-profiles',
        list: 'entity.list-of-asset-profiles',
        nameStartsWith: 'entity.asset-profile-name-starts-with',
        details: 'asset-profile.asset-profile-details',
        add: 'asset-profile.add',
        noEntities: 'asset-profile.no-asset-profiles-text',
        search: 'asset-profile.search',
        selectedEntities: 'asset-profile.selected-asset-profiles'
      }
    ],
    [
      EntityType.ASSET,
      {
        type: 'entity.type-asset',
        typePlural: 'entity.type-assets',
        list: 'entity.list-of-assets',
        nameStartsWith: 'entity.asset-name-starts-with',
        details: 'asset.asset-details',
        add: 'asset.add',
        noEntities: 'asset.no-assets-text',
        search: 'asset.search',
        selectedEntities: 'asset.selected-assets',
        selectGroupToAdd: 'asset.select-group-to-add',
        selectGroupToMove: 'asset.select-group-to-move',
        removeFromGroup: 'asset.remove-assets-from-group',
        group: 'asset.group',
        groupList: 'asset.list-of-groups',
        groupNameStartsWith: 'asset.group-name-starts-with'
      }
    ],
    [
      EntityType.ENTITY_VIEW,
      {
        type: 'entity.type-entity-view',
        typePlural: 'entity.type-entity-views',
        list: 'entity.list-of-entity-views',
        nameStartsWith: 'entity.entity-view-name-starts-with',
        details: 'entity-view.entity-view-details',
        add: 'entity-view.add',
        noEntities: 'entity-view.no-entity-views-text',
        search: 'entity-view.search',
        selectedEntities: 'entity-view.selected-entity-views',
        selectGroupToAdd: 'entity-view.select-group-to-add',
        selectGroupToMove: 'entity-view.select-group-to-move',
        removeFromGroup: 'entity-view.remove-entity-views-from-group',
        group: 'entity-view.group',
        groupList: 'entity-view.list-of-groups',
        groupNameStartsWith: 'entity-view.group-name-starts-with'
      }
    ],
    [
      EntityType.EDGE,
      {
        type: 'entity.type-edge',
        typePlural: 'entity.type-edges',
        list: 'entity.list-of-edges',
        nameStartsWith: 'entity.edge-name-starts-with',
        details: 'edge.edge-details',
        add: 'edge.add',
        noEntities: 'edge.no-edges-text',
        search: 'edge.search',
        selectedEntities: 'edge.selected-edges',
        selectGroupToAdd: 'edge.select-group-to-add',
        selectGroupToMove: 'edge.select-group-to-move',
        removeFromGroup: 'edge.remove-edges-from-group',
        group: 'edge.group',
        groupList: 'edge.list-of-groups',
        groupNameStartsWith: 'edge.group-name-starts-with'
      }
    ],
    [
      EntityType.RULE_CHAIN,
      {
        type: 'entity.type-rulechain',
        typePlural: 'entity.type-rulechains',
        list: 'entity.list-of-rulechains',
        nameStartsWith: 'entity.rulechain-name-starts-with',
        details: 'rulechain.rulechain-details',
        add: 'rulechain.add',
        noEntities: 'rulechain.no-rulechains-text',
        search: 'rulechain.search',
        selectedEntities: 'rulechain.selected-rulechains'
      }
    ],
    [
      EntityType.RULE_NODE,
      {
        type: 'entity.type-rulenode',
        typePlural: 'entity.type-rulenodes',
        list: 'entity.list-of-rulenodes',
        nameStartsWith: 'entity.rulenode-name-starts-with'
      }
    ],
    [
      EntityType.DASHBOARD,
      {
        type: 'entity.type-dashboard',
        typePlural: 'entity.type-dashboards',
        list: 'entity.list-of-dashboards',
        nameStartsWith: 'entity.dashboard-name-starts-with',
        details: 'dashboard.dashboard-details',
        add: 'dashboard.add',
        noEntities: 'dashboard.no-dashboards-text',
        search: 'dashboard.search',
        selectedEntities: 'dashboard.selected-dashboards',
        selectGroupToAdd: 'dashboard.select-group-to-add',
        selectGroupToMove: 'dashboard.select-group-to-move',
        removeFromGroup: 'dashboard.remove-dashboards-from-group',
        group: 'dashboard.group',
        groupList: 'dashboard.list-of-groups',
        groupNameStartsWith: 'dashboard.group-name-starts-with'
      }
    ],
    [
      EntityType.ALARM,
      {
        type: 'entity.type-alarm',
        typePlural: 'entity.type-alarms',
        list: 'entity.list-of-alarms',
        nameStartsWith: 'entity.alarm-name-starts-with',
        details: 'alarm.alarm-details',
        noEntities: 'alarm.no-alarms-prompt',
        search: 'alarm.search',
        selectedEntities: 'alarm.selected-alarms'
      }
    ],
    [
      EntityType.ENTITY_GROUP,
      {
        type: 'entity.type-entity-group',
        details: 'entity-group.entity-group-details',
        add: 'entity-group.add',
        noEntities: 'entity-group.no-entity-groups-text',
        search: 'entity-group.search',
        selectedEntities: 'entity-group.selected-entity-groups'
      }
    ],
    [
      EntityType.API_USAGE_STATE,
      {
        type: 'entity.type-api-usage-state'
      }
    ],
    [
      EntityType.WIDGETS_BUNDLE,
      {
        type: 'entity.type-widgets-bundle',
        typePlural: 'entity.type-widgets-bundles',
        list: 'entity.list-of-widgets-bundles',
        details: 'widgets-bundle.widgets-bundle-details',
        add: 'widgets-bundle.add',
        noEntities: 'widgets-bundle.no-widgets-bundles-text',
        search: 'widgets-bundle.search',
        selectedEntities: 'widgets-bundle.selected-widgets-bundles'
      }
    ],
    [
      EntityType.CONVERTER,
      {
        type: 'entity.type-converter',
        typePlural: 'entity.type-converters',
        list: 'entity.list-of-converters',
        nameStartsWith: 'entity.converter-name-starts-with',
        details: 'converter.converter-details',
        add: 'converter.add',
        noEntities: 'converter.no-converters-text',
        search: 'converter.search',
        selectedEntities: 'converter.selected-converters'
      }
    ],
    [
      EntityType.INTEGRATION,
      {
        type: 'entity.type-integration',
        typePlural: 'entity.type-integrations',
        list: 'entity.list-of-integrations',
        nameStartsWith: 'entity.integration-name-starts-with',
        details: 'integration.integration-details',
        add: 'integration.add',
        noEntities: 'integration.no-integrations-text',
        search: 'integration.search',
        selectedEntities: 'integration.selected-integrations'
      }
    ],
    [
      EntityType.SCHEDULER_EVENT,
      {
        type: 'entity.type-scheduler-event',
        typePlural: 'entity.type-scheduler-events',
        list: 'entity.list-of-scheduler-events',
        nameStartsWith: 'entity.scheduler-event-name-starts-with'
      }
    ],
    [
      EntityType.BLOB_ENTITY,
      {
        type: 'entity.type-blob-entity',
        typePlural: 'entity.type-blob-entities',
        list: 'entity.list-of-blob-entities',
        nameStartsWith: 'entity.blob-entity-name-starts-with'
      }
    ],
    [
      EntityType.ROLE,
      {
        type: 'entity.type-role',
        typePlural: 'entity.type-roles',
        list: 'entity.list-of-roles',
        nameStartsWith: 'entity.role-name-starts-with',
        details: 'role.role-details',
        add: 'role.add',
        noEntities: 'role.no-roles-text',
        search: 'role.search',
        selectedEntities: 'role.selected-roles'
      }
    ],
    [
      EntityType.GROUP_PERMISSION,
      {
        type: 'entity.type-group-permission'
      }
    ],
    [
      AliasEntityType.CURRENT_CUSTOMER,
      {
        type: 'entity.type-current-customer',
        list: 'entity.type-current-customer'
      }
    ],
    [
      AliasEntityType.CURRENT_TENANT,
      {
        type: 'entity.type-current-tenant',
        list: 'entity.type-current-tenant'
      }
    ],
    [
      AliasEntityType.CURRENT_USER,
      {
        type: 'entity.type-current-user',
        list: 'entity.type-current-user'
      }
    ],
    [
      AliasEntityType.CURRENT_USER_OWNER,
      {
        type: 'entity.type-current-user-owner',
        list: 'entity.type-current-user-owner'
      }
    ],
    [
      EntityType.TB_RESOURCE,
      {
        type: 'entity.type-tb-resource',
        details: 'resource.resource-library-details',
        add: 'resource.add',
        noEntities: 'resource.no-resource-text',
        search: 'resource.search',
        selectedEntities: 'resource.selected-resources'
      }
    ],
    [
      EntityType.OTA_PACKAGE,
      {
        type: 'entity.type-ota-package',
        details: 'ota-update.ota-update-details',
        add: 'ota-update.add',
        noEntities: 'ota-update.no-packages-text',
        search: 'ota-update.search',
        selectedEntities: 'ota-update.selected-package'
      }
    ],
    [
      EntityType.RPC,
      {
        type: 'entity.type-rpc'
      }
    ],
    [
      EntityType.QUEUE,
      {
        type: 'entity.type-queue',
        add: 'queue.add',
        search: 'queue.search',
        details: 'queue.details',
        selectedEntities: 'queue.selected-queues'
      }
    ],
    [
      EntityType.NOTIFICATION,
      {
        type: 'entity.type-notification'
      }
    ],
    [
      EntityType.NOTIFICATION_REQUEST,
      {
        type: 'entity.type-notification-request'
      }
    ],
    [
      EntityType.NOTIFICATION_RULE,
      {
        type: 'entity.type-notification-rule'
      }
    ],
    [
      EntityType.NOTIFICATION_TARGET,
      {
        type: 'entity.type-notification-target'
      }
    ],
    [
      EntityType.NOTIFICATION_TEMPLATE,
      {
        type: 'entity.type-notification-template'
      }
    ]
  ]
);

export const entityTypeResources = new Map<EntityType, EntityTypeResource<BaseData<HasId>>>(
  [
    [
      EntityType.TENANT,
      {
        helpLinkId: 'tenants'
      }
    ],
    [
      EntityType.TENANT_PROFILE,
      {
        helpLinkId: 'tenantProfiles'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        helpLinkId: 'customers'
      }
    ],
    [
      EntityType.USER,
      {
        helpLinkId: 'users'
      }
    ],
    [
      EntityType.DEVICE,
      {
        helpLinkId: 'devices'
      }
    ],
    [
      EntityType.DEVICE_PROFILE,
      {
        helpLinkId: 'deviceProfiles'
      }
    ],
    [
      EntityType.ASSET_PROFILE,
      {
        helpLinkId: 'assetProfiles'
      }
    ],
    [
      EntityType.ASSET,
      {
        helpLinkId: 'assets'
      }
    ],
    [
      EntityType.EDGE,
      {
        helpLinkId: 'edges'
      }
    ],
    [
      EntityType.ENTITY_VIEW,
      {
        helpLinkId: 'entityViews'
      }
    ],
    [
      EntityType.EDGE,
      {
        helpLinkId: 'edges'
      }
    ],
    [
      EntityType.RULE_CHAIN,
      {
        helpLinkId: 'rulechains'
      }
    ],
    [
      EntityType.DASHBOARD,
      {
        helpLinkId: 'dashboards'
      }
    ],
    [
      EntityType.WIDGETS_BUNDLE,
      {
        helpLinkId: 'widgetsBundles'
      }
    ],
    [
      EntityType.ROLE,
      {
        helpLinkId: 'roles'
      }
    ],
    [
      EntityType.ENTITY_GROUP,
      {
        helpLinkId: 'entityGroups'
      }
    ],
    [
      EntityType.TB_RESOURCE,
      {
        helpLinkId: 'resources'
      }
    ],
    [
      EntityType.OTA_PACKAGE,
      {
        helpLinkId: 'otaUpdates'
      }
    ],
    [
      EntityType.QUEUE,
      {
        helpLinkId: 'queue'
      }
    ]
  ]
);

export const baseDetailsPageByEntityType = new Map<EntityType, string>([
  [EntityType.TENANT, '/tenants'],
  [EntityType.TENANT_PROFILE, '/tenantProfiles'],
  [EntityType.CUSTOMER, '/customers/all'],
  [EntityType.USER, '/users/all'],
  [EntityType.DASHBOARD, '/dashboards/all'],
  [EntityType.ASSET, '/entities/assets/all'],
  [EntityType.DEVICE, '/entities/devices/all'],
  [EntityType.DEVICE_PROFILE, '/profiles/deviceProfiles'],
  [EntityType.ASSET_PROFILE, '/profiles/assetProfiles'],
  [EntityType.CONVERTER, '/integrationsCenter/converters'],
  [EntityType.INTEGRATION, '/integrationsCenter/integrations'],
  [EntityType.RULE_CHAIN, '/features/ruleChains'],
  [EntityType.EDGE, '/edgeManagement/instances/all'],
  [EntityType.ENTITY_VIEW, '/entities/entityViews/all'],
  [EntityType.ROLE, '/roles'],
  [EntityType.TB_RESOURCE, '/resources/resources-library'],
  [EntityType.OTA_PACKAGE, '/features/otaUpdates'],
  [EntityType.QUEUE, '/settings/queues']
]);

export interface EntitySubtype {
  tenantId: TenantId;
  entityType: EntityType;
  type: string;
}
