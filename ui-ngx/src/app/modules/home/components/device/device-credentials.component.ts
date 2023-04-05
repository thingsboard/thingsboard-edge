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
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import {
  credentialTypeNames,
  credentialTypesByTransportType,
  DeviceCredentials,
  DeviceCredentialsType,
  DeviceTransportType
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

  private deviceTransportTypeValue = DeviceTransportType.DEFAULT;
  get deviceTransportType(): DeviceTransportType {
    return this.deviceTransportTypeValue;
  }
  @Input()
  set deviceTransportType(type: DeviceTransportType) {
    if (type) {
      this.deviceTransportTypeValue = type;
      this.credentialsTypes = credentialTypesByTransportType.get(type);
      const currentType = this.deviceCredentialsFormGroup.get('credentialsType').value;
      if (!this.credentialsTypes.includes(currentType)) {
        this.deviceCredentialsFormGroup.get('credentialsType').patchValue(this.credentialsTypes[0], {onlySelf: true});
      }
    }
  }

  private destroy$ = new Subject<void>();

  deviceCredentialsFormGroup: UntypedFormGroup;

  deviceCredentialsType = DeviceCredentialsType;

  credentialsTypes = credentialTypesByTransportType.get(DeviceTransportType.DEFAULT);

  credentialTypeNamesMap = credentialTypeNames;

  private propagateChange = (v: any) => {};

  constructor(public fb: UntypedFormBuilder) {
    this.deviceCredentialsFormGroup = this.fb.group({
      credentialsType: [DeviceCredentialsType.ACCESS_TOKEN],
      credentialsId: [null],
      credentialsValue: [null]
    });
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
      const credentialsType = this.credentialsTypes.includes(value.credentialsType) ? value.credentialsType : this.credentialsTypes[0];
      this.deviceCredentialsFormGroup.patchValue({
        credentialsType,
        credentialsId: value.credentialsId,
        credentialsValue: value.credentialsValue
      }, {emitEvent: false});
      this.updateValidators();
    }
  }

  updateView() {
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value;
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
      this.deviceCredentialsFormGroup.updateValueAndValidity();
    }
  }

  public validate(c: UntypedFormControl) {
    return this.deviceCredentialsFormGroup.valid ? null : {
      deviceCredentials: {
        valid: false,
      },
    };
  }

  credentialsTypeChanged(): void {
    this.deviceCredentialsFormGroup.patchValue({
      credentialsId: null,
      credentialsValue: null
    });
    this.updateValidators();
  }

  updateValidators(): void {
    const credentialsType = this.deviceCredentialsFormGroup.get('credentialsType').value as DeviceCredentialsType;
    switch (credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([Validators.required, Validators.pattern(/^.{1,32}$/)]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        break;
      default:
        this.deviceCredentialsFormGroup.get('credentialsValue').setValidators([Validators.required]);
        this.deviceCredentialsFormGroup.get('credentialsValue').updateValueAndValidity({emitEvent: false});
        this.deviceCredentialsFormGroup.get('credentialsId').setValidators([]);
        this.deviceCredentialsFormGroup.get('credentialsId').updateValueAndValidity({emitEvent: false});
        break;
    }
  }
}
