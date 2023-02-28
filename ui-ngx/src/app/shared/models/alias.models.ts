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
import { EntitySearchDirection, RelationEntityTypeFilter } from '@shared/models/relation.models';
import { EntityFilter } from '@shared/models/query/query.models';

export enum AliasFilterType {
  singleEntity = 'singleEntity',
  entityGroup = 'entityGroup',
  entityList = 'entityList',
  entityName = 'entityName',
  entityType = 'entityType',
  entityGroupList = 'entityGroupList',
  entityGroupName = 'entityGroupName',
  entitiesByGroupName = 'entitiesByGroupName',
  stateEntity = 'stateEntity',
  stateEntityOwner = 'stateEntityOwner',
  assetType = 'assetType',
  deviceType = 'deviceType',
  entityViewType = 'entityViewType',
  edgeType = 'edgeType',
  apiUsageState = 'apiUsageState',
  relationsQuery = 'relationsQuery',
  assetSearchQuery = 'assetSearchQuery',
  deviceSearchQuery = 'deviceSearchQuery',
  entityViewSearchQuery = 'entityViewSearchQuery',
  edgeSearchQuery = 'edgeSearchQuery',
  schedulerEvent = 'schedulerEvent'
}

export const edgeAliasFilterTypes = new Array<string>(
  AliasFilterType.edgeType,
  AliasFilterType.edgeSearchQuery
);

export const aliasFilterTypeTranslationMap = new Map<AliasFilterType, string>(
  [
    [ AliasFilterType.singleEntity, 'alias.filter-type-single-entity' ],
    [ AliasFilterType.entityGroup, 'alias.filter-type-entity-group' ],
    [ AliasFilterType.entityList, 'alias.filter-type-entity-list' ],
    [ AliasFilterType.entityName, 'alias.filter-type-entity-name' ],
    [ AliasFilterType.entityType, 'alias.filter-type-entity-type' ],
    [ AliasFilterType.entityGroupList, 'alias.filter-type-entity-group-list' ],
    [ AliasFilterType.entityGroupName, 'alias.filter-type-entity-group-name' ],
    [ AliasFilterType.entitiesByGroupName, 'alias.filter-type-entities-by-group-name' ],
    [ AliasFilterType.stateEntity, 'alias.filter-type-state-entity' ],
    [ AliasFilterType.stateEntityOwner, 'alias.filter-type-state-entity-owner' ],
    [ AliasFilterType.assetType, 'alias.filter-type-asset-type' ],
    [ AliasFilterType.deviceType, 'alias.filter-type-device-type' ],
    [ AliasFilterType.entityViewType, 'alias.filter-type-entity-view-type' ],
    [ AliasFilterType.edgeType, 'alias.filter-type-edge-type' ],
    [ AliasFilterType.apiUsageState, 'alias.filter-type-apiUsageState' ],
    [ AliasFilterType.relationsQuery, 'alias.filter-type-relations-query' ],
    [ AliasFilterType.assetSearchQuery, 'alias.filter-type-asset-search-query' ],
    [ AliasFilterType.deviceSearchQuery, 'alias.filter-type-device-search-query' ],
    [ AliasFilterType.entityViewSearchQuery, 'alias.filter-type-entity-view-search-query' ],
    [ AliasFilterType.edgeSearchQuery, 'alias.filter-type-edge-search-query' ],
    [ AliasFilterType.schedulerEvent, 'alias.filter-type-scheduler-event' ]
  ]
);

export interface SingleEntityFilter {
  singleEntity?: EntityId;
}

export interface EntityGroupFilter {
  groupStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateGroupType?: EntityType;
  defaultStateEntityGroup?: string;
  groupType?: EntityType;
  entityGroup?: string;
}

export interface EntityListFilter {
  entityType?: EntityType;
  entityList?: string[];
}

export interface EntityNameFilter {
  entityType?: EntityType;
  entityNameFilter?: string;
}

export interface EntityTypeFilter {
  entityType?: EntityType;
}

export interface EntityGroupListFilter {
  groupType?: EntityType;
  entityGroupList?: string[];
}

export interface EntityGroupNameFilter {
  groupType?: EntityType;
  entityGroupNameFilter?: string;
}

export interface EntitiesByGroupNameFilter {
  groupStateEntity?: boolean;
  stateEntityParamName?: string;
  groupType?: EntityType;
  ownerId?: EntityId;
  entityGroupNameFilter?: string;
}

export interface StateEntityFilter {
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
}

export interface StateEntityOwnerFilter {
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
}

export interface AssetTypeFilter {
  assetType?: string;
  assetNameFilter?: string;
}

export interface DeviceTypeFilter {
  deviceType?: string;
  deviceNameFilter?: string;
}

export interface EntityViewFilter {
  entityViewType?: string;
  entityViewNameFilter?: string;
}

export interface EdgeTypeFilter {
  edgeType?: string;
  edgeNameFilter?: string;
}

export interface RelationsQueryFilter {
  rootStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  rootEntity?: EntityId;
  direction?: EntitySearchDirection;
  filters?: Array<RelationEntityTypeFilter>;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

export interface EntitySearchQueryFilter {
  rootStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  rootEntity?: EntityId;
  relationType?: string;
  direction?: EntitySearchDirection;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

// tslint:disable-next-line:no-empty-interface
export interface ApiUsageStateFilter {

}

export interface AssetSearchQueryFilter extends EntitySearchQueryFilter {
  assetTypes?: string[];
}

export interface DeviceSearchQueryFilter extends EntitySearchQueryFilter {
  deviceTypes?: string[];
}

export interface EntityViewSearchQueryFilter extends EntitySearchQueryFilter {
  entityViewTypes?: string[];
}

export interface EdgeSearchQueryFilter extends EntitySearchQueryFilter {
  edgeTypes?: string[];
}

export interface SchedulerEventFilter {
  originatorStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  originator?: EntityId;
  eventType?: string;
}

export type EntityFilters =
  SingleEntityFilter &
  EntityGroupFilter &
  EntityListFilter &
  EntityNameFilter &
  EntityTypeFilter &
  EntityGroupListFilter &
  EntityGroupNameFilter &
  EntitiesByGroupNameFilter &
  StateEntityFilter &
  StateEntityOwnerFilter &
  AssetTypeFilter &
  DeviceTypeFilter &
  EntityViewFilter &
  EdgeTypeFilter &
  RelationsQueryFilter &
  AssetSearchQueryFilter &
  DeviceSearchQueryFilter &
  EntityViewSearchQueryFilter &
  EntitySearchQueryFilter &
  EdgeSearchQueryFilter &
  SchedulerEventFilter;

export interface EntityAliasFilter extends EntityFilters {
  type?: AliasFilterType;
  resolveMultiple?: boolean;
}

export interface EntityAliasInfo {
  alias: string;
  filter: EntityAliasFilter;
  [key: string]: any;
}

export interface AliasesInfo {
  datasourceAliases: {[datasourceIndex: number]: EntityAliasInfo};
  targetDeviceAliases: {[targetDeviceAliasIndex: number]: EntityAliasInfo};
}

export interface EntityAlias extends EntityAliasInfo {
  id: string;
}

export interface EntityAliases {
  [id: string]: EntityAlias;
}

export interface EntityAliasFilterResult {
  stateEntity: boolean;
  entityFilter: EntityFilter;
  entityParamName?: string;
}
