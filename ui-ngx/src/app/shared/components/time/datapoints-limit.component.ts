///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TimeService } from '@core/services/time.service';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-datapoints-limit',
  templateUrl: './datapoints-limit.component.html',
  styleUrls: ['./datapoints-limit.component.scss'],
  providers: [
    {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DatapointsLimitComponent),
    multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DatapointsLimitComponent),
      multi: true
    }
  ]
})
export class DatapointsLimitComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  datapointsLimitFormGroup: FormGroup;

  modelValue: number | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private timeService: TimeService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.datapointsLimitFormGroup = this.fb.group({
      limit: [null, [Validators.min(this.minDatapointsLimit()), Validators.max(this.maxDatapointsLimit())]]
    });
    this.datapointsLimitFormGroup.get('limit').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  updateValidators() {
    if (this.datapointsLimitFormGroup) {
      if (this.required) {
        this.datapointsLimitFormGroup.get('limit').addValidators(Validators.required);
      } else {
        this.datapointsLimitFormGroup.get('limit').removeValidators(Validators.required);
      }
      this.datapointsLimitFormGroup.get('limit').updateValueAndValidity();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.datapointsLimitFormGroup.disable({emitEvent: false});
    } else {
      this.datapointsLimitFormGroup.enable({emitEvent: false});
    }
  }

  private checkLimit(limit?: number): number {
    if (!limit || limit < this.minDatapointsLimit()) {
      return this.minDatapointsLimit();
    } else if (limit > this.maxDatapointsLimit()) {
      return this.maxDatapointsLimit();
    }
    return limit;
  }

  writeValue(value: number | null): void {
    this.modelValue = this.checkLimit(value);
    this.datapointsLimitFormGroup.patchValue(
      { limit: this.modelValue }, {emitEvent: false}
    );
  }

  updateView(value: number | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  validate(): ValidationErrors {
    return this.datapointsLimitFormGroup.get('limit').valid ? null : {
      datapointsLimitFormGroup: false,
    };
  }

  minDatapointsLimit() {
    return this.timeService.getMinDatapointsLimit();
  }

  maxDatapointsLimit() {
    return this.timeService.getMaxDatapointsLimit();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

}
