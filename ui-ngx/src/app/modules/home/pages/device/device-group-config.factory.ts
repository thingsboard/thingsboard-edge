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

import { DeviceCredentials, DeviceInfo } from '@shared/models/device.models';
import { Observable, of, Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceComponent } from '@home/pages/device/device.component';
import { mergeMap, tap } from 'rxjs/operators';
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
import {
  DeviceWizardDialogComponent,
  DeviceWizardDialogData
} from '@home/components/wizard/device-wizard-dialog.component';
import { isDefinedAndNotNull } from '@core/utils';
import { Router, UrlTree } from '@angular/router';
import { WINDOW } from '@core/services/window.service';

@Injectable()
export class DeviceGroupConfigFactory implements EntityGroupStateConfigFactory<DeviceInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<DeviceInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private deviceService: DeviceService,
              private router: Router,
              private broadcast: BroadcastService,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<DeviceInfo>): Observable<GroupEntityTableConfig<DeviceInfo>> {
    const config = new GroupEntityTableConfig<DeviceInfo>(entityGroup, params);

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

    config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    config.saveEntity = device => this.deviceService.saveDevice(device).pipe(
      tap(() => {
        this.broadcast.broadcast('deviceSaved');
      }),
      mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
      ));
    config.deleteEntity = id => this.deviceService.deleteDevice(id.id);

    config.onEntityAction = action => this.onDeviceAction(action, config, params);
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

  deviceWizard(config: GroupEntityTableConfig<DeviceInfo>): Observable<DeviceInfo> {
    return this.dialog.open<DeviceWizardDialogComponent, DeviceWizardDialogData,
      DeviceInfo>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityGroup: config.entityGroup
      }
    }).afterClosed();
  }

  importDevices($event: Event, config: GroupEntityTableConfig<DeviceInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.DEVICE, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  private openDevice($event: Event, device: DeviceInfo, config: GroupEntityTableConfig<DeviceInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'deviceGroups', params.edgeEntitiesGroupId, device.id.id]);
      } else {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'entities', 'devices', 'groups', params.childEntityGroupId, device.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([device.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  manageCredentials($event: Event, device: DeviceInfo | ShortEntityView, isReadOnly: boolean, config: GroupEntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCredentialsDialogComponent, DeviceCredentialsDialogData,
      DeviceCredentials>(DeviceCredentialsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId: device.id.id,
        deviceProfileId: device.deviceProfileId?.id,
        isReadOnly
      }
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  manageOwnerAndGroups($event: Event, device: DeviceInfo, config: GroupEntityTableConfig<DeviceInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, device).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onDeviceAction(action: EntityAction<DeviceInfo>, config: GroupEntityTableConfig<DeviceInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openDevice(action.event, action.entity, config, params);
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
