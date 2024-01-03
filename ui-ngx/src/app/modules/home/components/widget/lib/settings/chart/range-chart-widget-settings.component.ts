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

import { Component, Injector } from '@angular/core';
import {
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import { rangeChartDefaultSettings } from '@home/components/widget/lib/chart/range-chart-widget.models';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-range-chart-widget-settings',
  templateUrl: './range-chart-widget-settings.component.html',
  styleUrls: []
})
export class RangeChartWidgetSettingsComponent extends WidgetSettingsComponent {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  rangeChartWidgetSettingsForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.rangeChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...rangeChartDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.rangeChartWidgetSettingsForm = this.fb.group({
      dataZoom: [settings.dataZoom, []],
      rangeColors: [settings.rangeColors, []],
      outOfRangeColor: [settings.outOfRangeColor, []],
      fillArea: [settings.fillArea, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],

      showTooltip: [settings.showTooltip, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLegend: boolean = this.rangeChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.rangeChartWidgetSettingsForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.rangeChartWidgetSettingsForm.get('tooltipShowDate').value;

    if (showLegend) {
      this.rangeChartWidgetSettingsForm.get('legendPosition').enable();
      this.rangeChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.rangeChartWidgetSettingsForm.get('legendLabelColor').enable();
    } else {
      this.rangeChartWidgetSettingsForm.get('legendPosition').disable();
      this.rangeChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.rangeChartWidgetSettingsForm.get('legendLabelColor').disable();
    }

    if (showTooltip) {
      this.rangeChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.rangeChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.rangeChartWidgetSettingsForm.get('tooltipShowDate').enable({emitEvent: false});
      this.rangeChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.rangeChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.rangeChartWidgetSettingsForm.get('tooltipDateFormat').enable();
        this.rangeChartWidgetSettingsForm.get('tooltipDateFont').enable();
        this.rangeChartWidgetSettingsForm.get('tooltipDateColor').enable();
      } else {
        this.rangeChartWidgetSettingsForm.get('tooltipDateFormat').disable();
        this.rangeChartWidgetSettingsForm.get('tooltipDateFont').disable();
        this.rangeChartWidgetSettingsForm.get('tooltipDateColor').disable();
      }
    } else {
      this.rangeChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipShowDate').disable({emitEvent: false});
      this.rangeChartWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipDateFont').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipDateColor').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.rangeChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _tooltipValuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.rangeChartWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

}
