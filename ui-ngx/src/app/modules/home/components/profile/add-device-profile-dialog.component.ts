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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileType,
  deviceProfileTypeTranslationMap,
  DeviceTransportType,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { EntityType } from '@shared/models/entity-type.models';
import { MatHorizontalStepper } from '@angular/material/stepper';
import { RuleChainId } from '@shared/models/id/rule-chain-id';

export interface AddDeviceProfileDialogData {
  deviceProfileName: string;
}

@Component({
  selector: 'tb-add-device-profile-dialog',
  templateUrl: './add-device-profile-dialog.component.html',
  providers: [],
  styleUrls: ['./add-device-profile-dialog.component.scss']
})
export class AddDeviceProfileDialogComponent extends
  DialogComponent<AddDeviceProfileDialogComponent, DeviceProfile> implements AfterViewInit {

  @ViewChild('addDeviceProfileStepper', {static: true}) addDeviceProfileStepper: MatHorizontalStepper;

  selectedIndex = 0;

  entityType = EntityType;

  deviceProfileTypes = Object.keys(DeviceProfileType);

  deviceProfileTypeTranslations = deviceProfileTypeTranslationMap;

  deviceTransportTypes = Object.keys(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceProfileDetailsFormGroup: FormGroup;

  transportConfigFormGroup: FormGroup;

  alarmRulesFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddDeviceProfileDialogData,
              public dialogRef: MatDialogRef<AddDeviceProfileDialogComponent, DeviceProfile>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.deviceProfileDetailsFormGroup = this.fb.group(
      {
        name: [data.deviceProfileName, [Validators.required]],
        type: [DeviceProfileType.DEFAULT, [Validators.required]],
        defaultRuleChainId: [null, []],
        description: ['', []]
      }
    );
    this.transportConfigFormGroup = this.fb.group(
      {
        transportType: [DeviceTransportType.DEFAULT, [Validators.required]],
        transportConfiguration: [createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT),
          [Validators.required]]
      }
    );
    this.transportConfigFormGroup.get('transportType').valueChanges.subscribe(() => {
      this.deviceProfileTransportTypeChanged();
    });

    this.alarmRulesFormGroup = this.fb.group(
      {
        alarms: [null]
      }
    );
  }

  private deviceProfileTransportTypeChanged() {
    const deviceTransportType: DeviceTransportType = this.transportConfigFormGroup.get('transportType').value;
    this.transportConfigFormGroup.patchValue(
      {transportConfiguration: createDeviceProfileTransportConfiguration(deviceTransportType)});
  }

  ngAfterViewInit(): void {
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep() {
    this.addDeviceProfileStepper.previous();
  }

  nextStep() {
    if (this.selectedIndex < 2) {
      this.addDeviceProfileStepper.next();
    } else {
      this.add();
    }
  }

  selectedForm(): FormGroup {
    switch (this.selectedIndex) {
      case 0:
        return this.deviceProfileDetailsFormGroup;
      case 1:
        return this.transportConfigFormGroup;
      case 2:
        return this.alarmRulesFormGroup;
    }
  }

  private add(): void {
    const deviceProfile: DeviceProfile = {
      name: this.deviceProfileDetailsFormGroup.get('name').value,
      type: this.deviceProfileDetailsFormGroup.get('type').value,
      transportType: this.transportConfigFormGroup.get('transportType').value,
      description: this.deviceProfileDetailsFormGroup.get('description').value,
      profileData: {
        configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
        transportConfiguration: this.transportConfigFormGroup.get('transportConfiguration').value,
        alarms: this.alarmRulesFormGroup.get('alarms').value
      }
    };
    if (this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value) {
      deviceProfile.defaultRuleChainId = new RuleChainId(this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value);
    }
    this.deviceProfileService.saveDeviceProfile(deviceProfile).subscribe(
      (savedDeviceProfile) => {
        this.dialogRef.close(savedDeviceProfile);
      }
    );
  }
}
