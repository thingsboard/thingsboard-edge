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
import { Edge } from '@shared/models/edge.models';

export const ASSET_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Asset>>(EntityType.ASSET);
export const DEVICE_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Device>>(EntityType.DEVICE);
export const ENTITY_VIEW_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<EntityView>>(EntityType.ENTITY_VIEW);
export const EDGE_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Edge>>(EntityType.EDGE);
export const DASHBOARD_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Dashboard>>(EntityType.DASHBOARD);
export const USER_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<User>>(EntityType.USER);
export const CUSTOMER_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Customer>>(EntityType.CUSTOMER);

export const groupConfigFactoryTokenMap = new Map<EntityType, InjectionToken<EntityGroupStateConfigFactory<BaseData<HasId>>>>(
  [
    [EntityType.ASSET, ASSET_GROUP_CONFIG_FACTORY],
    [EntityType.DEVICE, DEVICE_GROUP_CONFIG_FACTORY],
    [EntityType.ENTITY_VIEW, ENTITY_VIEW_GROUP_CONFIG_FACTORY],
    [EntityType.EDGE, EDGE_GROUP_CONFIG_FACTORY],
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
  edgeEntitiesTitle?: string;
  edgeGroupName?: string;
}

export class GroupEntityTableConfig<T extends BaseData<HasId>> extends EntityTableConfig<T, PageLink, ShortEntityView> {

  settings = groupSettingsDefaults(this.entityGroup.type, this.entityGroup.configuration.settings);
  actionDescriptorsBySourceId: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = {};

  onGroupEntityRowClick: ($event: Event, entity: ShortEntityView) => void = null;
  onToggleEntityGroupDetails = () => {};
  onToggleEntityDetails = ($event: Event, entity: ShortEntityView) => {};

  loginAsUserEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableLoginAsUser;
  manageCredentialsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableCredentialsManagement;
  manageUsersEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableUsersManagement;
  manageCustomersEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableCustomersManagement;
  manageAssetsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableAssetsManagement;
  manageDevicesEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableDevicesManagement;
  manageEntityViewsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableEntityViewsManagement;
  manageEdgesEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableEdgesManagement;
  manageDashboardsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableDashboardsManagement;
  manageSchedulerEventsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableSchedulerEventsManagement;

  constructor(public entityGroup: EntityGroupStateInfo<T>,
              public groupParams: EntityGroupParams) {
    super(groupParams.customerId);
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
