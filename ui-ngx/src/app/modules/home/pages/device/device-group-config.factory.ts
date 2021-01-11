///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Device, DeviceCredentials } from '@shared/models/device.models';
import { Observable, of, Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceComponent } from '@home/pages/device/device.component';
import { tap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@home/pages/device/device-credentials-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { Operation } from '@shared/models/security.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable()
export class DeviceGroupConfigFactory implements EntityGroupStateConfigFactory<Device> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<Device>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private deviceService: DeviceService,
              private broadcast: BroadcastService) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<Device>): Observable<GroupEntityTableConfig<Device>> {
    const config = new GroupEntityTableConfig<Device>(entityGroup, params);

    config.entityComponent = DeviceComponent;

    config.componentsData = {
      deviceCredentials$: new Subject<DeviceCredentials>()
    };

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

    config.onEntityAction = action => this.onDeviceAction(action, config);
    config.addEntity = () => this.deviceWizard(config);

    if (config.settings.enableCredentialsManagement) {
      if (this.userPermissionsService.hasGroupEntityPermission(Operation.READ_CREDENTIALS, config.entityGroup) &&
        !this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.view-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, true, config)
          }
        );
      }

      if (this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.manage-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, false, config)
          }
        );
      }
    }

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('device.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDevices($event, config)
        }
      );
    }
    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  deviceWizard(config: GroupEntityTableConfig<Device>): Observable<Device> {
    return this.dialog.open<DeviceWizardDialogComponent, AddGroupEntityDialogData<Device>,
      Device>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  importDevices($event: Event, config: GroupEntityTableConfig<Device>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.DEVICE, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        config.table.updateData();
      }
    });
  }

  manageCredentials($event: Event, device: Device | ShortEntityView, isReadOnly: boolean, config: GroupEntityTableConfig<Device>) {
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
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  onDeviceAction(action: EntityAction<Device>, config: GroupEntityTableConfig<Device>): boolean {
    switch (action.action) {
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity, false, config);
        return true;
      case 'viewCredentials':
        this.manageCredentials(action.event, action.entity, true, config);
        return true;
    }
    return false;
  }

}
