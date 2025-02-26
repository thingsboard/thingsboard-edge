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
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { TimeSeriesChartGridSettings } from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-grid-settings',
  templateUrl: './time-series-chart-grid-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartGridSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartGridSettingsComponent implements OnInit, ControlValueAccessor {

  settingsExpanded = false;

  @Input()
  disabled: boolean;

  private modelValue: TimeSeriesChartGridSettings;

  private propagateChange = null;

  public gridSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.gridSettingsFormGroup = this.fb.group({
      show: [null, []],
      backgroundColor: [null, []],
      borderWidth: [null, [Validators.min(0)]],
      borderColor: [null, []],
    });
    this.gridSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.gridSettingsFormGroup.get('show').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.gridSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.gridSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartGridSettings): void {
    this.modelValue = value;
    this.gridSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.gridSettingsFormGroup.get('show').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  private updateValidators() {
    const show: boolean = this.gridSettingsFormGroup.get('show').value;
    if (show) {
      this.gridSettingsFormGroup.enable({emitEvent: false});
    } else {
      this.gridSettingsFormGroup.disable({emitEvent: false});
      this.gridSettingsFormGroup.get('show').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.gridSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
