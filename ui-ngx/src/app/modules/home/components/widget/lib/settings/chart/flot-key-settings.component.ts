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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { ChartType, TbFlotKeySettings, TbFlotKeyThreshold } from '@home/components/widget/lib/flot-widget.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { WidgetService } from '@core/http/widget.service';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { IAliasController } from 'src/app/core/api/widget-api.models';

export function flotDataKeyDefaultSettings(chartType: ChartType): TbFlotKeySettings {
  const settings: TbFlotKeySettings = {
    // Common settings
    hideDataByDefault: false,
    disableDataHiding: false,
    removeFromLegend: false,
    excludeFromStacking: false,

    // Line settings
    showLines: chartType === 'graph',
    lineWidth: 1,
    fillLines: false,

    // Points settings
    showPoints: false,
    showPointsLineWidth: 5,
    showPointsRadius: 3,
    showPointShape: 'circle',
    pointShapeFormatter: 'var size = radius * Math.sqrt(Math.PI) / 2;\n' +
      'ctx.moveTo(x - size, y - size);\n' +
      'ctx.lineTo(x + size, y + size);\n' +
      'ctx.moveTo(x - size, y + size);\n' +
      'ctx.lineTo(x + size, y - size);',

    // Tooltip settings
    tooltipValueFormatter: '',

    // Y axis settings
    showSeparateAxis: false,
    axisTitle: '',
    axisMin: null,
    axisMax: null,
    axisPosition: 'left',

    // --> Y axis tick labels settings
    axisTickSize: null,
    axisTickDecimals: null,
    axisTicksFormatter: '',

    // Thresholds
    thresholds: [],

    // Comparison settings
    comparisonSettings: {
      showValuesForComparison: true,
      comparisonValuesLabel: '',
      color: ''
    }
  };
  return settings;
}

@Component({
  selector: 'tb-flot-key-settings',
  templateUrl: './flot-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FlotKeySettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => FlotKeySettingsComponent),
      multi: true,
    }
  ]
})
export class FlotKeySettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  chartType: ChartType;

  @Input()
  aliasController: IAliasController;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TbFlotKeySettings;

  private propagateChange = null;

  public flotKeySettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.flotKeySettingsFormGroup = this.fb.group({

      // Common settings

      hideDataByDefault: [false, []],
      disableDataHiding: [false, []],
      removeFromLegend: [false, []],
      excludeFromStacking: [false, []],

      // Line settings

      showLines: [this.chartType === 'graph', []],
      lineWidth: [1, [Validators.min(0)]],
      fillLines: [false, []],

      // Points settings

      showPoints: [false, []],
      showPointsLineWidth: [5, [Validators.min(0)]],
      showPointsRadius: [3, [Validators.min(0)]],
      showPointShape: ['circle', []],
      pointShapeFormatter: ['', []],

      // Tooltip settings

      tooltipValueFormatter: ['', []],

      // Y axis settings

      showSeparateAxis: [false, []],
      axisTitle: [null, []],
      axisMin: [null, []],
      axisMax: [null, []],
      axisPosition: ['left', []],

      // --> Y axis tick labels settings

      axisTickSize: [null, [Validators.min(0)]],
      axisTickDecimals: [null, [Validators.min(0)]],
      axisTicksFormatter: ['', []],

      // Thresholds

      thresholds: this.fb.array([]),

      // Comparison settings

      comparisonSettings: this.fb.group({
        showValuesForComparison: [true, []],
        comparisonValuesLabel: ['', []],
        color: ['', []]
      })

    });

    this.flotKeySettingsFormGroup.get('showLines').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    this.flotKeySettingsFormGroup.get('showPoints').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    this.flotKeySettingsFormGroup.get('comparisonSettings.showValuesForComparison').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    this.flotKeySettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.flotKeySettingsFormGroup.disable({emitEvent: false});
    } else {
      this.flotKeySettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TbFlotKeySettings): void {
    const thresholds = value?.thresholds;
    this.modelValue = value;
    this.flotKeySettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    const thresholdsControls: Array<AbstractControl> = [];
    if (thresholds && thresholds.length) {
      thresholds.forEach((threshold) => {
        thresholdsControls.push(this.fb.control(threshold, []));
      });
    }
    this.flotKeySettingsFormGroup.setControl('thresholds', this.fb.array(thresholdsControls), {emitEvent: false});
    this.updateValidators(false);
  }

  validate(c: FormControl) {
    return (this.flotKeySettingsFormGroup.valid) ? null : {
      flotKeySettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TbFlotKeySettings = this.flotKeySettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showLines: boolean = this.flotKeySettingsFormGroup.get('showLines').value;
    const showPoints: boolean = this.flotKeySettingsFormGroup.get('showPoints').value;
    const showValuesForComparison: boolean = this.flotKeySettingsFormGroup.get('comparisonSettings.showValuesForComparison').value;

    if (showLines) {
      this.flotKeySettingsFormGroup.get('lineWidth').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('fillLines').enable({emitEvent});
    } else {
      this.flotKeySettingsFormGroup.get('lineWidth').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('fillLines').disable({emitEvent});
    }

    if (showPoints) {
      this.flotKeySettingsFormGroup.get('showPointsLineWidth').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointsRadius').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointShape').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('pointShapeFormatter').enable({emitEvent});
    } else {
      this.flotKeySettingsFormGroup.get('showPointsLineWidth').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointsRadius').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('showPointShape').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('pointShapeFormatter').disable({emitEvent});
    }

    if (showValuesForComparison) {
      this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').enable({emitEvent});
      this.flotKeySettingsFormGroup.get('comparisonSettings.color').enable({emitEvent});
    } else {
      this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').disable({emitEvent});
      this.flotKeySettingsFormGroup.get('comparisonSettings.color').disable({emitEvent});
    }

    this.flotKeySettingsFormGroup.get('lineWidth').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('fillLines').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointsLineWidth').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointsRadius').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('showPointShape').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('pointShapeFormatter').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('comparisonSettings.comparisonValuesLabel').updateValueAndValidity({emitEvent: false});
    this.flotKeySettingsFormGroup.get('comparisonSettings.color').updateValueAndValidity({emitEvent: false});
  }

  thresholdsFormArray(): FormArray {
    return this.flotKeySettingsFormGroup.get('thresholds') as FormArray;
  }

  public trackByThreshold(index: number, thresholdControl: AbstractControl): any {
    return thresholdControl;
  }

  public removeThreshold(index: number) {
    (this.flotKeySettingsFormGroup.get('thresholds') as FormArray).removeAt(index);
  }

  public addThreshold() {
    const threshold: TbFlotKeyThreshold = {
      thresholdValueSource: 'predefinedValue',
      thresholdEntityAlias: null,
      thresholdAttribute: null,
      thresholdValue: null,
      lineWidth: null,
      color: null
    };
    const thresholdsArray = this.flotKeySettingsFormGroup.get('thresholds') as FormArray;
    const thresholdControl = this.fb.control(threshold, []);
    (thresholdControl as any).new = true;
    thresholdsArray.push(thresholdControl);
    this.flotKeySettingsFormGroup.updateValueAndValidity();
  }

  thresholdDrop(event: CdkDragDrop<string[]>) {
    const thresholdsArray = this.flotKeySettingsFormGroup.get('thresholds') as FormArray;
    const threshold = thresholdsArray.at(event.previousIndex);
    thresholdsArray.removeAt(event.previousIndex);
    thresholdsArray.insert(event.currentIndex, threshold);
  }

}
