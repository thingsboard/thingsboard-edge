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
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  GroupChipsEntityTableColumn,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Observable, of } from 'rxjs';
import { Store } from '@ngrx/store';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { EntityView, EntityViewInfo } from '@app/shared/models/entity-view.models';
import { EntityViewService } from '@core/http/entity-view.service';
import { EntityViewTableHeaderComponent } from '@modules/home/pages/entity-view/entity-view-table-header.component';
import { EdgeService } from '@core/http/edge.service';
import { UtilsService } from '@core/services/utils.service';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { EntityViewComponent } from '@home/pages/entity-view/entity-view.component';
import { AuthUser } from '@shared/models/user.model';

@Injectable()
export class EntityViewsTableConfigResolver implements Resolve<EntityTableConfig<EntityViewInfo | EntityView>> {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<EntityViewInfo | EntityView>,
              private store: Store<AppState>,
              private broadcast: BroadcastService,
              private entityViewService: EntityViewService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EntityViewInfo | EntityView>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<EntityViewInfo | EntityView>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      entityViewType: '',
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    return (config.customerId ?
      this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('entity-view.entity-views');
        } else {
          config.tableTitle = this.translate.instant('entity-view.entity-views');
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

  configDefaults(config: EntityTableConfig<EntityViewInfo | EntityView>) {
    config.entityType = EntityType.ENTITY_VIEW;
    config.entityComponent = EntityViewComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<EntityView>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_VIEW);
    config.entityResources = entityTypeResources.get(EntityType.ENTITY_VIEW);

    config.addDialogStyle = {maxWidth: '800px'};

    config.entityTitle = (entityView) => entityView ?
      this.utils.customTranslation(entityView.name, entityView.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = entityView =>
      this.translate.instant('entity-view.delete-entity-view-title', {entityViewName: entityView.name});
    config.deleteEntityContent = () => this.translate.instant('entity-view.delete-entity-view-text');
    config.deleteEntitiesTitle = count => this.translate.instant('entity-view.delete-entity-views-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('entity-view.delete-entity-views-text');

    config.loadEntity = id => this.entityViewService.getEntityView(id.id);
    config.saveEntity = entityView => this.entityViewService.saveEntityView(entityView).pipe(
        tap(() => {
          this.broadcast.broadcast('entityViewSaved');
        }));
    config.onEntityAction = action => this.onEntityViewAction(action, config);
    config.headerComponent = EntityViewTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<EntityViewInfo | EntityView>): Array<EntityColumn<EntityViewInfo>> {
    const columns: Array<EntityColumn<EntityViewInfo>> = [
      new DateEntityTableColumn<EntityViewInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityViewInfo>('name', 'entity-view.name', '25%', config.entityTitle),
      new EntityTableColumn<EntityViewInfo>('type', 'entity-view.entity-view-type', '20%'),
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<EntityViewInfo>('ownerName', title, '25%'));
    }
    columns.push(
      new GroupChipsEntityTableColumn<EntityViewInfo>( 'groups', 'entity.groups', '30%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<EntityViewInfo | EntityView>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getCustomerEntityViewInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink, config.componentsData.entityViewType);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getAllEntityViewInfos(config.componentsData.includeCustomers, pageLink,
          config.componentsData.entityViewType);
    }
    config.deleteEntity = id => this.entityViewService.deleteEntityView(id.id);
  }

  configureCellActions(config: EntityTableConfig<EntityViewInfo | EntityView>): Array<CellActionDescriptor<EntityViewInfo>> {
    const actions: Array<CellActionDescriptor<EntityViewInfo>> = [];
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<EntityViewInfo | EntityView>): Array<GroupActionDescriptor<EntityViewInfo>> {
    const actions: Array<GroupActionDescriptor<EntityViewInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<EntityViewInfo | EntityView>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    return actions;
  }

  private openEntityView($event: Event, entityView: EntityViewInfo, config: EntityTableConfig<EntityViewInfo | EntityView>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([entityView.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  onEntityViewAction(action: EntityAction<EntityView>, config: EntityTableConfig<EntityViewInfo | EntityView>): boolean {
    switch (action.action) {
      case 'open':
        this.openEntityView(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
