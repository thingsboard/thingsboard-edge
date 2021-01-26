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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray, FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, Validators
} from '@angular/forms';
import {
  CAMEL_CASE_REGEXP,
  ResourceLwM2M
} from '@home/components/profile/device/lwm2m/profile-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone, isUndefined } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resource.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourceComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourceComponent implements ControlValueAccessor, OnInit, Validators {

  resourceFormGroup : FormGroup;

  disabled = false as boolean;
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }
  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.resourceFormGroup = this.fb.group({'resources': this.fb.array([])});
    this.resourceFormGroup.valueChanges.subscribe(value => {
      if (!this.disabled) {
        this.propagateChangeState(value.resources);
      }
    });
  }

  ngOnInit(): void {
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.createResourceLwM2M(value);
  }

  get resourceFormArray(): FormArray{
    return this.resourceFormGroup.get('resources') as FormArray;
  }

  resourceLwm2mFormArray(instance: FormGroup): FormArray {
    return instance.get('resources') as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourceFormGroup.disable();
    } else {
      this.resourceFormGroup.enable();
    }
  }

  getDisabledState(): boolean {
    return this.disabled;
  }

  updateValueKeyName (event: any, z: number): void {
    this.resourceFormArray.at(z).patchValue( {keyName:  this.keysToCamel(deepClone(event.target.value))} );
  }

  keysToCamel(o: any): string {
    let val = o.split(" ");
    let playStore = [];
    val.forEach(function (item, k){
      item = item.replace(CAMEL_CASE_REGEXP, '');
      item = (k===0)? item.charAt(0).toLowerCase() + item.substr(1) : item.charAt(0).toUpperCase() + item.substr(1)
      playStore.push(item);
    });
    return playStore.join('');
  }

  createResourceLwM2M(resourcesLwM2MJson: ResourceLwM2M []): void {
    if(resourcesLwM2MJson.length === this.resourceFormArray.length) {
      this.resourceFormArray.patchValue(resourcesLwM2MJson, {emitEvent: false})
    } else {
      this.resourceFormArray.clear();
      resourcesLwM2MJson.forEach(resourceLwM2M => {
        this.resourceFormArray.push(this.fb.group({
          id: resourceLwM2M.id,
          name: resourceLwM2M.name,
          observe: resourceLwM2M.observe,
          attribute: resourceLwM2M.attribute,
          telemetry: resourceLwM2M.telemetry,
          keyName: [resourceLwM2M.keyName, Validators.required]
        }));
      })
    }
  }

  private propagateChange = (v: any) => {
  };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState(value: any): void {
    if (value && this.resourceFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams(index: number): number {
    return index;
  }
}
