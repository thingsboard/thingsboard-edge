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
  DateEntityTableColumn, EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor, GroupChipsEntityTableColumn,
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
import { Asset, AssetInfo } from '@app/shared/models/asset.models';
import { AssetService } from '@app/core/http/asset.service';
import { AssetTableHeaderComponent } from '@modules/home/pages/asset/asset-table-header.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { UtilsService } from '@core/services/utils.service';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { AssetComponent } from '@home/pages/asset/asset.component';
import { AuthUser } from '@shared/models/user.model';
import { CustomerId } from '@shared/models/id/customer-id';
import { DeviceInfo } from '@shared/models/device.models';

@Injectable()
export class AssetsTableConfigResolver implements Resolve<EntityTableConfig<AssetInfo | Asset>> {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<AssetInfo | Asset>,
              private store: Store<AppState>,
              private broadcast: BroadcastService,
              private assetService: AssetService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AssetInfo | Asset>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<AssetInfo | Asset>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      assetProfileId: null,
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
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('asset.assets');
        } else {
          config.tableTitle = this.translate.instant('asset.assets');
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

  configDefaults(config: EntityTableConfig<AssetInfo | Asset>) {
    config.entityType = EntityType.ASSET;
    config.entityComponent = AssetComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<Asset>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.ASSET);
    config.entityResources = entityTypeResources.get(EntityType.ASSET);

    config.entityTitle = (asset) => asset ?
      this.utils.customTranslation(asset.name, asset.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = asset => this.translate.instant('asset.delete-asset-title', { assetName: asset.name });
    config.deleteEntityContent = () => this.translate.instant('asset.delete-asset-text');
    config.deleteEntitiesTitle = count => this.translate.instant('asset.delete-assets-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('asset.delete-assets-text');

    config.loadEntity = id => this.assetService.getAsset(id.id);
    config.saveEntity = asset => this.assetService.saveAsset(asset).pipe(
      tap(() => {
        this.broadcast.broadcast('assetSaved');
      }));
    config.onEntityAction = action => this.onAssetAction(action, config);
    config.headerComponent = AssetTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<AssetInfo | Asset>): Array<EntityColumn<AssetInfo>> {
    const columns: Array<EntityColumn<AssetInfo>> = [
      new DateEntityTableColumn<AssetInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AssetInfo>('name', 'asset.name', '20%', config.entityTitle),
      new EntityTableColumn<AssetInfo>('type', 'asset.asset-type', '20%'),
      new EntityTableColumn<AssetInfo>('label', 'asset.label', '15%'),
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<AssetInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new GroupChipsEntityTableColumn<AssetInfo>( 'groups', 'entity.groups', '25%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<AssetInfo | Asset>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.assetService.getCustomerAssetInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink, config.componentsData.assetProfileId !== null ?
            config.componentsData.assetProfileId.id : '');
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.assetService.getAllAssetInfos(config.componentsData.includeCustomers, pageLink,
          config.componentsData.assetProfileId !== null ?
            config.componentsData.assetProfileId.id : '');
    }
    config.deleteEntity = id => this.assetService.deleteAsset(id.id);
  }

  configureCellActions(config: EntityTableConfig<AssetInfo | Asset>): Array<CellActionDescriptor<AssetInfo>> {
    const actions: Array<CellActionDescriptor<AssetInfo>> = [];
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<AssetInfo | Asset>): Array<GroupActionDescriptor<AssetInfo>> {
    const actions: Array<GroupActionDescriptor<AssetInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<AssetInfo | Asset>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('asset.add-asset-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('asset.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importAssets($event, config)
      }
    );
    return actions;
  }

  private openAsset($event: Event, asset: AssetInfo, config: EntityTableConfig<AssetInfo | Asset>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([asset.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  importAssets($event: Event, config: EntityTableConfig<AssetInfo | Asset>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.homeDialogs.importEntities(customerId, EntityType.ASSET, null).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        config.updateData();
      }
    });
  }

  onAssetAction(action: EntityAction<Asset>, config: EntityTableConfig<AssetInfo | Asset>): boolean {
    switch (action.action) {
      case 'open':
        this.openAsset(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
