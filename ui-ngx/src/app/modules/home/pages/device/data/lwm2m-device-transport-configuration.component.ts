///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { DeviceTransportConfiguration, Lwm2mDeviceTransportConfiguration } from '@shared/models/device.models';
import { PowerMode, PowerModeTranslationMap } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
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
  powerMods = Object.values(PowerMode);
  powerModeTranslationMap = PowerModeTranslationMap;

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
      edrxCycle: [0]
    });
    this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((powerMode: PowerMode) => {
      if (powerMode === PowerMode.E_DRX) {
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').enable({emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').patchValue(0, {emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle')
          .setValidators([Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]);
      } else {
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').disable({emitEvent: false});
        this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').clearValidators();
      }
      this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').updateValueAndValidity({emitEvent: false});
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
    }
  }

  writeValue(value: Lwm2mDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.lwm2mDeviceTransportConfigurationFormGroup.get('powerMode').patchValue(value.powerMode, {emitEvent: false, onlySelf: true});
      this.lwm2mDeviceTransportConfigurationFormGroup.get('edrxCycle').patchValue(value.edrxCycle || 0, {emitEvent: false});
    } else {
      this.lwm2mDeviceTransportConfigurationFormGroup.patchValue({powerMode: null, edrxCycle: 0}, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: DeviceTransportConfiguration = null;
    if (this.lwm2mDeviceTransportConfigurationFormGroup.valid) {
      configuration = this.lwm2mDeviceTransportConfigurationFormGroup.value;
      // configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }
}
