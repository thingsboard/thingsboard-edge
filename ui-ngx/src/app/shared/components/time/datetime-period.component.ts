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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DAY, FixedWindow, MINUTE } from '@shared/models/time/time.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { distinctUntilChanged } from 'rxjs/operators';

interface DateTimePeriod {
  startDate: Date;
  endDate: Date;
}

@Component({
  selector: 'tb-datetime-period',
  templateUrl: './datetime-period.component.html',
  styleUrls: ['./datetime-period.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatetimePeriodComponent),
      multi: true
    }
  ]
})
export class DatetimePeriodComponent implements ControlValueAccessor {

  @Input() disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  private modelValue: FixedWindow;

  maxStartDate: Date;
  maxEndDate: Date;

  private maxStartDateTs: number;
  private minEndDateTs: number;
  private maxStartTs: number;
  private maxEndTs: number;

  private timeShiftMs = MINUTE;

  dateTimePeriodFormGroup = this.fb.group({
    startDate: this.fb.control<Date>(null),
    endDate: this.fb.control<Date>(null)
  });

  private changePending = false;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
    this.dateTimePeriodFormGroup.valueChanges.pipe(
      distinctUntilChanged((prevDateTimePeriod, dateTimePeriod) =>
        prevDateTimePeriod.startDate === dateTimePeriod.startDate && prevDateTimePeriod.endDate === dateTimePeriod.endDate),
      takeUntilDestroyed()
    ).subscribe((dateTimePeriod: DateTimePeriod) => {
      this.updateMinMaxDates(dateTimePeriod);
      this.updateView();
    });

    this.dateTimePeriodFormGroup.get('startDate').valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(startDate => this.onStartDateChange(startDate));
    this.dateTimePeriodFormGroup.get('endDate').valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(endDate => this.onEndDateChange(endDate));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.changePending && this.propagateChange) {
      this.changePending = false;
      this.propagateChange(this.modelValue);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.dateTimePeriodFormGroup.disable({emitEvent: false});
    } else {
      this.dateTimePeriodFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(datePeriod: FixedWindow): void {
    this.modelValue = datePeriod;
    if (this.modelValue) {
      this.dateTimePeriodFormGroup.patchValue({
        startDate: new Date(this.modelValue.startTimeMs),
        endDate: new Date(this.modelValue.endTimeMs)
      }, {emitEvent: false});
    } else {
      const date = new Date();
      this.dateTimePeriodFormGroup.patchValue({
        startDate: new Date(date.getTime() - DAY),
        endDate: date
      }, {emitEvent: false});
      this.updateView();
    }
    this.updateMinMaxDates(this.dateTimePeriodFormGroup.value);
  }

  private updateView() {
    let value: FixedWindow = null;
    const dateTimePeriod = this.dateTimePeriodFormGroup.value;
    if (dateTimePeriod.startDate && dateTimePeriod.endDate) {
      value = {
        startTimeMs: dateTimePeriod.startDate.getTime(),
        endTimeMs: dateTimePeriod.endDate.getTime()
      };
    }
    this.modelValue = value;
    if (!this.propagateChange) {
      this.changePending = true;
    } else {
      this.propagateChange(this.modelValue);
    }
  }

  private updateMinMaxDates(dateTimePeriod: Partial<DateTimePeriod>) {
    this.maxEndDate = new Date();
    this.maxEndTs = this.maxEndDate.getTime();
    this.maxStartTs = this.maxEndTs - this.timeShiftMs;
    this.maxStartDate = new Date(this.maxStartTs);

    if (dateTimePeriod.endDate) {
      this.maxStartDateTs = dateTimePeriod.endDate.getTime() - this.timeShiftMs;
    }
    if (dateTimePeriod.startDate) {
      this.minEndDateTs = dateTimePeriod.startDate.getTime() + this.timeShiftMs;
    }
  }

  private onStartDateChange(startDate: Date) {
    if (startDate) {
      if (startDate.getTime() > this.maxStartTs) {
        this.dateTimePeriodFormGroup.get('startDate').patchValue(new Date(this.maxStartTs), { emitEvent: false });
      }
      if (startDate.getTime() > this.maxStartDateTs) {
        this.dateTimePeriodFormGroup.get('endDate').patchValue(new Date(startDate.getTime() + this.timeShiftMs), { emitEvent: false });
      }
    }
  }

  private onEndDateChange(endDate: Date) {
    if (endDate) {
      if (endDate.getTime() > this.maxEndTs) {
        this.dateTimePeriodFormGroup.get('endDate').patchValue(new Date(this.maxEndTs), { emitEvent: false });
      }
      if (endDate.getTime() < this.minEndDateTs) {
        this.dateTimePeriodFormGroup.get('startDate').patchValue(new Date(endDate.getTime() - this.timeShiftMs), { emitEvent: false });
      }
    }
  }

}
