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

import { Injectable, Renderer2 } from '@angular/core';

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn, defaultEntityTablePermissions,
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
import { select, Store } from '@ngrx/store';
import { getCurrentAuthUser, selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { MatDialog } from '@angular/material/dialog';
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
import { TbPopoverService } from '@shared/components/popover.service';
import {
  DashboardsTableFilter,
  DashboardsTableFilterComponent
} from '@home/pages/dashboard/dashboards-table-filter.component';
import { MatButton } from '@angular/material/button';

@Injectable()
export class DashboardsTableConfigResolver implements Resolve<EntityTableConfig<DashboardInfo | Dashboard>> {

  private readonly config: EntityTableConfig<DashboardInfo | Dashboard> = new EntityTableConfig<DashboardInfo | Dashboard>();

  constructor(private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private dashboardService: DashboardService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private popoverService: TbPopoverService,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.DASHBOARD;
    this.config.entityComponent = DashboardFormComponent;
    this.config.entityTabsComponent = DashboardTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DASHBOARD);
    this.config.entityResources = entityTypeResources.get(EntityType.DASHBOARD);

    this.config.entityTitle = (dashboard) => dashboard ?
      this.utils.customTranslation(dashboard.title, dashboard.title) : '';

    this.config.rowPointer = true;

    this.config.deleteEntityTitle = dashboard =>
      this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    this.config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    this.config.loadEntity = id => this.dashboardService.getDashboard(id.id);
    this.config.saveEntity = dashboard => this.dashboardService.saveDashboard(dashboard as Dashboard);
    this.config.onEntityAction = action => this.onDashboardAction(action);
    this.config.headerActionDescriptors.push({
      name: this.translate.instant('filter.filter'),
      icon: 'filter_list',
      onAction: ($event, headerButton) => {
        this.toggleFilter($event, headerButton);
      },
      isEnabled: () => true
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DashboardInfo | Dashboard>> {
    const routeParams = route.params;
    this.config.componentsData = {
      dashboardScope: route.data.dashboardsType,
      customerId: routeParams.customerId,
      includeCustomers: true,
      dashboardsTableFilterChanged: (filter: DashboardsTableFilter) => {
        this.config.componentsData.includeCustomers = filter.includeCustomers;
        const authUser = getCurrentAuthUser(this.store);
        this.config.columns = this.configureColumns(authUser);
        this.config.getTable().columnsUpdated();
        this.config.getTable().resetSortAndFilter(true);
      }
    };
    if (this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.READ) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ)) {
      this.config.handleRowClick = ($event, dashboard) => {
        if (this.config.isDetailsOpen()) {
          this.config.toggleEntityDetails($event, dashboard);
        } else {
          this.openDashboard($event, dashboard);
        }
        return true;
      };
    } else {
      this.config.handleRowClick = () => false;
    }
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      /* tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.dashboardsType === 'edge') {
            this.config.componentsData.dashboardScope = 'edge_customer_user';
          } else {
            this.config.componentsData.dashboardScope = 'customer_user';
          }
          this.config.componentsData.customerId = authUser.customerId;
        }
      }), */
      mergeMap((authUser) =>
          (this.config.componentsData.customerId ?
          this.customerService.getCustomer(this.config.componentsData.customerId) : of(null as Customer)).pipe(
            map((customer) => [authUser, customer])
          )
      ),
      map(([authUser, parentCustomer]: [AuthUser, Customer]) => {
        if (parentCustomer) {
          this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('dashboard.dashboards');
        } else {
          this.config.tableTitle = this.translate.instant('dashboard.dashboards');
        }
        this.config.columns = this.configureColumns(authUser);
        this.configureEntityFunctions();
        this.config.cellActionDescriptors = this.configureCellActions();
        this.config.groupActionDescriptors = this.configureGroupActions();
        this.config.addActionDescriptors = this.configureAddActions();
        return this.config;
      })
    );
  }

  toggleFilter($event: MouseEvent, headerButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = headerButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const filter: DashboardsTableFilter = {
        includeCustomers: this.config.componentsData.includeCustomers
      };
      this.popoverService.displayPopover(trigger, this.config.getTable().renderer,
        this.config.getTable().viewContainerRef, DashboardsTableFilterComponent, 'bottomRight', true, null,
        {
          filter,
          tableConfig: this.config
        }, {}, {}, {}, true);
    }
  }

  configureColumns(authUser: AuthUser): Array<EntityTableColumn<DashboardInfo>> {
    const columns: Array<EntityTableColumn<DashboardInfo>> = [
      new DateEntityTableColumn<DashboardInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DashboardInfo>('title', 'dashboard.title',
        this.config.componentsData.includeCustomers ? '50%' : '100%', this.config.entityTitle)
    ];
    if (this.config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || this.config.componentsData.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<DashboardInfo>('ownerName', title, '50%'));
    }
    return columns;
  }

  configureEntityFunctions(): void {
    if (this.config.componentsData.customerId) {
      this.config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getCustomerDashboards(this.config.componentsData.includeCustomers,
          this.config.componentsData.customerId, pageLink);
    } else {
      this.config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getAllDashboards(this.config.componentsData.includeCustomers, pageLink);
    }
    this.config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);
  }

  configureCellActions(): Array<CellActionDescriptor<DashboardInfo>> {
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
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  configureGroupActions(): Array<GroupActionDescriptor<DashboardInfo>> {
    const actions: Array<GroupActionDescriptor<DashboardInfo>> = [];
    return actions;
  }

  configureAddActions(): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('dashboard.create-new-dashboard'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('dashboard.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importDashboard($event)
      }
    );
    return actions;
  }

  openDashboard($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([dashboard.id.id], {relativeTo: this.config.getTable().route});
    this.router.navigateByUrl(url);
  }

  importDashboard($event: Event) {
    const customerId = this.config.componentsData.customerId ? new CustomerId(this.config.componentsData.customerId) : null;
    this.importExport.importDashboard(customerId).subscribe(
      (dashboard) => {
        if (dashboard) {
          this.config.updateData();
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

  onDashboardAction(action: EntityAction<DashboardInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
    }
    return false;
  }

}
