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
  defaultTimeSeriesChartYAxisSettings,
  getNextTimeSeriesYAxisId,
  TimeSeriesChartYAxes, TimeSeriesChartYAxisId,
  TimeSeriesChartYAxisSettings,
  timeSeriesChartYAxisValid,
  timeSeriesChartYAxisValidator
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { mergeDeep } from '@core/utils';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-y-axes-panel',
  templateUrl: './time-series-chart-y-axes-panel.component.html',
  styleUrls: ['./time-series-chart-y-axes-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartYAxesPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeSeriesChartYAxesPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartYAxesPanelComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  advanced = false;

  @Input()
  @coerceBoolean()
  supportsUnitConversion = false;

  @Output()
  axisRemoved = new EventEmitter<TimeSeriesChartYAxisId>();

  yAxesFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return this.axesFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.yAxesFormGroup = this.fb.group({
      axes: [this.fb.array([]), []]
    });
    this.yAxesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let axes: TimeSeriesChartYAxisSettings[] = this.yAxesFormGroup.get('axes').value;
        for (let i = 0; i < axes.length; i++) {
          axes[i].order = i;
        }
        if (axes) {
          axes = axes.filter(axis => timeSeriesChartYAxisValid(axis));
        }
        const yAxes: TimeSeriesChartYAxes = {};
        for (const axis of axes) {
          yAxes[axis.id] = axis;
        }
        this.propagateChange(yAxes);
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
      this.yAxesFormGroup.disable({emitEvent: false});
    } else {
      this.yAxesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TimeSeriesChartYAxes | undefined): void {
    const yAxes: TimeSeriesChartYAxes = value || {};
    if (!yAxes.default) {
      yAxes.default = mergeDeep({} as TimeSeriesChartYAxisSettings, defaultTimeSeriesChartYAxisSettings,
        {id: 'default', order: 0} as TimeSeriesChartYAxisSettings);
    }
    const yAxisSettingsList = Object.values(yAxes);
    yAxisSettingsList.sort((a1, a2) => a1.order - a2.order);
    this.yAxesFormGroup.setControl('axes', this.prepareAxesFormArray(yAxisSettingsList), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.yAxesFormGroup.valid;
    return valid ? null : {
      yAxes: {
        valid: false,
      },
    };
  }

  axisDrop(event: CdkDragDrop<string[]>) {
    const axesArray = this.yAxesFormGroup.get('axes') as UntypedFormArray;
    const axis = axesArray.at(event.previousIndex);
    axesArray.removeAt(event.previousIndex);
    axesArray.insert(event.currentIndex, axis);
  }

  axesFormArray(): UntypedFormArray {
    return this.yAxesFormGroup.get('axes') as UntypedFormArray;
  }

  trackByAxis(index: number, axisControl: AbstractControl): any {
    return axisControl;
  }

  removeAxis(index: number) {
    const axis =
      (this.yAxesFormGroup.get('axes') as UntypedFormArray).at(index).value as TimeSeriesChartYAxisSettings;
    (this.yAxesFormGroup.get('axes') as UntypedFormArray).removeAt(index);
    this.axisRemoved.emit(axis.id);
  }

  addAxis() {
    const axis = mergeDeep<TimeSeriesChartYAxisSettings>({} as TimeSeriesChartYAxisSettings,
      defaultTimeSeriesChartYAxisSettings);
    const axes: TimeSeriesChartYAxisSettings[] = this.yAxesFormGroup.get('axes').value;
    axis.id = getNextTimeSeriesYAxisId(axes);
    axis.order = axes.length;
    const axesArray = this.yAxesFormGroup.get('axes') as UntypedFormArray;
    const axisControl = this.fb.control(axis, [timeSeriesChartYAxisValidator]);
    axesArray.push(axisControl);
  }

  private prepareAxesFormArray(axes: TimeSeriesChartYAxisSettings[]): UntypedFormArray {
    const axesControls: Array<AbstractControl> = [];
    axes.forEach((axis) => {
      axesControls.push(this.fb.control(axis, [timeSeriesChartYAxisValidator]));
    });
    return this.fb.array(axesControls);
  }
}
