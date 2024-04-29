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
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';

@Component({
  selector: 'tb-polar-area-chart-widget-settings',
  templateUrl: './polar-area-chart-widget-settings.component.html',
  styleUrls: []
})
export class PolarAreaChartWidgetSettingsComponent extends WidgetSettingsComponent {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  polarAreaChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.polarAreaChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep<PolarAreaChartWidgetSettings>({} as PolarAreaChartWidgetSettings, polarAreaChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.polarAreaChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

      barSettings: [settings.barSettings, []],

      axisMin: [settings.axisMin, []],
      axisMax: [settings.axisMax, []],
      axisTickLabelFont: [settings.axisTickLabelFont, []],
      axisTickLabelColor: [settings.axisTickLabelColor, []],
      angleAxisStartAngle: [settings.angleAxisStartAngle, [Validators.min(0), Validators.max(360)]],

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
    const showLegend: boolean = this.polarAreaChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.polarAreaChartWidgetSettingsForm.get('showTooltip').value;

    if (showLegend) {
      this.polarAreaChartWidgetSettingsForm.get('legendPosition').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.polarAreaChartWidgetSettingsForm.get('legendPosition').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
