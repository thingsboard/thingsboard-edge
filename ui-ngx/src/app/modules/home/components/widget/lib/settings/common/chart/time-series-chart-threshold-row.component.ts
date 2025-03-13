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
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartThresholdsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-thresholds-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKey, Datasource, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { deepClone } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  ValueSourceTypes,
  ValueSourceType,
  ValueSourceTypeTranslation
} from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-threshold-row',
  templateUrl: './time-series-chart-threshold-row.component.html',
  styleUrls: ['./time-series-chart-threshold-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartThresholdRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  DataKeyType = DataKeyType;

  DatasourceType = DatasourceType;

  TimeSeriesChartThresholdType = ValueSourceType;

  timeSeriesThresholdTypes = ValueSourceTypes;

  timeSeriesThresholdTypeTranslations = ValueSourceTypeTranslation;

  get aliasController(): IAliasController {
    return this.thresholdsPanel.aliasController;
  }

  get dataKeyCallbacks(): DataKeysCallbacks {
    return this.thresholdsPanel.dataKeyCallbacks;
  }

  get datasource(): Datasource {
    return this.thresholdsPanel.datasource;
  }

  get widgetConfig(): WidgetConfig {
    return this.thresholdsPanel.widgetConfig;
  }

  @Input()
  disabled: boolean;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  @coerceBoolean()
  hideYAxis = false;

  @Output()
  thresholdRemoved = new EventEmitter();

  thresholdFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartThreshold;

  latestKeyFormControl: UntypedFormControl;

  entityKeyFormControl: UntypedFormControl;

  thresholdSettingsFormControl: UntypedFormControl;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private thresholdsPanel: TimeSeriesChartThresholdsPanelComponent,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.thresholdFormGroup = this.fb.group({
      type: [null, []],
      value: [null, [Validators.required]],
      entityAlias: [null, [Validators.required]],
      yAxisId: [null, [Validators.required]],
      lineColor: [null, []],
      units: [null, []],
      decimals: [null, []]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    this.thresholdSettingsFormControl = this.fb.control(null);
    this.thresholdFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.latestKeyFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.entityKeyFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.thresholdSettingsFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((thresholdSettings: Partial<TimeSeriesChartThreshold>) => {
      this.modelValue = {...this.modelValue, ...thresholdSettings};
      this.thresholdFormGroup.patchValue(
        {
          yAxisId: this.modelValue.yAxisId,
          units: this.modelValue.units,
          decimals: this.modelValue.decimals,
          lineColor: this.modelValue.lineColor
        },
        {emitEvent: false});
      this.propagateChange(this.modelValue);
    });
    this.thresholdFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['yAxisIds'].includes(propName)) {
          if (this.modelValue?.yAxisId &&
            !this.yAxisIds.includes(this.modelValue.yAxisId)) {
            this.thresholdFormGroup.patchValue({yAxisId: 'default'}, {emitEvent: true});
          }
        }
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
    if (isDisabled) {
      this.thresholdFormGroup.disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
      this.thresholdSettingsFormControl.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
      this.thresholdSettingsFormControl.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartThreshold): void {
    this.modelValue = value;
    this.thresholdFormGroup.patchValue(
      {
        type: value.type,
        value: value.value,
        entityAlias: value.entityAlias,
        yAxisId: value.yAxisId,
        lineColor: value.lineColor,
        units: value.units,
        decimals: value.decimals,
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }
    this.thresholdSettingsFormControl.patchValue(deepClone(this.modelValue),
      {emitEvent: false});
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const type: ValueSourceType = this.thresholdFormGroup.get('type').value;
    if (type === ValueSourceType.constant) {
      this.thresholdFormGroup.get('value').enable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.latestKey) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.enable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.entity) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').enable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value = this.thresholdFormGroup.value;
    this.modelValue.type = value.type;
    this.modelValue.value = value.value;
    this.modelValue.entityAlias = value.entityAlias;
    this.modelValue.yAxisId = value.yAxisId;
    this.modelValue.lineColor = value.lineColor;
    this.modelValue.units = value.units;
    this.modelValue.decimals = value.decimals;
    if (value.type === ValueSourceType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      this.modelValue.latestKey = latestKey?.name;
      this.modelValue.latestKeyType = (latestKey?.type as any);
    } else if (value.type === ValueSourceType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      this.modelValue.entityKey = entityKey?.name;
      this.modelValue.entityKeyType = (entityKey?.type as any);
    }
    this.thresholdSettingsFormControl.patchValue(deepClone(this.modelValue),
      {emitEvent: false});
    this.propagateChange(this.modelValue);
  }
}
