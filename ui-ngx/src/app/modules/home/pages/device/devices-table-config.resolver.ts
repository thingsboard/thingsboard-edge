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

import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
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
import { DeviceCredentials, DeviceInfo, DeviceInfoFilter, DeviceInfoQuery } from '@app/shared/models/device.models';
import { Observable, of, Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { DeviceService } from '@app/core/http/device.service';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { DeviceTableHeaderComponent } from '@modules/home/pages/device/device-table-header.component';
import { MatDialog } from '@angular/material/dialog';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@modules/home/pages/device/device-credentials-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, isDefined, isDefinedAndNotNull } from '@core/utils';
import { DeviceComponent } from './device.component';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AuthUser } from '@shared/models/user.model';
import { Operation, Resource } from '@shared/models/security.models';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  DeviceWizardDialogComponent,
  DeviceWizardDialogData
} from '@home/components/wizard/device-wizard-dialog.component';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';

interface DevicePageQueryParams extends PageQueryParam {
  deviceProfileId?: string;
  active?: boolean | string;
}

@Injectable()
export class DevicesTableConfigResolver implements Resolve<EntityTableConfig<DeviceInfo>> {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<DeviceInfo>,
              private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private deviceService: DeviceService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DeviceInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<DeviceInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      deviceInfoFilter: {},
      includeCustomers: true,
      deviceCredentials$: new Subject<DeviceCredentials>(),
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
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('device.devices');
        } else {
          config.tableTitle = this.translate.instant('device.devices');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        config.onLoadAction = (activatedRoute) => this.onLoadAction(config, activatedRoute);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<DeviceInfo>) {
    config.entityType = EntityType.DEVICE;
    config.entityComponent = DeviceComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<DeviceInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.DEVICE);
    config.entityResources = entityTypeResources.get(EntityType.DEVICE);

    config.entityTitle = (device) => device ?
      this.utils.customTranslation(device.name, device.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', {deviceName: device.name});
    config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    config.saveEntity = device => this.deviceService.saveDevice(device).pipe(
        tap(() => {
          this.broadcast.broadcast('deviceSaved');
        }),
      mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
      ));
    config.onEntityAction = action => this.onDeviceAction(action, config);
    config.headerComponent = DeviceTableHeaderComponent;
  }

  onLoadAction(config: EntityTableConfig<DeviceInfo>, route: ActivatedRoute): void {
    const routerQueryParams: DevicePageQueryParams = route.snapshot.queryParams;
    if (routerQueryParams) {
      const queryParams = deepClone(routerQueryParams);
      let replaceUrl = false;
      if (routerQueryParams?.deviceProfileId) {
        config.componentsData.deviceInfoFilter.deviceProfileId = new DeviceProfileId(routerQueryParams?.deviceProfileId);
        delete queryParams.deviceProfileId;
        replaceUrl = true;
      }
      if (isDefined(routerQueryParams?.active)) {
        config.componentsData.deviceInfoFilter.active = (routerQueryParams?.active === true || routerQueryParams?.active === 'true');
        delete queryParams.active;
        replaceUrl = true;
      }
      if (replaceUrl) {
        this.router.navigate([], {
          relativeTo: route,
          queryParams,
          queryParamsHandling: '',
          replaceUrl: true
        });
      }
    }
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<DeviceInfo>): Array<EntityColumn<DeviceInfo>> {
    const columns: Array<EntityColumn<DeviceInfo>> = [
      new DateEntityTableColumn<DeviceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DeviceInfo>('name', 'device.name', '20%', config.entityTitle),
      new EntityTableColumn<DeviceInfo>('type', 'device-profile.device-profile', '20%'),
      new EntityTableColumn<DeviceInfo>('label', 'device.label', '15%'),
      new EntityTableColumn<DeviceInfo>('active', 'device.state', '80px',
        entity => this.deviceState(entity), entity => this.deviceStateStyle(entity))
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<DeviceInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new GroupChipsEntityTableColumn<DeviceInfo>( 'groups', 'entity.groups', '25%')
    );
    columns.push(
      new EntityTableColumn<DeviceInfo>('gateway', 'device.is-gateway', '60px',
        entity => checkBoxCell(entity.additionalInfo && entity.additionalInfo.gateway), () => ({}), false)
    );
    return columns;
  }

  private deviceState(device: DeviceInfo): string {
    let translateKey = 'device.active';
    let backgroundColor = 'rgba(25, 128, 56, 0.08)';
    if (!device.active) {
      translateKey = 'device.inactive';
      backgroundColor = 'rgba(209, 39, 48, 0.08)';
    }
    return `<div class="status" style="border-radius: 16px; height: 32px;
                line-height: 32px; padding: 0 12px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private deviceStateStyle(device: DeviceInfo): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038',
      cursor: 'pointer'
    };
    if (!device.active) {
      styleObj.color = '#d12730';
    }
    return styleObj;
  }

  configureEntityFunctions(config: EntityTableConfig<DeviceInfo>): void {
    config.entitiesFetchFunction = pageLink => this.deviceService.getDeviceInfosByQuery(this.prepareDeviceInfoQuery(config, pageLink));
    config.deleteEntity = id => this.deviceService.deleteDevice(id.id);
  }

  prepareDeviceInfoQuery(config: EntityTableConfig<DeviceInfo>, pageLink: PageLink): DeviceInfoQuery {
    const deviceInfoFilter: DeviceInfoFilter = deepClone(config.componentsData.deviceInfoFilter);
    deviceInfoFilter.includeCustomers = config.componentsData.includeCustomers;
    if (config.customerId) {
      deviceInfoFilter.customerId = new CustomerId(config.customerId);
    }
    return new DeviceInfoQuery(pageLink, deviceInfoFilter);
  }

  configureCellActions(config: EntityTableConfig<DeviceInfo>): Array<CellActionDescriptor<DeviceInfo>> {
    const actions: Array<CellActionDescriptor<DeviceInfo>> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.READ_CREDENTIALS) &&
      !this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE_CREDENTIALS)) {
      actions.push(
        {
          name: this.translate.instant('device.view-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity, true, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE_CREDENTIALS)) {
      actions.push(
        {
          name: this.translate.instant('device.manage-credentials'),
          icon: 'security',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCredentials($event, entity, false, config)
        }
      );
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<DeviceInfo>): Array<GroupActionDescriptor<DeviceInfo>> {
    const actions: Array<GroupActionDescriptor<DeviceInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<DeviceInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('device.add-device-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.deviceWizard($event, config)
      },
      {
        name: this.translate.instant('device.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importDevices($event, config)
      },
    );
    config.addEntity = () => {this.deviceWizard(null, config); return of(null); };
    return actions;
  }

  private openDevice($event: Event, device: DeviceInfo, config: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([device.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  importDevices($event: Event, config: EntityTableConfig<DeviceInfo>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.homeDialogs.importEntities(customerId, EntityType.DEVICE, null).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  deviceWizard($event: Event, config: EntityTableConfig<DeviceInfo>) {
    this.dialog.open<DeviceWizardDialogComponent, DeviceWizardDialogData,
      boolean>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: config.customerId
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  manageCredentials($event: Event, device: DeviceInfo, isReadOnly: boolean, config: EntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCredentialsDialogComponent, DeviceCredentialsDialogData,
      DeviceCredentials>(DeviceCredentialsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId: device.id.id,
        deviceProfileId: device.deviceProfileId.id,
        isReadOnly
      }
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  manageOwnerAndGroups($event: Event, device: DeviceInfo, config: EntityTableConfig<DeviceInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, device).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onDeviceAction(action: EntityAction<DeviceInfo>, config: EntityTableConfig<DeviceInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDevice(action.event, action.entity, config);
        return true;
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity, false, config);
        return true;
      case 'viewCredentials':
        this.manageCredentials(action.event, action.entity, true, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
