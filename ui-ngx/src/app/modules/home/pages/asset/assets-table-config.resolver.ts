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
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Asset } from '@app/shared/models/asset.models';
import { AssetService } from '@app/core/http/asset.service';
import { AssetTableHeaderComponent } from '@modules/home/pages/asset/asset-table-header.component';
import { AssetTabsComponent } from '@home/pages/asset/asset-tabs.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { UtilsService } from '@core/services/utils.service';

@Injectable()
export class AssetsTableConfigResolver implements Resolve<EntityTableConfig<Asset>> {

  private readonly config: EntityTableConfig<Asset> = new EntityTableConfig<Asset>();

  private customerId: string;

  constructor(private store: Store<AppState>,
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

    this.config.entityType = EntityType.ASSET;
    // this.config.entityComponent = AssetComponent;
    this.config.entityTabsComponent = AssetTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ASSET);
    this.config.entityResources = entityTypeResources.get(EntityType.ASSET);

    this.config.entityTitle = (asset) => asset ?
      this.utils.customTranslation(asset.name, asset.name) : '';

    this.config.deleteEntityTitle = asset => this.translate.instant('asset.delete-asset-title', { assetName: asset.name });
    this.config.deleteEntityContent = () => this.translate.instant('asset.delete-asset-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('asset.delete-assets-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('asset.delete-assets-text');

    this.config.loadEntity = id => this.assetService.getAsset(id.id);
    this.config.saveEntity = asset => {
      return this.assetService.saveAsset(asset).pipe(
        tap(() => {
          this.broadcast.broadcast('assetSaved');
        }));
    };
    this.config.onEntityAction = action => this.onAssetAction(action, this.config);
    this.config.detailsReadonly = () => (this.config.componentsData.assetScope === 'customer_user' ||
      this.config.componentsData.assetScope === 'edge_customer_user');

    this.config.headerComponent = AssetTableHeaderComponent;

  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<Asset>> {
    const routeParams = route.params;
    this.config.componentsData = {
      assetScope: route.data.assetsType,
      assetType: ''
    };
    this.customerId = routeParams.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.assetsType === 'edge') {
            this.config.componentsData.assetScope = 'edge_customer_user';
          } else {
            this.config.componentsData.assetScope = 'customer_user';
          }
          this.customerId = authUser.customerId;
        }
      }),
      mergeMap(() =>
        this.customerId ? this.customerService.getCustomer(this.customerId) : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          if (parentCustomer.additionalInfo && parentCustomer.additionalInfo.isPublic) {
            this.config.tableTitle = this.translate.instant('customer.public-assets');
          } else {
            this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('asset.assets');
          }
        } else {
          this.config.tableTitle = this.translate.instant('asset.assets');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.assetScope);
        this.configureEntityFunctions(this.config.componentsData.assetScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.assetScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.assetScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.assetScope);
        this.config.addEnabled = !(this.config.componentsData.assetScope === 'customer_user' || this.config.componentsData.assetScope === 'edge_customer_user');
        this.config.entitiesDeleteEnabled = this.config.componentsData.assetScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.assetScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(assetScope: string): Array<EntityTableColumn<Asset>> {
    const columns: Array<EntityTableColumn<Asset>> = [
      new DateEntityTableColumn<Asset>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Asset>('name', 'asset.name', '25%', this.config.entityTitle),
      new EntityTableColumn<Asset>('type', 'asset.asset-type', '25%'),
      new EntityTableColumn<Asset>('label', 'asset.label', '25%'),
    ];
    /*if (assetScope === 'tenant') {
      columns.push(
        new EntityTableColumn<Asset>('customerTitle', 'customer.customer', '25%'),
        new EntityTableColumn<Asset>('customerIsPublic', 'asset.public', '60px',
          entity => {
            return checkBoxCell(entity.customerIsPublic);
          }, () => ({}), false),
      );
    }*/
    return columns;
  }

  configureEntityFunctions(assetScope: string): void {
    if (assetScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getTenantAssets(pageLink, this.config.componentsData.assetType);
      this.config.deleteEntity = id => this.assetService.deleteAsset(id.id);
    }
    /* else if (assetScope === 'edge' || assetScope === 'edge_customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getEdgeAssets(this.config.componentsData.edgeId, pageLink, this.config.componentsData.assetType);
    }*/
    else {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getCustomerAssets(this.customerId, pageLink, this.config.componentsData.assetType);
    //  this.config.deleteEntity = id => this.assetService.unassignAssetFromCustomer(id.id);
    }
  }

  configureCellActions(assetScope: string): Array<CellActionDescriptor<Asset>> {
    const actions: Array<CellActionDescriptor<Asset>> = [];
    /*if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.make-public'),
          icon: 'share',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('asset.assign-to-customer'),
          icon: 'assignment_ind',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
        },
        {
          name: this.translate.instant('asset.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('asset.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('asset.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (assetScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    */
    return actions;
  }

  configureGroupActions(assetScope: string): Array<GroupActionDescriptor<Asset>> {
    const actions: Array<GroupActionDescriptor<Asset>> = [];
    /*if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.assign-assets'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => this.assignToCustomer($event, entities.map((entity) => entity.id))
        }
      );
    }
    if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-assets'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignAssetsFromCustomer($event, entities)
        }
      );
    }
    if (assetScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-assets-from-edge'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignAssetsFromEdge($event, entities)
        }
      );
    }
    */
    return actions;
  }

  configureAddActions(assetScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.add-asset-text'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.getTable().addEntity($event)
        },
        {
          name: this.translate.instant('asset.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importAssets($event)
        }
      );
    }
    /*if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.assign-new-asset'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addAssetsToCustomer($event)
        }
      );
    }*/
    return actions;
  }

  importAssets($event: Event) {
    /*this.homeDialogs.importEntities(EntityType.ASSET).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        this.config.updateData();
      }
    });*/
  }

/*  private openAsset($event: Event, asset: Asset, config: EntityTableConfig<AssetInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([asset.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  addAssetsToCustomer($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToCustomerDialogComponent, AddEntitiesToCustomerDialogData,
      boolean>(AddEntitiesToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: this.customerId,
        entityType: EntityType.ASSET
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  makePublic($event: Event, asset: Asset) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.make-public-asset-title', {assetName: asset.name}),
      this.translate.instant('asset.make-public-asset-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.assetService.makeAssetPublic(asset.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  assignToCustomer($event: Event, assetIds: Array<AssetId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData,
      boolean>(AssignToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityIds: assetIds,
        entityType: EntityType.ASSET
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  unassignFromCustomer($event: Event, asset: AssetInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const isPublic = asset.customerIsPublic;
    let title;
    let content;
    if (isPublic) {
      title = this.translate.instant('asset.make-private-asset-title', {assetName: asset.name});
      content = this.translate.instant('asset.make-private-asset-text');
    } else {
      title = this.translate.instant('asset.unassign-asset-title', {assetName: asset.name});
      content = this.translate.instant('asset.unassign-asset-text');
    }
    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.assetService.unassignAssetFromCustomer(asset.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.assetScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  unassignAssetsFromCustomer($event: Event, assets: Array<AssetInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.unassign-assets-title', {count: assets.length}),
      this.translate.instant('asset.unassign-assets-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          assets.forEach(
            (asset) => {
              tasks.push(this.assetService.unassignAssetFromCustomer(asset.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }*/

  onAssetAction(action: EntityAction<Asset>, config: EntityTableConfig<Asset>): boolean {
    switch (action.action) {
      /*case 'open':
        this.openAsset(action.event, action.entity, config);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;*/
    }
    return false;
  }
}
