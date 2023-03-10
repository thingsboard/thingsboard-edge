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

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Observable, of } from 'rxjs';
import { Store } from '@ngrx/store';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { DialogService } from '@core/services/dialog.service';
import { Dashboard, DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@app/core/http/dashboard.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { DashboardFormComponent } from '@home/pages/dashboard/dashboard-form.component';
import { DashboardTabsComponent } from '@home/pages/dashboard/dashboard-tabs.component';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { AuthUser } from '@shared/models/user.model';
import { DashboardTableHeaderComponent } from '@home/pages/dashboard/dashboard-table-header.component';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';

@Injectable()
export class DashboardsTableConfigResolver implements Resolve<EntityTableConfig<DashboardInfo | Dashboard>> {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<DashboardInfo | Dashboard>,
              private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private dashboardService: DashboardService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DashboardInfo | Dashboard>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<DashboardInfo | Dashboard>(groupParams.customerId);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    if (this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.READ) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ)) {
      config.handleRowClick = ($event, dashboard) => {
        if (config.isDetailsOpen()) {
          config.toggleEntityDetails($event, dashboard);
        } else {
          this.openDashboard($event, dashboard, config);
        }
        return true;
      };
    } else {
      config.handleRowClick = () => false;
    }
    return (config.customerId ?
      this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('dashboard.dashboards');
        } else {
          config.tableTitle = this.translate.instant('dashboard.dashboards');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<DashboardInfo | Dashboard>) {
    config.entityType = EntityType.DASHBOARD;
    config.entityComponent = DashboardFormComponent;
    config.entityTabsComponent = DashboardTabsComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.DASHBOARD);
    config.entityResources = entityTypeResources.get(EntityType.DASHBOARD);

    config.entityTitle = (dashboard) => dashboard ?
      this.utils.customTranslation(dashboard.title, dashboard.title) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = dashboard =>
      this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    config.loadEntity = id => this.dashboardService.getDashboard(id.id);
    config.saveEntity = dashboard => this.dashboardService.saveDashboard(dashboard as Dashboard);
    config.onEntityAction = action => this.onDashboardAction(action, config);
    config.headerComponent = DashboardTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<DashboardInfo | Dashboard>): Array<EntityTableColumn<DashboardInfo>> {
    const columns: Array<EntityTableColumn<DashboardInfo>> = [
      new DateEntityTableColumn<DashboardInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DashboardInfo>('title', 'dashboard.title',
        config.componentsData.includeCustomers ? '50%' : '100%', config.entityTitle)
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<DashboardInfo>('ownerName', title, '50%'));
    }
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<DashboardInfo | Dashboard>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getCustomerDashboards(config.componentsData.includeCustomers,
          config.customerId, pageLink);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getAllDashboards(config.componentsData.includeCustomers, pageLink);
    }
    config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);
  }

  configureCellActions(config: EntityTableConfig<DashboardInfo | Dashboard>): Array<CellActionDescriptor<DashboardInfo>> {
    const actions: Array<CellActionDescriptor<DashboardInfo>> = [];
    actions.push(
      {
        name: this.translate.instant('dashboard.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportDashboard($event, entity)
      },
    );
    actions.push(
      {
        name: this.translate.instant('dashboard.dashboard-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<DashboardInfo | Dashboard>): Array<GroupActionDescriptor<DashboardInfo>> {
    const actions: Array<GroupActionDescriptor<DashboardInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<DashboardInfo | Dashboard>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('dashboard.create-new-dashboard'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('dashboard.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importDashboard($event, config)
      }
    );
    return actions;
  }

  openDashboard($event: Event, dashboard: DashboardInfo, config: EntityTableConfig<DashboardInfo | Dashboard>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([dashboard.id.id], {relativeTo: config.getTable().route});
    this.router.navigateByUrl(url);
  }

  importDashboard($event: Event, config: EntityTableConfig<DashboardInfo | Dashboard>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.importExport.importDashboard(customerId).subscribe(
      (dashboard) => {
        if (dashboard) {
          config.updateData();
        }
      }
    );
  }

  exportDashboard($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportDashboard(dashboard.id.id);
  }

  onDashboardAction(action: EntityAction<DashboardInfo>, config: EntityTableConfig<DashboardInfo | Dashboard>): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity, config);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
    }
    return false;
  }

}
