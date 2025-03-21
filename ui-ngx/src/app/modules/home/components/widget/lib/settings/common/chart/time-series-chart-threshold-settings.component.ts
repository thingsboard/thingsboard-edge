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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetConfig } from '@shared/models/widget.models';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartThresholdSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings-panel.component';

@Component({
  selector: 'tb-time-series-chart-threshold-settings',
  templateUrl: './time-series-chart-threshold-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartThresholdSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartThresholdSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  widgetConfig: WidgetConfig;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  @coerceBoolean()
  hideYAxis = false;

  @Input()
  @coerceBoolean()
  boxButton = false;

  @Input()
  icon = 'settings';

  @Input()
  title = 'widgets.time-series-chart.threshold.threshold-settings';

  private modelValue: Partial<TimeSeriesChartThreshold>;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Partial<TimeSeriesChartThreshold>): void {
    this.modelValue = value;
  }

  openThresholdSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        thresholdSettings: deepClone(this.modelValue),
        panelTitle: this.title,
        widgetConfig: this.widgetConfig,
        hideYAxis: this.hideYAxis,
        yAxisIds: this.yAxisIds
      };
      const thresholdSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, TimeSeriesChartThresholdSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], false, null,
        ctx,
        {},
        {}, {}, true);
      thresholdSettingsPanelPopover.tbComponentRef.instance.popover = thresholdSettingsPanelPopover;
      thresholdSettingsPanelPopover.tbComponentRef.instance.thresholdSettingsApplied.subscribe((thresholdSettings) => {
        thresholdSettingsPanelPopover.hide();
        this.modelValue = thresholdSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
