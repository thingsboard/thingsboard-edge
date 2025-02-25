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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { QuickTimeInterval, QuickTimeIntervalTranslationMap } from '@shared/models/time/time.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-quick-time-interval',
  templateUrl: './quick-time-interval.component.html',
  styleUrls: ['./quick-time-interval.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => QuickTimeIntervalComponent),
      multi: true
    }
  ]
})
export class QuickTimeIntervalComponent implements OnInit, ControlValueAccessor, OnChanges {

  private allIntervals = Object.values(QuickTimeInterval) as QuickTimeInterval[];

  modelValue: QuickTimeInterval;
  timeIntervalTranslationMap = QuickTimeIntervalTranslationMap;

  rendered = false;

  @Input()
  @coerceBoolean()
  displayLabel = true;

  @Input() disabled: boolean;

  @Input() onlyCurrentInterval = false;

  @Input()
  allowedIntervals: Array<QuickTimeInterval>

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  intervals: Array<QuickTimeInterval>;

  private allAvailableIntervals: Array<QuickTimeInterval>;

  quickIntervalFormGroup: FormGroup;

  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder) {
    this.quickIntervalFormGroup = this.fb.group({
      interval: [ null ]
    });
    this.quickIntervalFormGroup.get('interval').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      let modelValue;
      if (!value) {
        modelValue = null;
      } else {
        modelValue = value;
      }
      this.updateView(modelValue);
    });
  }

  ngOnInit(): void {
    this.allAvailableIntervals = this.getAllAvailableIntervals();
    this.intervals = this.allowedIntervals?.length ? this.allowedIntervals : this.allAvailableIntervals;
  }

  ngOnChanges({allowedIntervals}: SimpleChanges): void {
    if (allowedIntervals && !allowedIntervals.firstChange && !isEqual(allowedIntervals.currentValue, allowedIntervals.previousValue)) {
      this.intervals = this.allowedIntervals?.length ? this.allowedIntervals : this.allAvailableIntervals;
      const currentInterval: QuickTimeInterval = this.quickIntervalFormGroup.get('interval').value;
      if (currentInterval && !this.intervals.includes(currentInterval)) {
        this.quickIntervalFormGroup.get('interval').patchValue(this.intervals[0], {emitEvent: true});
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.quickIntervalFormGroup.disable({emitEvent: false});
    } else {
      this.quickIntervalFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: QuickTimeInterval): void {
    let interval: QuickTimeInterval;
    if (value && this.allowedIntervals?.length && !this.allowedIntervals.includes(value)) {
      interval = this.allowedIntervals[0];
    } else {
      interval = value;
    }
    this.modelValue = interval;
    this.quickIntervalFormGroup.get('interval').patchValue(interval, {emitEvent: false});
  }

  updateView(value: QuickTimeInterval | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private getAllAvailableIntervals() {
    if (this.onlyCurrentInterval) {
      return this.allIntervals.filter(interval => interval.startsWith('CURRENT_'));
    }
    return this.allIntervals;
  }
}
