// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { DeviceInstalledItemDescriptor, IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TbIotHubAddItemDialogComponent, IotHubAddItemDialogData, IotHubAddItemDialogResult } from './iot-hub-add-item-dialog.component';
import { TbIotHubItemDetailDialogComponent, IotHubItemDetailDialogData, IotHubItemDetailDialogMode } from './iot-hub-item-detail-dialog.component';
import { TbIotHubInstallDialogComponent, IotHubInstallDialogData } from './iot-hub-install-dialog.component';
import { TbIotHubUpdateDialogComponent, IotHubUpdateDialogData } from './iot-hub-update-dialog.component';
import { TbIotHubDeleteDialogComponent, IotHubDeleteDialogData } from './iot-hub-delete-dialog.component';
import { TbDeviceInstallDialogComponent, DeviceInstallDialogData } from './device-install-dialog/device-install-dialog.component';
import { TbIotHubInstalledItemsDialogComponent, IotHubInstalledItemsDialogData } from './iot-hub-installed-items-dialog.component';

@Injectable()
export class IotHubActionsService {

  constructor(
    private dialog: MatDialog
  ) {}

  openItemDetail(item: MpItemVersionView, installedItem?: IotHubInstalledItem, installedItemsCount?: number,
                 mode?: IotHubItemDetailDialogMode, showCreator?: boolean, preview?: boolean): Observable<any> {
    return this.dialog.open(TbIotHubItemDetailDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item, installedItem, installedItemsCount, mode, showCreator, preview } as IotHubItemDetailDialogData
    }).afterClosed();
  }

  openInstalledItems(item: MpItemVersionView): Observable<any> {
    return this.dialog.open(TbIotHubInstalledItemsDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item } as IotHubInstalledItemsDialogData
    }).afterClosed();
  }

  addItem(itemType: ItemType, options?: { itemSubType?: string; entityId?: EntityId }): Observable<IotHubAddItemDialogResult> {
    return this.dialog.open(TbIotHubAddItemDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: {
        itemType,
        itemSubType: options?.itemSubType,
        entityId: options?.entityId
      } as IotHubAddItemDialogData
    }).afterClosed();
  }

  installItem(item: MpItemVersionView): Observable<string> {
    if (item.type === ItemType.DEVICE) {
      return this.installDevice(item);
    }
    return this.dialog.open(TbIotHubInstallDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { item } as IotHubInstallDialogData
    }).afterClosed();
  }

  updateItem(installedItem: IotHubInstalledItem, version: string, versionId: string): Observable<string | boolean> {
    if (!installedItem) {
      return of(false);
    }
    return this.dialog.open<TbIotHubUpdateDialogComponent, IotHubUpdateDialogData, string | boolean>(TbIotHubUpdateDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: {
        installedItemId: installedItem.id.id,
        itemName: installedItem.itemName,
        itemType: installedItem.itemType as ItemType,
        version,
        versionId
      }
    }).afterClosed();
  }

  deleteItem(installedItem: IotHubInstalledItem): Observable<boolean> {
    if (!installedItem) {
      return of(false);
    }
    return this.dialog.open<TbIotHubDeleteDialogComponent, IotHubDeleteDialogData, boolean>(TbIotHubDeleteDialogComponent, {
      panelClass: ['tb-dialog'],
      disableClose: true,
      autoFocus: false,
      data: { installedItemId: installedItem.id.id, itemName: installedItem.itemName, itemType: installedItem.itemType }
    }).afterClosed();
  }

  installDevice(item: MpItemVersionView): Observable<string> {
    return this.openDeviceInstallDialog(item);
  }

  reviewDevice(item: MpItemVersionView, deviceDescriptor: DeviceInstalledItemDescriptor): Observable<any> {
    return this.openDeviceInstallDialog(item, {
        reviewMode: true,
        selectedInstallMethod: deviceDescriptor.selectedInstallMethod,
        installState: deviceDescriptor.installState
      });
  }

  private openDeviceInstallDialog(item: MpItemVersionView,
                                  options?: { reviewMode?: boolean; selectedInstallMethod?: string; installState?: any }): Observable<any> {
    return this.dialog.open<TbDeviceInstallDialogComponent, DeviceInstallDialogData>(TbDeviceInstallDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-md'],
      disableClose: true,
      autoFocus: false,
      data: { item, ...options }
    }).afterClosed();
  }
}
