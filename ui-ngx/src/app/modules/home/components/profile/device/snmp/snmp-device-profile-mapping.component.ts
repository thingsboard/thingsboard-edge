///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { SnmpMapping } from '@shared/models/device.models';
import { Subscription } from 'rxjs';
import { DataType, DataTypeTranslationMap } from '@shared/models/constants';
import { isUndefinedOrNull } from '@core/utils';

@Component({
  selector: 'tb-snmp-device-profile-mapping',
  templateUrl: './snmp-device-profile-mapping.component.html',
  styleUrls: ['./snmp-device-profile-mapping.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SnmpDeviceProfileMappingComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SnmpDeviceProfileMappingComponent),
      multi: true
    }]
})
export class SnmpDeviceProfileMappingComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  mappingsConfigForm: FormGroup;

  dataTypes = Object.values(DataType);
  dataTypesTranslationMap = DataTypeTranslationMap;

  @Input()
  disabled: boolean;

  private readonly oidPattern: RegExp  = /^\.?([0-2])((\.0)|(\.[1-9][0-9]*))*$/;

  private valueChange$: Subscription = null;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) { }

  ngOnInit() {
    this.mappingsConfigForm = this.fb.group({
      mappings: this.fb.array([])
    });
    this.valueChange$ = this.mappingsConfigForm.valueChanges.subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mappingsConfigForm.disable({emitEvent: false});
    } else {
      this.mappingsConfigForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mappingsConfigForm.valid && this.mappingsConfigForm.value.mappings.length ? null : {
      mapping: false
    };
  }

  writeValue(mappings: SnmpMapping[]) {
    if (mappings?.length === this.mappingsConfigFormArray.length) {
      this.mappingsConfigFormArray.patchValue(mappings, {emitEvent: false});
    } else {
      const mappingsControl: Array<AbstractControl> = [];
      if (mappings) {
        mappings.forEach((config) => {
          mappingsControl.push(this.createdFormGroup(config));
        });
      }
      this.mappingsConfigForm.setControl('mappings', this.fb.array(mappingsControl), {emitEvent: false});
      if (!mappings || !mappings.length) {
        this.addMappingConfig();
      }
      if (this.disabled) {
        this.mappingsConfigForm.disable({emitEvent: false});
      } else {
        this.mappingsConfigForm.enable({emitEvent: false});
      }
    }
    if (!this.disabled && !this.mappingsConfigForm.valid) {
      this.updateModel();
    }
  }

  get mappingsConfigFormArray(): FormArray {
    return this.mappingsConfigForm.get('mappings') as FormArray;
  }

  public addMappingConfig() {
    this.mappingsConfigFormArray.push(this.createdFormGroup());
    this.mappingsConfigForm.updateValueAndValidity();
    if (!this.mappingsConfigForm.valid) {
      this.updateModel();
    }
  }

  public removeMappingConfig(index: number) {
    this.mappingsConfigFormArray.removeAt(index);
  }

  private createdFormGroup(value?: SnmpMapping): FormGroup {
    if (isUndefinedOrNull(value)) {
      value = {
        dataType: DataType.STRING,
        key: '',
        oid: ''
      };
    }
    return this.fb.group({
      dataType: [value.dataType, Validators.required],
      key: [value.key, Validators.required],
      oid: [value.oid, [Validators.required, Validators.pattern(this.oidPattern)]]
    });
  }

  private updateModel() {
    const value: SnmpMapping[] = this.mappingsConfigForm.get('mappings').value;
    this.propagateChange(value);
  }

}
