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
import { Observable, of } from 'rxjs';
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

    if (config.settings.enableCredentialsManagement) {
      if (this.userPermissionsService.hasGroupEntityPermission(Operation.READ_CREDENTIALS, config.entityGroup) &&
        !this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.view-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, true)
          }
        );
      }

      if (this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.manage-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, false)
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

  manageCredentials($event: Event, device: Device | ShortEntityView, isReadOnly: boolean) {
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
