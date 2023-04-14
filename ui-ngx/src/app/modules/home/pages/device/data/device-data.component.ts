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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceData,
  deviceProfileTypeConfigurationInfoMap,
  deviceTransportTypeConfigurationInfoMap
} from '@shared/models/device.models';

@Component({
  selector: 'tb-device-data',
  templateUrl: './device-data.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceDataComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceDataComponent),
      multi: true
    },
  ]
})
export class DeviceDataComponent implements ControlValueAccessor, OnInit, Validator {

  deviceDataFormGroup: UntypedFormGroup;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  displayDeviceConfiguration: boolean;
  displayTransportConfiguration: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.deviceDataFormGroup = this.fb.group({
      configuration: [null, Validators.required],
      transportConfiguration: [null, Validators.required]
    });
    this.deviceDataFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceDataFormGroup.disable({emitEvent: false});
    } else {
      this.deviceDataFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceData | null): void {
    const deviceProfileType = value?.configuration?.type;
    this.displayDeviceConfiguration = deviceProfileType &&
      deviceProfileTypeConfigurationInfoMap.get(deviceProfileType).hasDeviceConfiguration;
    const deviceTransportType = value?.transportConfiguration?.type;
    this.displayTransportConfiguration = deviceTransportType &&
      deviceTransportTypeConfigurationInfoMap.get(deviceTransportType).hasDeviceConfiguration;
    this.deviceDataFormGroup.patchValue({configuration: value?.configuration}, {emitEvent: false});
    this.deviceDataFormGroup.patchValue({transportConfiguration: value?.transportConfiguration}, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.deviceDataFormGroup.valid ? null : {
      deviceDataForm: false
    };
  }

  private updateModel() {
    let deviceData: DeviceData = null;
    if (this.deviceDataFormGroup.valid) {
      deviceData = this.deviceDataFormGroup.getRawValue();
    }
    this.propagateChange(deviceData);
  }
}
