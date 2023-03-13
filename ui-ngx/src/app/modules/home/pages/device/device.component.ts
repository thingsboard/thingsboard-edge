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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Device } from '@shared/models/device.models';
import {
  createDeviceConfiguration,
  createDeviceTransportConfiguration, DeviceCredentials,
  DeviceData,
  DeviceProfileInfo,
  DeviceProfileType,
  DeviceTransportType
} from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { ClipboardService } from 'ngx-clipboard';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { Subject } from 'rxjs';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { distinctUntilChanged } from 'rxjs/operators';
import { getEntityDetailsPageURL } from '@core/utils';

@Component({
  selector: 'tb-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.scss']
})
export class DeviceComponent extends GroupEntityComponent<Device> {

  entityType = EntityType;

  deviceCredentials$: Subject<DeviceCredentials>;

//  deviceScope: 'tenant' | 'customer' | 'customer_user' | 'edge';

  otaUpdateType = OtaUpdateType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private deviceService: DeviceService,
              private clipboardService: ClipboardService,
              @Inject('entity') protected entityValue: Device,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<Device>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    // this.deviceScope = this.entitiesTableConfig.componentsData.deviceScope;
    this.deviceCredentials$ = this.entitiesTableConfigValue.componentsData.deviceCredentials$;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageCredentials() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageCredentialsEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: Device): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: Device): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        deviceProfileId: [entity ? entity.deviceProfileId : null, [Validators.required]],
        firmwareId: [entity ? entity.firmwareId : null],
        softwareId: [entity ? entity.softwareId : null],
        label: [entity ? entity.label : '', [Validators.maxLength(255)]],
        deviceData: [entity ? entity.deviceData : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            gateway: [entity && entity.additionalInfo ? entity.additionalInfo.gateway : false],
            overwriteActivityTime: [entity && entity.additionalInfo ? entity.additionalInfo.overwriteActivityTime : false],
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    form.get('deviceProfileId').valueChanges.pipe(
      distinctUntilChanged((prev, curr) => prev?.id === curr?.id)
    ).subscribe(profileId => {
      if (profileId && this.isEdit) {
        this.entityForm.patchValue({
          firmwareId: null,
          softwareId: null
        }, {emitEvent: false});
      }
    });
    return form;
  }

  updateForm(entity: Device) {
    this.entityForm.patchValue({
      name: entity.name,
      deviceProfileId: entity.deviceProfileId,
      firmwareId: entity.firmwareId,
      softwareId: entity.softwareId,
      label: entity.label,
      deviceData: entity.deviceData,
      additionalInfo: {
        gateway: entity.additionalInfo ? entity.additionalInfo.gateway : false,
        overwriteActivityTime: entity.additionalInfo ? entity.additionalInfo.overwriteActivityTime : false,
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
  }


  onDeviceIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('device.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onDeviceProfileUpdated() {
    this.entitiesTableConfig.updateData(false);
  }

  onDeviceProfileChanged(deviceProfile: DeviceProfileInfo) {
    if (deviceProfile && this.isEdit) {
      const deviceProfileType: DeviceProfileType = deviceProfile.type;
      const deviceTransportType: DeviceTransportType = deviceProfile.transportType;
      let deviceData: DeviceData = this.entityForm.getRawValue().deviceData;
      if (!deviceData) {
        deviceData = {
          configuration: createDeviceConfiguration(deviceProfileType),
          transportConfiguration: createDeviceTransportConfiguration(deviceTransportType)
        };
        this.entityForm.patchValue({deviceData});
        this.entityForm.markAsDirty();
      } else {
        let changed = false;
        if (deviceData.configuration.type !== deviceProfileType) {
          deviceData.configuration = createDeviceConfiguration(deviceProfileType);
          changed = true;
        }
        if (deviceData.transportConfiguration.type !== deviceTransportType) {
          deviceData.transportConfiguration = createDeviceTransportConfiguration(deviceTransportType);
          changed = true;
        }
        if (changed) {
          this.entityForm.patchValue({deviceData});
          this.entityForm.markAsDirty();
        }
      }
    }
  }
}
