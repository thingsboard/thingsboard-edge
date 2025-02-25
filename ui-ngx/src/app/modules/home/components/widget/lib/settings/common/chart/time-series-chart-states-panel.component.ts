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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import {
  TimeSeriesChartStateSettings,
  TimeSeriesChartStateSourceType,
  timeSeriesChartStateValid,
  timeSeriesChartStateValidator
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-states-panel',
  templateUrl: './time-series-chart-states-panel.component.html',
  styleUrls: ['./time-series-chart-states-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartStatesPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeSeriesChartStatesPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartStatesPanelComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  statesFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.statesFormGroup = this.fb.group({
      states: [this.fb.array([]), []]
    });
    this.statesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let states: TimeSeriesChartStateSettings[] = this.statesFormGroup.get('states').value;
        if (states) {
          states = states.filter(s => timeSeriesChartStateValid(s));
        }
        this.propagateChange(states);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.statesFormGroup.disable({emitEvent: false});
    } else {
      this.statesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TimeSeriesChartStateSettings[] | undefined): void {
    const states = value || [];
    this.statesFormGroup.setControl('states', this.prepareStatesFormArray(states), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.statesFormGroup.valid;
    return valid ? null : {
      states: {
        valid: false,
      },
    };
  }

  statesFormArray(): UntypedFormArray {
    return this.statesFormGroup.get('states') as UntypedFormArray;
  }

  trackByState(index: number, stateControl: AbstractControl): any {
    return stateControl;
  }

  removeState(index: number) {
    (this.statesFormGroup.get('states') as UntypedFormArray).removeAt(index);
  }

  addState() {
    const state: TimeSeriesChartStateSettings = {
      label: '',
      value: 0,
      sourceType: TimeSeriesChartStateSourceType.constant
    };
    const statesArray = this.statesFormGroup.get('states') as UntypedFormArray;
    const stateControl = this.fb.control(state, [timeSeriesChartStateValidator]);
    statesArray.push(stateControl);
  }

  private prepareStatesFormArray(states: TimeSeriesChartStateSettings[] | undefined): UntypedFormArray {
    const statesControls: Array<AbstractControl> = [];
    if (states) {
      states.forEach((state) => {
        statesControls.push(this.fb.control(state, [timeSeriesChartStateValidator]));
      });
    }
    return this.fb.array(statesControls);
  }

}
