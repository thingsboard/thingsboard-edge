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

import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Inject,
  Injector,
  SkipSelf,
  ViewChild
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DeviceProfile } from '@shared/models/device.models';
import { DeviceProfileComponent } from './device-profile.component';
import { DeviceProfileService } from '@core/http/device-profile.service';

export interface DeviceProfileDialogData {
  deviceProfile: DeviceProfile;
  isAdd: boolean;
}

@Component({
  selector: 'tb-device-profile-dialog',
  templateUrl: './device-profile-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DeviceProfileDialogComponent}],
  styleUrls: []
})
export class DeviceProfileDialogComponent extends
  DialogComponent<DeviceProfileDialogComponent, DeviceProfile> implements ErrorStateMatcher, AfterViewInit {

  isAdd: boolean;
  deviceProfile: DeviceProfile;

  submitted = false;

  @ViewChild('deviceProfileComponent', {static: true}) deviceProfileComponent: DeviceProfileComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceProfileDialogData,
              public dialogRef: MatDialogRef<DeviceProfileDialogComponent, DeviceProfile>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private deviceProfileService: DeviceProfileService) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.deviceProfile = this.data.deviceProfile;
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.deviceProfileComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.deviceProfileComponent.entityForm.valid) {
      this.deviceProfile = {...this.deviceProfile, ...this.deviceProfileComponent.entityFormValue()};
      this.deviceProfileService.saveDeviceProfileAndConfirmOtaChange(this.deviceProfile, this.deviceProfile).subscribe(
        (deviceProfile) => {
          this.dialogRef.close(deviceProfile);
        }
      );
    }
  }
}
