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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
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
import { RateLimits, rateLimitsArrayToString, stringToRateLimitsArray } from './rate-limits.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-rate-limits-list',
  templateUrl: './rate-limits-list.component.html',
  styleUrls: ['./rate-limits-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RateLimitsListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RateLimitsListComponent),
      multi: true
    }
  ]
})
export class RateLimitsListComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  rateLimitsListFormGroup: FormGroup;

  rateLimitsArray: Array<RateLimits>;

  private propagateChange = (_v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.rateLimitsListFormGroup = this.fb.group({
      rateLimits: this.fb.array([])
    });
    this.rateLimitsListFormGroup.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.updateView(value?.rateLimits ?? []);
      }
    );
  }

  public removeRateLimits(index: number) {
    this.rateLimitsFormArray.removeAt(index);
  }

  public addRateLimits() {
    this.rateLimitsFormArray.push(this.fb.group({
      value: [null, [Validators.required]],
      time: [null, [Validators.required, this.uniqTimeRequired()]]
    }));
  }

  get rateLimitsFormArray(): FormArray<FormGroup> {
    return this.rateLimitsListFormGroup.get('rateLimits') as FormArray<FormGroup>;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.rateLimitsListFormGroup.disable({emitEvent: false});
    } else {
      this.rateLimitsListFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.rateLimitsListFormGroup.valid ? null : {
      rateLimitsList: {valid: false}
    };
  }

  writeValue(rateLimits: string) {
    const rateLimitsControls: Array<FormGroup> = [];
    if (rateLimits) {
      this.rateLimitsArray = stringToRateLimitsArray(rateLimits);
      this.rateLimitsArray.forEach((rateLimit) => {
        const rateLimitsControl = this.fb.group({
          value: [rateLimit.value, [Validators.required]],
          time: [rateLimit.time, [Validators.required, this.uniqTimeRequired()]]
        });
        if (this.disabled) {
          rateLimitsControl.disable();
        }
        rateLimitsControls.push(rateLimitsControl);
      })
    } else {
      this.rateLimitsArray = null;
    }
    this.rateLimitsListFormGroup.setControl('rateLimits', this.fb.array(rateLimitsControls), {emitEvent: false});
  }

  private updateView(rateLimitsArray: Array<RateLimits>) {
    if (rateLimitsArray.length > 0) {
      const notNullRateLimits = rateLimitsArray.filter(rateLimits =>
        isDefinedAndNotNull(rateLimits.value) && isDefinedAndNotNull(rateLimits.time)
      );
      const rateLimitsString = rateLimitsArrayToString(notNullRateLimits);
      this.propagateChange(rateLimitsString);
      this.rateLimitsArray = stringToRateLimitsArray(rateLimitsString);
    } else {
      this.propagateChange(null);
      this.rateLimitsArray = null;
    }
  }

  private uniqTimeRequired(): ValidatorFn {
    return (control: FormControl) => {
      const formGroup = control.parent as FormGroup;
      if (!formGroup) return null;

      const formArray = formGroup.parent as FormArray;
      if (!formArray) return null;

      const newTime = control.value;
      const index = formArray.controls.indexOf(formGroup);

      const isDuplicate = formArray.controls
        .filter((_, i) => i !== index)
        .some(group => group.get('time')?.value === newTime && newTime !== '');

      return isDuplicate ? { duplicateTime: true } : null;
    };
  }
}
