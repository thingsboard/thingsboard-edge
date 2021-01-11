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

import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Injectable } from '@angular/core';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Customer } from '@shared/models/customer.model';
import { CustomerService } from '@core/http/customer.service';
import { CustomerComponent } from '@home/pages/customer/customer.component';
import { Router } from '@angular/router';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable()
export class CustomerGroupConfigFactory implements EntityGroupStateConfigFactory<Customer> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<Customer>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private customerService: CustomerService,
              private router: Router) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<Customer>): Observable<GroupEntityTableConfig<Customer>> {
    const config = new GroupEntityTableConfig<Customer>(entityGroup, params);

    config.entityComponent = CustomerComponent;

    config.entityTitle = (customer) => customer ?
      this.utils.customTranslation(customer.title, customer.title) : '';

    config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    config.loadEntity = id => this.customerService.getCustomer(id.id);
    config.saveEntity = customer => this.customerService.saveCustomer(customer);
    config.deleteEntity = id => this.customerService.deleteCustomer(id.id);

    if (params.hierarchyView) {
      config.entityAdded = customer => {
        params.hierarchyCallbacks.customerAdded(params.nodeId, customer);
      };
      config.entityUpdated = customer => {
        params.hierarchyCallbacks.customerUpdated(customer);
      };
      config.entitiesDeleted = customerIds => {
        params.hierarchyCallbacks.customersDeleted(customerIds.map(id => id.id));
      };
    }

    config.onEntityAction = action => this.onCustomerAction(action, config, params);

    if (this.userPermissionsService.hasGenericPermission(Resource.USER_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-user-groups'),
          icon: 'account_circle',
          isEnabled: config.manageUsersEnabled,
          onAction: ($event, entity) => this.manageUsers($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-groups'),
          icon: 'supervisor_account',
          isEnabled: config.manageCustomersEnabled,
          onAction: ($event, entity) => this.manageCustomers($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-asset-groups'),
          icon: 'domain',
          isEnabled: config.manageAssetsEnabled,
          onAction: ($event, entity) => this.manageAssets($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-device-groups'),
          icon: 'devices_other',
          isEnabled: config.manageDevicesEnabled,
          onAction: ($event, entity) => this.manageDevices($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-entity-view-groups'),
          icon: 'view_quilt',
          isEnabled: config.manageEntityViewsEnabled,
          onAction: ($event, entity) => this.manageEntityViews($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-dashboard-groups'),
          icon: 'dashboard',
          isEnabled: config.manageDashboardsEnabled,
          onAction: ($event, entity) => this.manageDashboards($event, entity, config, params)
        }
      );
    }

    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  manageUsers($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
              params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.USER);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/userGroups`);
    }
  }

  manageCustomers($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
                  params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.CUSTOMER);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/customerGroups`);
    }
  }

  manageAssets($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
               params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.ASSET);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/assetGroups`);
    }
  }

  manageDevices($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
                params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.DEVICE);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/deviceGroups`);
    }
  }

  manageEntityViews($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
                    params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.ENTITY_VIEW);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/entityViewGroups`);
    }
  }

  manageDashboards($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>,
                   params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.DASHBOARD);
    } else {
      this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/dashboardGroups`);
    }
  }

  onCustomerAction(action: EntityAction<Customer>, config: GroupEntityTableConfig<Customer>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'manageUsers':
        this.manageUsers(action.event, action.entity, config, params);
        return true;
      case 'manageCustomers':
        this.manageCustomers(action.event, action.entity, config, params);
        return true;
      case 'manageAssets':
        this.manageAssets(action.event, action.entity, config, params);
        return true;
      case 'manageDevices':
        this.manageDevices(action.event, action.entity, config, params);
        return true;
      case 'manageEntityViews':
        this.manageEntityViews(action.event, action.entity, config, params);
        return true;
      case 'manageDashboards':
        this.manageDashboards(action.event, action.entity, config, params);
        return true;
    }
    return false;
  }

}
