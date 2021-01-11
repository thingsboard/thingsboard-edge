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

import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitySearchDirection, EntityTypeFilter } from '@shared/models/relation.models';
import { EntityInfo } from './entity.models';
import { EntityFilter } from '@shared/models/query/query.models';

export enum AliasFilterType {
  singleEntity = 'singleEntity',
  entityGroup = 'entityGroup',
  entityList = 'entityList',
  entityName = 'entityName',
  entityGroupList = 'entityGroupList',
  entityGroupName = 'entityGroupName',
  entitiesByGroupName = 'entitiesByGroupName',
  stateEntity = 'stateEntity',
  stateEntityOwner = 'stateEntityOwner',
  assetType = 'assetType',
  deviceType = 'deviceType',
  entityViewType = 'entityViewType',
  apiUsageState = 'apiUsageState',
  relationsQuery = 'relationsQuery',
  assetSearchQuery = 'assetSearchQuery',
  deviceSearchQuery = 'deviceSearchQuery',
  entityViewSearchQuery = 'entityViewSearchQuery'
}

export const aliasFilterTypeTranslationMap = new Map<AliasFilterType, string>(
  [
    [ AliasFilterType.singleEntity, 'alias.filter-type-single-entity' ],
    [ AliasFilterType.entityGroup, 'alias.filter-type-entity-group' ],
    [ AliasFilterType.entityList, 'alias.filter-type-entity-list' ],
    [ AliasFilterType.entityName, 'alias.filter-type-entity-name' ],
    [ AliasFilterType.entityGroupList, 'alias.filter-type-entity-group-list' ],
    [ AliasFilterType.entityGroupName, 'alias.filter-type-entity-group-name' ],
    [ AliasFilterType.entitiesByGroupName, 'alias.filter-type-entities-by-group-name' ],
    [ AliasFilterType.stateEntity, 'alias.filter-type-state-entity' ],
    [ AliasFilterType.stateEntityOwner, 'alias.filter-type-state-entity-owner' ],
    [ AliasFilterType.assetType, 'alias.filter-type-asset-type' ],
    [ AliasFilterType.deviceType, 'alias.filter-type-device-type' ],
    [ AliasFilterType.entityViewType, 'alias.filter-type-entity-view-type' ],
    [ AliasFilterType.apiUsageState, 'alias.filter-type-apiUsageState' ],
    [ AliasFilterType.relationsQuery, 'alias.filter-type-relations-query' ],
    [ AliasFilterType.assetSearchQuery, 'alias.filter-type-asset-search-query' ],
    [ AliasFilterType.deviceSearchQuery, 'alias.filter-type-device-search-query' ],
    [ AliasFilterType.entityViewSearchQuery, 'alias.filter-type-entity-view-search-query' ]
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

export interface RelationsQueryFilter {
  rootStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  rootEntity?: EntityId;
  direction?: EntitySearchDirection;
  filters?: Array<EntityTypeFilter>;
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

export type EntityFilters =
  SingleEntityFilter &
  EntityGroupFilter &
  EntityListFilter &
  EntityNameFilter &
  EntityGroupListFilter &
  EntityGroupNameFilter &
  EntitiesByGroupNameFilter &
  StateEntityFilter &
  StateEntityOwnerFilter &
  AssetTypeFilter &
  DeviceTypeFilter &
  EntityViewFilter &
  RelationsQueryFilter &
  AssetSearchQueryFilter &
  DeviceSearchQueryFilter &
  EntityViewSearchQueryFilter &
  EntitySearchQueryFilter;

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
