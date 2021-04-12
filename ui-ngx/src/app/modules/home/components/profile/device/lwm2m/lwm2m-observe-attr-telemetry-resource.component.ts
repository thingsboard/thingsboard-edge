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

import {Component, forwardRef, Input} from '@angular/core';
import {ControlValueAccessor, FormArray, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {ResourceLwM2M, RESOURCES} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import _ from 'lodash';
import {coerceBooleanProperty} from '@angular/cdk/coercion';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resource.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourceComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourceComponent implements ControlValueAccessor {

  private requiredValue: boolean;

  resourceFormGroup: FormGroup;
  disabled = false;

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
    this.resourceFormGroup = this.fb.group({
      resources: this.fb.array([])
    });
    this.resourceFormGroup.valueChanges.subscribe(value => {
      if (!this.disabled) {
        this.propagateChangeState(value.resources);
      }
    });
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.createResourceLwM2M(value);
  }

  get resourceFormArray(): FormArray{
    return this.resourceFormGroup.get(RESOURCES) as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourceFormGroup.disable();
    } else {
      this.resourceFormGroup.enable();
    }
  }

  updateValueKeyName = (event: Event, index: number): void => {
    this.resourceFormArray.at(index).patchValue({keyName: _.camelCase((event.target as HTMLInputElement).value)});
  }

  updateAttributeLwm2m = (event: Event, index: number): void => {
    this.resourceFormArray.at(index).patchValue({attributeLwm2m: event});
  }

  getNameResourceLwm2m = (resourceLwM2M: ResourceLwM2M): string => {
    return  '<' + resourceLwM2M.id +'> ' + resourceLwM2M.name;
  }

  createResourceLwM2M(resourcesLwM2M: ResourceLwM2M[]): void {
    if (resourcesLwM2M.length === this.resourceFormArray.length) {
      this.resourceFormArray.patchValue(resourcesLwM2M, {emitEvent: false});
    } else {
      this.resourceFormArray.clear();
      resourcesLwM2M.forEach(resourceLwM2M => {
        this.resourceFormArray.push(this.fb.group( {
          id: resourceLwM2M.id,
          name: resourceLwM2M.name,
          observe: resourceLwM2M.observe,
          attribute: resourceLwM2M.attribute,
          telemetry: resourceLwM2M.telemetry,
          keyName: [resourceLwM2M.keyName, Validators.required],
          attributeLwm2m: [resourceLwM2M.attributeLwm2m]
        }));
      });
    }
  }

  private propagateChange = (v: any) => { };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState = (value: any): void => {
    if (value && this.resourceFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams = (index: number): number => {
    return index;
  }

  updateObserve = (index: number):  void =>{
    if (this.resourceFormArray.at(index).value.attribute === false && this.resourceFormArray.at(index).value.telemetry === false) {
      this.resourceFormArray.at(index).patchValue({observe: false});
      this.resourceFormArray.at(index).patchValue({attributeLwm2m: {}});
    }
  }

  disableObserve = (index: number):  boolean =>{
    return !this.resourceFormArray.at(index).value.telemetry && !this.resourceFormArray.at(index).value.attribute;
  }
}
