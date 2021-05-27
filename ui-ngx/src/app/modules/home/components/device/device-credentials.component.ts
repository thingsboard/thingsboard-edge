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
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  credentialTypeNames,
  DeviceCredentialMQTTBasic,
  DeviceCredentials,
  DeviceCredentialsType
} from '@shared/models/device.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-device-credentials',
  templateUrl: './device-credentials.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceCredentialsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceCredentialsComponent),
      multi: true,
    }],
  styleUrls: []
})
export class DeviceCredentialsComponent implements ControlValueAccessor, OnInit, Validator, OnDestroy {

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();

  deviceCredentialsFormGroup: FormGroup;

  deviceCredentialsType = DeviceCredentialsType;

  credentialsTypes = Object.values(DeviceCredentialsType);

  credentialTypeNamesMap = credentialTypeNames;

  hidePassword = true;

  private propagateChange = (v: any) => {};

  constructor(public fb: FormBuilder) {
    this.deviceCredentialsFormGroup = this.fb.group({
      credentialsType: [DeviceCredentialsType.ACCESS_TOKEN],
      credentialsId: [null],
      credentialsValue: [null],
      credentialsBasic: this.fb.group({
        clientId: [null, [Validators.pattern(/^[A-Za-z0-9]+$/)]],
        userName: [null],
        password: [null]
      }, {validators: this.atLeastOne(Validators.required, ['clientId', 'userName'])})
    });
    this.deviceCredentialsFormGroup.get('credentialsBasic').disable();
    this.deviceCredentialsFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateView();
    });
    this.deviceCredentialsFormGroup.get('credentialsType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.credentialsTypeChanged();
    });
  }

  ngOnInit(): void {
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(value: DeviceCredentials | null): void {
    if (isDefinedAndNotNull(value)) {
      let credentialsBasic = {clientId: null, userName: null, password: null};
      let credentialsValue = null;
      if (value.credentialsType === DeviceCredentialsType.MQTT_BASIC) {
        credentialsBasic = JSON.parse(value.credentialsValue) as DeviceCredentialMQTTBasic;
      } else {
        credentialsValue = value.credentialsValue;
      }
      this.deviceCredentialsFormGroup.patchValue({
        credentialsType: value.credentialsType,
        credentialsId: value.credentialsId,
        credentialsValue,
        credentialsBasic
      }, {emitEvent: false});
      this.updateValidators();
    }
  }

  updateView() {
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value;
    if (deviceCredentialsValue.credentialsType === DeviceCredentialsType.MQTT_BASIC) {
      deviceCredentialsValue.credentialsValue = JSON.stringify(deviceCredentialsValue.credentialsBasic);
    }
    delete deviceCredentialsValue.credentialsBasic;
    this.propagateChange(deviceCredentialsValue);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    } else {
      this.deviceCredentialsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  public validate(c: FormControl) {
    return this.deviceCredentialsFormGroup.valid ? null : {
      deviceCredentials: {
        valid: false,
      },
    };
  }

  credentialsTypeChanged(): void {
    this.deviceCredentialsFormGroup.patchValue({
      credentialsId: null,
      credentialsValue: null,
      credentialsBasic: {clientId: '', userName: '', password: ''}
    });
    this.updateValidators();
  }

  updateValidators(): void {
    this.hidePassword = true;
    const credentialsType = this.deviceCredentialsFormGroup.get('credentialsType').value as DeviceCredentialsType;
    switch (credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([Validators.required, Validators.pattern(/^.{1,20}$/)]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').disable({emitEvent: false});
        break;
      case DeviceCredentialsType.X509_CERTIFICATE:
      case DeviceCredentialsType.LWM2M_CREDENTIALS:
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([Validators.required]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').disable({emitEvent: false});
        break;
      case DeviceCredentialsType.MQTT_BASIC:
        this.deviceCredentialsFormGroup.get('credentialsBasic').enable({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsBasic').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        break;
    }
  }

  private atLeastOne(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  passwordChanged() {
    const value = this.deviceCredentialsFormGroup.get('credentialsBasic.password').value;
    if (value !== '') {
      this.deviceCredentialsFormGroup.get('credentialsBasic.userName').setValidators([Validators.required]);
    } else {
      this.deviceCredentialsFormGroup.get('credentialsBasic.userName').setValidators([]);
    }
    this.deviceCredentialsFormGroup.get('credentialsBasic.userName').updateValueAndValidity({
      emitEvent: false,
      onlySelf: true
    });
  }
}
