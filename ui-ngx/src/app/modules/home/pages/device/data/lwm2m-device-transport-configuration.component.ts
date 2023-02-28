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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceTransportConfiguration,
  DeviceTransportType,
  Lwm2mDeviceTransportConfiguration
} from '@shared/models/device.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceTransportConfigurationComponent),
    multi: true
  }]
})
export class Lwm2mDeviceTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  lwm2mDeviceTransportConfigurationFormGroup: FormGroup;

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

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.lwm2mDeviceTransportConfigurationFormGroup = this.fb.group({
      powerMode: [null],
      edrxCycle: [{disabled: true, value: 0}, Validators.required],
      psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
      pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required]
    });
    this.lwm2mDeviceTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.lwm2mDeviceTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.enable({emitEvent: false});
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value: Lwm2mDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.lwm2mDeviceTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').patchValue(null, {emitEvent: false});
    }
    if (!this.disabled) {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.lwm2mDeviceTransportConfigurationFormGroup.valid) {
      configuration = this.lwm2mDeviceTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }
}
