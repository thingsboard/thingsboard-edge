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

import { Injectable } from '@angular/core';

import { Resolve, Router } from '@angular/router';

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Customer } from '@app/shared/models/customer.model';
import { CustomerService } from '@app/core/http/customer.service';
import { CustomerTabsComponent } from '@home/pages/customer/customer-tabs.component';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';

@Injectable()
export class CustomersTableConfigResolver implements Resolve<EntityTableConfig<Customer>> {

  private readonly config: EntityTableConfig<Customer> = new EntityTableConfig<Customer>();

  constructor(private customerService: CustomerService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private store: Store<AppState>,
              private utils: UtilsService) {

    this.config.entityType = EntityType.CUSTOMER;
    // this.config.entityComponent = CustomerComponent;
    this.config.entityTabsComponent = CustomerTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CUSTOMER);
    this.config.entityResources = entityTypeResources.get(EntityType.CUSTOMER);
    const authState = getCurrentAuthState(this.store);

    this.config.entityTitle = (customer) => customer ?
      this.utils.customTranslation(customer.title, customer.title) : '';

    this.config.columns.push(
      new DateEntityTableColumn<Customer>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Customer>('title', 'customer.title', '25%', this.config.entityTitle),
      new EntityTableColumn<Customer>('email', 'contact.email', '25%'),
      new EntityTableColumn<Customer>('country', 'contact.country', '25%'),
      new EntityTableColumn<Customer>('city', 'contact.city', '25%')
    );
    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('customer.manage-customer-users'),
        icon: 'account_circle',
        isEnabled: (customer) => !customer.additionalInfo || !customer.additionalInfo.isPublic,
        onAction: ($event, entity) => this.manageCustomerUsers($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-assets'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
          ? this.translate.instant('customer.manage-public-assets')
          : this.translate.instant('customer.manage-customer-assets');
        },
        icon: 'domain',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerAssets($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-devices'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-devices')
            : this.translate.instant('customer.manage-customer-devices');
        },
        icon: 'devices_other',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerDevices($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-dashboards'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-dashboards')
            : this.translate.instant('customer.manage-customer-dashboards');
        },
        icon: 'dashboard',
        isEnabled: (customer) => true,
        onAction: ($event, entity) => this.manageCustomerDashboards($event, entity)
      });
    if (authState.edgesSupportEnabled) {
      this.config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-edges'),
          nameFunction: (customer) => {
            return customer.additionalInfo && customer.additionalInfo.isPublic
              ? this.translate.instant('customer.manage-public-edges')
              : this.translate.instant('customer.manage-customer-edges');
          },
          icon: 'router',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerEdges($event, entity)
        }
      );
    }

    this.config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    this.config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    this.config.entitiesFetchFunction = pageLink => this.customerService.getCustomers(pageLink);
    this.config.loadEntity = id => this.customerService.getCustomer(id.id);
    this.config.saveEntity = customer => this.customerService.saveCustomer(customer);
    this.config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    this.config.onEntityAction = action => this.onCustomerAction(action, this.config);
    this.config.deleteEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.entitySelectionEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.detailsReadonly = (customer) => customer && customer.additionalInfo && customer.additionalInfo.isPublic;
  }

  resolve(): EntityTableConfig<Customer> {
    this.config.tableTitle = this.translate.instant('customer.customers');

    return this.config;
  }

  private openCustomer($event: Event, customer: Customer, config: EntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([customer.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  manageCustomerUsers($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/users`);
  }

  manageCustomerAssets($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/assets`);
  }

  manageCustomerDevices($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/devices`);
  }

  manageCustomerDashboards($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/dashboards`);
  }

  manageCustomerEdges($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/edgeInstances`);
  }

  onCustomerAction(action: EntityAction<Customer>, config: EntityTableConfig<Customer>): boolean {
    switch (action.action) {
      case 'open':
        this.openCustomer(action.event, action.entity, config);
        return true;
      case 'manageUsers':
        this.manageCustomerUsers(action.event, action.entity);
        return true;
      case 'manageAssets':
        this.manageCustomerAssets(action.event, action.entity);
        return true;
      case 'manageDevices':
        this.manageCustomerDevices(action.event, action.entity);
        return true;
      case 'manageDashboards':
        this.manageCustomerDashboards(action.event, action.entity);
        return true;
      case 'manageEdges':
        this.manageCustomerEdges(action.event, action.entity);
        return true;
    }
    return false;
  }

}
