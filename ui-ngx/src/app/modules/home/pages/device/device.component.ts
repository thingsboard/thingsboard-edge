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

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Device } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { ClipboardService } from 'ngx-clipboard';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';

@Component({
  selector: 'tb-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.scss']
})
export class DeviceComponent extends GroupEntityComponent<Device> {

  entityType = EntityType;

//  deviceScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private deviceService: DeviceService,
              private clipboardService: ClipboardService,
              @Inject('entity') protected entityValue: Device,
              @Inject('entitiesTableConfig') protected entitiesTableConfig: GroupEntityTableConfig<Device>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfig);
  }

  ngOnInit() {
    // this.deviceScope = this.entitiesTableConfig.componentsData.deviceScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideAssignmentActions() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.assignmentEnabled(this.entity);
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

  buildForm(entity: Device): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        label: [entity ? entity.label : ''],
        additionalInfo: this.fb.group(
          {
            gateway: [entity && entity.additionalInfo ? entity.additionalInfo.gateway : false],
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: Device) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({additionalInfo:
        {gateway: entity.additionalInfo ? entity.additionalInfo.gateway : false}});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
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

  copyAccessToken($event) {
    if (this.entity.id) {
      this.deviceService.getDeviceCredentials(this.entity.id.id, true).subscribe(
        (deviceCredentials) => {
          const credentialsId = deviceCredentials.credentialsId;
          if (this.clipboardService.copyFromContent(credentialsId)) {
            this.store.dispatch(new ActionNotificationShow(
              {
                message: this.translate.instant('device.accessTokenCopiedMessage'),
                type: 'success',
                duration: 750,
                verticalPosition: 'bottom',
                horizontalPosition: 'right'
              }));
          }
        }
      );
    }
  }
}
