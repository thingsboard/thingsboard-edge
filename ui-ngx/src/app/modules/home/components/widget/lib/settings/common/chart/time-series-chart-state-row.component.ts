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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartStateSettings,
  TimeSeriesChartStateSourceType,
  timeSeriesStateSourceTypes,
  timeSeriesStateSourceTypeTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-state-row',
  templateUrl: './time-series-chart-state-row.component.html',
  styleUrls: ['./time-series-chart-state-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartStateRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartStateRowComponent implements ControlValueAccessor, OnInit {

  TimeSeriesChartStateSourceType = TimeSeriesChartStateSourceType;

  timeSeriesStateSourceTypes = timeSeriesStateSourceTypes;

  timeSeriesStateSourceTypeTranslations = timeSeriesStateSourceTypeTranslations;

  @Input()
  disabled: boolean;

  @Output()
  stateRemoved = new EventEmitter();

  stateFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartStateSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.stateFormGroup = this.fb.group({
      label: [null, []],
      value: [null, [Validators.required]],
      sourceType: [null, [Validators.required]],
      sourceValue: [null, [Validators.required]],
      sourceRangeFrom: [null, []],
      sourceRangeTo: [null, []]
    });
    this.stateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.stateFormGroup.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stateFormGroup.disable({emitEvent: false});
    } else {
      this.stateFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartStateSettings): void {
    this.modelValue = value;
    this.stateFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const sourceType: TimeSeriesChartStateSourceType = this.stateFormGroup.get('sourceType').value;
    if (sourceType === TimeSeriesChartStateSourceType.constant) {
      this.stateFormGroup.get('sourceValue').enable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeFrom').disable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeTo').disable({emitEvent: false});
    } else if (sourceType === TimeSeriesChartStateSourceType.range) {
      this.stateFormGroup.get('sourceValue').disable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeFrom').enable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeTo').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.stateFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}
