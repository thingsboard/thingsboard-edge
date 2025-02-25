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
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  AxisPosition,
  timeSeriesAxisPositionTranslations,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  TimeSeriesChartAxisSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-panel.component';
import { deepClone } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-y-axis-row',
  templateUrl: './time-series-chart-y-axis-row.component.html',
  styleUrls: ['./time-series-chart-y-axis-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartYAxisRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartYAxisRowComponent implements ControlValueAccessor, OnInit {

  axisPositions = [AxisPosition.left, AxisPosition.right];

  timeSeriesAxisPositionTranslations = timeSeriesAxisPositionTranslations;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  advanced = false;

  @Output()
  axisRemoved = new EventEmitter();

  axisFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartYAxisSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.axisFormGroup = this.fb.group({
      label: [null, []],
      position: [null, []],
      units: [null, []],
      decimals: [null, []],
      min: [null, []],
      max: [null, []],
      show: [null, []]
    });
    this.axisFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.axisFormGroup.get('show').valueChanges.pipe(
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
      this.axisFormGroup.disable({emitEvent: false});
    } else {
      this.axisFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartYAxisSettings): void {
    this.modelValue = value;
    this.axisFormGroup.patchValue(
      {
        label: value.label,
        position: value.position,
        units: value.units,
        decimals: value.decimals,
        min: value.min,
        max: value.max,
        show: value.show,
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  editAxis($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        axisType: 'yAxis',
        panelTitle: this.translate.instant('widgets.time-series-chart.axis.y-axis-settings'),
        axisSettings: deepClone(this.modelValue),
        advanced: this.advanced
      };
      const yAxisSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, TimeSeriesChartAxisSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      yAxisSettingsPanelPopover.tbComponentRef.instance.popover = yAxisSettingsPanelPopover;
      yAxisSettingsPanelPopover.tbComponentRef.instance.axisSettingsApplied.subscribe((yAxisSettings) => {
        yAxisSettingsPanelPopover.hide();
        this.modelValue = {...this.modelValue, ...yAxisSettings};
        this.axisFormGroup.patchValue(
          {
            label: this.modelValue.label,
            position: this.modelValue.position,
            units: this.modelValue.units,
            decimals: this.modelValue.decimals,
            min: this.modelValue.min,
            max: this.modelValue.max,
            show: this.modelValue.show
          },
          {emitEvent: false});
        this.updateValidators();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateValidators() {
    const show: boolean = this.axisFormGroup.get('show').value;
    if (show) {
      this.axisFormGroup.get('label').enable({emitEvent: false});
      this.axisFormGroup.get('position').enable({emitEvent: false});
      this.axisFormGroup.get('units').enable({emitEvent: false});
      this.axisFormGroup.get('decimals').enable({emitEvent: false});
    } else {
      this.axisFormGroup.get('label').disable({emitEvent: false});
      this.axisFormGroup.get('position').disable({emitEvent: false});
      this.axisFormGroup.get('units').disable({emitEvent: false});
      this.axisFormGroup.get('decimals').disable({emitEvent: false});
    }
  }

  private updateModel() {
    const value = this.axisFormGroup.value;
    this.modelValue.label = value.label;
    this.modelValue.position = value.position;
    this.modelValue.units = value.units;
    this.modelValue.decimals = value.decimals;
    this.modelValue.min = value.min;
    this.modelValue.max = value.max;
    this.modelValue.show = value.show;
    this.propagateChange(this.modelValue);
  }
}
