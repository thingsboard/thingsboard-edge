///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component } from '@angular/core';
import {
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, mergeDeep } from '@core/utils';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  barChartWidgetDefaultSettings,
  BarChartWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-widget.models';

@Component({
  selector: 'tb-bar-chart-widget-settings',
  templateUrl: './bar-chart-widget-settings.component.html',
  styleUrls: []
})
export class BarChartWidgetSettingsComponent extends WidgetSettingsComponent {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  barChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.barChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep<BarChartWidgetSettings>({} as BarChartWidgetSettings, barChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.barChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

      barSettings: [settings.barSettings, []],

      axisMin: [settings.axisMin, []],
      axisMax: [settings.axisMax, []],
      axisTickLabelFont: [settings.axisTickLabelFont, []],
      axisTickLabelColor: [settings.axisTickLabelColor, []],

      animation: [settings.animation, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendValueFont: [settings.legendValueFont, []],
      legendValueColor: [settings.legendValueColor, []],

      showTooltip: [settings.showTooltip, []],
      tooltipValueType: [settings.tooltipValueType, []],
      tooltipValueDecimals: [settings.tooltipValueDecimals, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLegend: boolean = this.barChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.barChartWidgetSettingsForm.get('showTooltip').value;

    if (showLegend) {
      this.barChartWidgetSettingsForm.get('legendPosition').enable();
      this.barChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.barChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.barChartWidgetSettingsForm.get('legendValueFont').enable();
      this.barChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.barChartWidgetSettingsForm.get('legendPosition').disable();
      this.barChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.barChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.barChartWidgetSettingsForm.get('legendValueFont').disable();
      this.barChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.barChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.barChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.barChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.barChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.barChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.barChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.barChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.barChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.barChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.barChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
