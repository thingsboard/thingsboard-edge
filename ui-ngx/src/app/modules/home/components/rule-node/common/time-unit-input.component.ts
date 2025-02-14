///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { TimeUnit, timeUnitTranslations } from '../rule-node-config.models';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';
import { DAY, HOUR, MINUTE, SECOND } from '@shared/models/time/time.models';
import { SubscriptSizing } from '@angular/material/form-field';

interface TimeUnitInputModel {
  time: number;
  timeUnit: TimeUnit
}

@Component({
  selector: 'tb-time-unit-input',
  templateUrl: './time-unit-input.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TimeUnitInputComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => TimeUnitInputComponent),
    multi: true
  }]
})
export class TimeUnitInputComponent implements ControlValueAccessor, Validator, OnInit {

  @Input()
  labelText: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  requiredText: string;

  @Input()
  @coerceNumber()
  minTime = 0;

  @Input()
  minErrorText: string;

  @Input()
  @coerceNumber()
  maxTime: number;

  @Input()
  maxErrorText: string;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  timeUnits = Object.values(TimeUnit).filter(item => item !== TimeUnit.MILLISECONDS) as TimeUnit[];

  timeUnitTranslations = timeUnitTranslations;

  timeInputForm = this.fb.group({
    time: [0],
    timeUnit: [TimeUnit.SECONDS]
  });

  private timeIntervalsInSec = new Map<TimeUnit, number>([
    [TimeUnit.DAYS, DAY/SECOND],
    [TimeUnit.HOURS, HOUR/SECOND],
    [TimeUnit.MINUTES, MINUTE/SECOND],
    [TimeUnit.SECONDS, SECOND/SECOND],
  ]);

  private modelValue: number;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    if(this.required || this.maxTime) {
      const timeControl = this.timeInputForm.get('time');
      const validators = [Validators.pattern(/^\d*$/)];
      if (this.required) {
        validators.push(Validators.required);
      }
      if (this.maxTime) {
        validators.push((control: AbstractControl) =>
          Validators.max(Math.floor(this.maxTime / this.timeIntervalsInSec.get(this.timeInputForm.get('timeUnit').value)))(control)
        );
      }
      if (isDefinedAndNotNull(this.minTime)) {
        validators.push(Validators.min(this.minTime));
      }

      timeControl.setValidators(validators);
      timeControl.updateValueAndValidity({ emitEvent: false });
    }

    this.timeInputForm.get('timeUnit').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.timeInputForm.get('time').updateValueAndValidity({onlySelf: true});
      this.timeInputForm.get('time').markAsTouched({onlySelf: true});
    });

    this.timeInputForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.updatedModel(value);
    });
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.timeInputForm.disable({emitEvent: false});
    } else {
      this.timeInputForm.enable({emitEvent: false});
      if(this.timeInputForm.invalid) {
        setTimeout(() => this.updatedModel(this.timeInputForm.value, true))
      }
    }
  }

  writeValue(sec: number) {
    if (sec !== this.modelValue) {
      if (isDefinedAndNotNull(sec) && isNumeric(sec) && Number(sec) !== 0) {
        this.timeInputForm.patchValue(this.parseTime(sec), {emitEvent: false});
        this.modelValue = sec;
      } else {
        this.timeInputForm.patchValue({
          time: 0,
          timeUnit: TimeUnit.SECONDS
        }, {emitEvent: false});
        this.modelValue = 0;
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.timeInputForm.valid ? null : {
      timeInput: false
    };
  }

  private updatedModel(value: Partial<TimeUnitInputModel>, forceUpdated = false) {
    const time = value.time * this.timeIntervalsInSec.get(value.timeUnit);
    if (this.modelValue !== time || forceUpdated) {
      this.modelValue = time;
      this.propagateChange(time);
    }
  }

  private parseTime(value: number): TimeUnitInputModel {
    for (const [timeUnit, timeValue] of this.timeIntervalsInSec) {
      const calc = value / timeValue;
      if (Number.isInteger(calc)) {
        return {
          time: calc,
          timeUnit: timeUnit
        }
      }
    }
  }

}
