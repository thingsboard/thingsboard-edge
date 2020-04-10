///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

    config.onEntityAction = action => this.onCustomerAction(action, config);

    if (this.userPermissionsService.hasGenericPermission(Resource.USER_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-user-groups'),
          icon: 'account_circle',
          isEnabled: config.manageUsersEnabled,
          onAction: ($event, entity) => this.manageUsers($event, entity, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-groups'),
          icon: 'supervisor_account',
          isEnabled: config.manageCustomersEnabled,
          onAction: ($event, entity) => this.manageCustomers($event, entity, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-asset-groups'),
          icon: 'domain',
          isEnabled: config.manageAssetsEnabled,
          onAction: ($event, entity) => this.manageAssets($event, entity, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-device-groups'),
          icon: 'devices_other',
          isEnabled: config.manageDevicesEnabled,
          onAction: ($event, entity) => this.manageDevices($event, entity, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-entity-view-groups'),
          icon: 'view_quilt',
          isEnabled: config.manageEntityViewsEnabled,
          onAction: ($event, entity) => this.manageEntityViews($event, entity, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-dashboard-groups'),
          icon: 'dashboard',
          isEnabled: config.manageDashboardsEnabled,
          onAction: ($event, entity) => this.manageDashboards($event, entity, config)
        }
      );
    }

    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  manageUsers($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/userGroups`);
  }

  manageCustomers($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/customerGroups`);
  }

  manageAssets($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/assetGroups`);
  }

  manageDevices($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/deviceGroups`);
  }

  manageEntityViews($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/entityViewGroups`);
  }

  manageDashboards($event: Event, customer: Customer | ShortEntityView, config: GroupEntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customerGroups/${config.entityGroup.id.id}/${customer.id.id}/dashboardGroups`);
  }

  onCustomerAction(action: EntityAction<Customer>, config: GroupEntityTableConfig<Customer>): boolean {
    switch (action.action) {
      case 'manageUsers':
        this.manageUsers(action.event, action.entity, config);
        return true;
      case 'manageCustomers':
        this.manageCustomers(action.event, action.entity, config);
        return true;
      case 'manageAssets':
        this.manageAssets(action.event, action.entity, config);
        return true;
      case 'manageDevices':
        this.manageDevices(action.event, action.entity, config);
        return true;
      case 'manageEntityViews':
        this.manageEntityViews(action.event, action.entity, config);
        return true;
      case 'manageDashboards':
        this.manageDashboards(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
