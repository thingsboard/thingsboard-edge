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
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { RateLimits, rateLimitsArrayToString, stringToRateLimitsArray } from './rate-limits.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';

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
export class RateLimitsListComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input() disabled: boolean;

  rateLimitsListFormGroup: FormGroup;

  rateLimitsArray: Array<RateLimits>;

  private valueChangeSubscription: Subscription = null;
  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.rateLimitsListFormGroup = this.fb.group({
      rateLimits: this.fb.array([])
    });
    this.valueChangeSubscription = this.rateLimitsListFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.updateView(value?.rateLimits);
      }
    );
  }

  public removeRateLimits(index: number) {
    (this.rateLimitsListFormGroup.get('rateLimits') as FormArray).removeAt(index);
  }

  public addRateLimits() {
    this.rateLimitsFormArray.push(this.fb.group({
      value: [null, [Validators.required]],
      time: [null, [Validators.required]]
    }));
  }

  get rateLimitsFormArray(): FormArray {
    return this.rateLimitsListFormGroup.get('rateLimits') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
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
      const rateLimitsArray = rateLimits.split(',');
      for (let i = 0; i < rateLimitsArray.length; i++) {
        const [value, time] = rateLimitsArray[i].split(':');
        const rateLimitsControl = this.fb.group({
          value: [value, [Validators.required]],
          time: [time, [Validators.required]]
        });
        if (this.disabled) {
          rateLimitsControl.disable();
        }
        rateLimitsControls.push(rateLimitsControl);
      }
    }
    this.rateLimitsListFormGroup.setControl('rateLimits', this.fb.array(rateLimitsControls), {emitEvent: false});
    this.rateLimitsArray = stringToRateLimitsArray(rateLimits);
  }

  updateView(rateLimitsArray: Array<RateLimits>) {
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

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
