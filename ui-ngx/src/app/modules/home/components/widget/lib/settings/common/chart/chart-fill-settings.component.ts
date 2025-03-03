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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ChartFillSettings,
  ChartFillType,
  chartFillTypes,
  chartFillTypeTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-chart-fill-settings',
  templateUrl: './chart-fill-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ChartFillSettingsComponent),
      multi: true
    }
  ]
})
export class ChartFillSettingsComponent implements OnInit, ControlValueAccessor {

  chartFillTypes = chartFillTypes;

  chartFillTypeTranslationMap: Map<ChartFillType, string> = new Map<ChartFillType, string>([]);

  ChartFillType = ChartFillType;

  @Input()
  disabled: boolean;

  @Input()
  titleText = 'widgets.chart.fill';

  @Input()
  fillNoneTitle = 'widgets.chart.fill-type-none';

  private modelValue: ChartFillSettings;

  private propagateChange = null;

  public fillSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.fillSettingsFormGroup = this.fb.group({
      type: [null, []],
      opacity: [null, [Validators.min(0), Validators.max(1)]],
      gradient: this.fb.group({
        start: [null, [Validators.min(0), Validators.max(100)]],
        end: [null, [Validators.min(0), Validators.max(100)]]
      })
    });
    this.fillSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.fillSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    for (const type of chartFillTypes) {
      let translation: string;
      if (type === ChartFillType.none) {
        translation = this.fillNoneTitle;
      } else {
        translation = chartFillTypeTranslations.get(type);
      }
      this.chartFillTypeTranslationMap.set(type, translation);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.fillSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.fillSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ChartFillSettings): void {
    this.modelValue = value;
    this.fillSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const type: ChartFillType = this.fillSettingsFormGroup.get('type').value;
    if (type === ChartFillType.none) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === ChartFillType.opacity) {
      this.fillSettingsFormGroup.get('opacity').enable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === ChartFillType.gradient) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value: ChartFillSettings = this.fillSettingsFormGroup.getRawValue();
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
