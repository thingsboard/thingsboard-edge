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

import { Device, DeviceCredentials } from '@shared/models/device.models';
import { Params } from '@angular/router';
import { Observable, of } from 'rxjs';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { DatePipe } from '@angular/common';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Injectable } from '@angular/core';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { DeviceComponent } from '@home/pages/device/device.component';
import { tap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@home/pages/device/device-credentials-dialog.component';

@Injectable()
export class DeviceGroupConfigFactory implements EntityGroupStateConfigFactory<Device> {

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private deviceService: DeviceService,
              private broadcast: BroadcastService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  createConfig(params: Params, entityGroup: EntityGroupStateInfo<Device>): Observable<GroupEntityTableConfig<Device>> {
    const config = new GroupEntityTableConfig<Device>(
      this.entityGroupService,
      this.userPermissionsService,
      this.translate,
      this.utils,
      this.datePipe,
      this.dialog,
      entityGroup
    );

    config.entityType = EntityType.DEVICE;
    config.entityComponent = DeviceComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.DEVICE);
    config.entityResources = entityTypeResources.get(EntityType.DEVICE);

    config.entityTitle = (device) => device ?
      this.utils.customTranslation(device.name, device.name) : '';

    config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', { deviceName: device.name });
    config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    config.loadEntity = id => this.deviceService.getDevice(id.id);
    config.saveEntity = device => {
      return this.deviceService.saveDevice(device).pipe(
        tap(() => {
          this.broadcast.broadcast('deviceSaved');
        }));
    };
    config.deleteEntity = id => this.deviceService.deleteDevice(id.id);

    config.onEntityAction = action => this.onDeviceAction(action);

    config.tableTitle = `${entityGroup.name}: ${this.translate.instant('device.devices')}`;

    return of(config);
  }

  manageCredentials($event: Event, device: Device, isReadOnly: boolean) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCredentialsDialogComponent, DeviceCredentialsDialogData,
      DeviceCredentials>(DeviceCredentialsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId: device.id.id,
        isReadOnly
      }
    });
  }

  onDeviceAction(action: EntityAction<Device>): boolean {
    switch (action.action) {
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity, false);
        return true;
      case 'viewCredentials':
        this.manageCredentials(action.event, action.entity, true);
        return true;
    }
    return false;
  }
}
