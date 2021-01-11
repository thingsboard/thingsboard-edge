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

import { BaseData, HasId } from '@shared/models/base-data';
import { EntityBooleanFunction, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  EntityGroupDetailsMode,
  EntityGroupInfo,
  EntityGroupParams,
  groupSettingsDefaults,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { GroupEntityTableHeaderComponent } from '@home/components/group/group-entity-table-header.component';
import { InjectionToken } from '@angular/core';
import { Device } from '@shared/models/device.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { WidgetActionDescriptor } from '@shared/models/widget.models';
import { Asset } from '@shared/models/asset.models';
import { EntityView } from '@shared/models/entity-view.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { User } from '@shared/models/user.model';
import { Customer } from '@shared/models/customer.model';

export const ASSET_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Asset>>(EntityType.ASSET);
export const DEVICE_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Device>>(EntityType.DEVICE);
export const ENTITY_VIEW_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<EntityView>>(EntityType.ENTITY_VIEW);
export const DASHBOARD_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Dashboard>>(EntityType.DASHBOARD);
export const USER_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<User>>(EntityType.USER);
export const CUSTOMER_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Customer>>(EntityType.CUSTOMER);

export const groupConfigFactoryTokenMap = new Map<EntityType, InjectionToken<EntityGroupStateConfigFactory<BaseData<HasId>>>>(
  [
    [EntityType.ASSET, ASSET_GROUP_CONFIG_FACTORY],
    [EntityType.DEVICE, DEVICE_GROUP_CONFIG_FACTORY],
    [EntityType.ENTITY_VIEW, ENTITY_VIEW_GROUP_CONFIG_FACTORY],
    [EntityType.DASHBOARD, DASHBOARD_GROUP_CONFIG_FACTORY],
    [EntityType.USER, USER_GROUP_CONFIG_FACTORY],
    [EntityType.CUSTOMER, CUSTOMER_GROUP_CONFIG_FACTORY]
  ]
);

export interface EntityGroupStateConfigFactory<T extends BaseData<HasId>> {
  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<T>): Observable<GroupEntityTableConfig<T>>;
}

export interface EntityGroupStateInfo<T extends BaseData<HasId>> extends EntityGroupInfo {
  // origEntityGroup?: EntityGroupInfo;
  customerGroupsTitle?: string;
  parentEntityGroup?: EntityGroupInfo;
  entityGroupConfig?: GroupEntityTableConfig<T>;
}

export class GroupEntityTableConfig<T extends BaseData<HasId>> extends EntityTableConfig<T, PageLink, ShortEntityView> {

  customerId: string;

  settings = groupSettingsDefaults(this.entityGroup.type, this.entityGroup.configuration.settings);
  actionDescriptorsBySourceId: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = {};

  onToggleEntityGroupDetails = () => {};
  onToggleEntityDetails = ($event: Event, entity: ShortEntityView) => {};

  assignmentEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableAssignment;
  manageCredentialsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableCredentialsManagement;
  manageUsersEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableUsersManagement;
  manageCustomersEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableCustomersManagement;
  manageAssetsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableAssetsManagement;
  manageDevicesEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableDevicesManagement;
  manageEntityViewsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableEntityViewsManagement;
  manageDashboardsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableDashboardsManagement;

  constructor(public entityGroup: EntityGroupStateInfo<T>,
              public groupParams: EntityGroupParams) {
    super();
    this.customerId = groupParams.customerId;
    this.entityType = entityGroup.type;
    this.entityTranslations = entityTypeTranslations.get(this.entityType);
    this.entityResources = entityTypeResources.get(this.entityType);
    this.entityTabsComponent = GroupEntityTabsComponent;
    this.headerComponent = GroupEntityTableHeaderComponent;
    this.entitiesDeleteEnabled = this.settings.enableDelete;
    this.searchEnabled = this.settings.enableSearch;
    this.selectionEnabled = this.settings.enableSelection;
    this.displayPagination = this.settings.displayPagination;
    this.defaultPageSize = this.settings.defaultPageSize;
    this.detailsPanelEnabled = this.settings.detailsMode !== EntityGroupDetailsMode.disabled;
    this.deleteEnabled = () => this.settings.enableDelete;
  }
}
