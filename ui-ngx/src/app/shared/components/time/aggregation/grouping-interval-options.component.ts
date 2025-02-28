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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TimeService } from '@core/services/time.service';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';
import {
  Interval,
  intervalValuesToTimeIntervals,
  TimeInterval,
  TimewindowAggIntervalOptions
} from '@shared/models/time/time.models';
import { isDefined } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-grouping-interval-options',
  templateUrl: './grouping-interval-options.component.html',
  styleUrls: ['./grouping-interval-options.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GroupingIntervalOptionsComponent),
      multi: true
    }
  ]
})
export class GroupingIntervalOptionsComponent implements OnInit, ControlValueAccessor {

  @Input()
  @coerceNumber()
  min: number;

  @Input()
  @coerceNumber()
  max: number;

  @Input()
  @coerceBoolean()
  useCalendarIntervals = false;

  @Input() disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  allIntervals: Array<TimeInterval>;
  allIntervalValues: Array<Interval>;
  selectedIntervals: Array<TimeInterval>;

  timeintervalFormGroup: FormGroup;

  private modelValue: TimewindowAggIntervalOptions;
  private rendered = false;
  private propagateChangeValue: any;

  private propagateChange = (value: any) => {
    this.propagateChangeValue = value;
  };

  constructor(private timeService: TimeService,
              private fb: FormBuilder) {
    this.timeintervalFormGroup = this.fb.group({
      aggIntervals: [ [] ],
      defaultAggInterval: [ null ],
    });
    this.timeintervalFormGroup.get('aggIntervals').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(selectedIntervalValues => this.setSelectedIntervals(selectedIntervalValues));
    this.timeintervalFormGroup.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updateView());
  }

  ngOnInit(): void {
    this.updateIntervalsList();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (isDefined(this.propagateChangeValue)) {
      this.propagateChange(this.propagateChangeValue);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.timeintervalFormGroup.disable({emitEvent: false});
    } else {
      this.timeintervalFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(intervalOptions: TimewindowAggIntervalOptions): void {
    this.modelValue = intervalOptions;
    this.rendered = true;
    this.timeintervalFormGroup.get('defaultAggInterval').patchValue(intervalOptions.defaultAggInterval, { emitEvent: false });
    this.setIntervals(intervalOptions.aggIntervals);
  }

  private updateIntervalsList() {
    this.allIntervals = this.timeService.getIntervals(this.min, this.max, this.useCalendarIntervals);
    this.allIntervalValues = this.allIntervals.map(interval => interval.value);
  }

  private setIntervals(intervals: Array<Interval>) {
    const selectedIntervals = !intervals?.length ? this.allIntervalValues : intervals;
    this.timeintervalFormGroup.get('aggIntervals').patchValue(
      selectedIntervals,
      {emitEvent: false});
    this.setSelectedIntervals(selectedIntervals);
  }

  private setSelectedIntervals(selectedIntervalValues: Array<Interval>) {
    if (!selectedIntervalValues.length || selectedIntervalValues.length === this.allIntervalValues.length) {
      this.selectedIntervals = this.allIntervals;
    } else {
      this.selectedIntervals = intervalValuesToTimeIntervals(selectedIntervalValues);
    }
    const defaultInterval: Interval = this.timeintervalFormGroup.get('defaultAggInterval').value;
    if (defaultInterval && !selectedIntervalValues.includes(defaultInterval)) {
      this.timeintervalFormGroup.get('defaultAggInterval').patchValue(null);
    }
  }

  private updateView() {
    if (!this.rendered) {
      return;
    }
    let selectedIntervals: Array<Interval>;
    const intervals: Array<Interval> = this.timeintervalFormGroup.get('aggIntervals').value;

    if (intervals.length < this.allIntervals.length) {
      selectedIntervals = intervals;
    } else {
      selectedIntervals = [];
    }
    this.modelValue = {
      aggIntervals: selectedIntervals,
      defaultAggInterval: this.timeintervalFormGroup.get('defaultAggInterval').value
    };
    this.propagateChange(this.modelValue);
  }

}
