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
  radarChartWidgetDefaultSettings,
  RadarChartWidgetSettings
} from '@home/components/widget/lib/chart/radar-chart-widget.models';
import { radarChartShapes, radarChartShapeTranslations } from '@home/components/widget/lib/chart/radar-chart.models';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations
} from '@home/components/widget/lib/chart/chart.models';

@Component({
  selector: 'tb-radar-chart-widget-settings',
  templateUrl: './radar-chart-widget-settings.component.html',
  styleUrls: []
})
export class RadarChartWidgetSettingsComponent extends WidgetSettingsComponent {

  radarChartShapes = radarChartShapes;

  radarChartShapeTranslations = radarChartShapeTranslations;

  chartLineTypes = chartLineTypes;

  chartLineTypeTranslations = chartLineTypeTranslations;

  chartShapes = chartShapes;

  chartShapeTranslations = chartShapeTranslations;

  chartLabelPositions = chartLabelPositions;

  chartLabelPositionTranslations = chartLabelPositionTranslations;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  radarChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.radarChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep<RadarChartWidgetSettings>({} as RadarChartWidgetSettings, radarChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.radarChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

      shape: [settings.shape, []],
      color: [settings.color, []],
      showLine: [settings.showLine, []],
      lineType: [settings.lineType, []],
      lineWidth: [settings.lineWidth, [Validators.min(0)]],
      showPoints: [settings.showPoints, []],
      pointShape: [settings.pointShape, []],
      pointSize: [settings.pointSize, [Validators.min(0)]],
      showLabel: [settings.showLabel, []],
      labelPosition: [settings.labelPosition, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],
      fillAreaSettings: [settings.fillAreaSettings, []],

      axisShowLabel: [settings.axisShowLabel, []],
      axisLabelFont: [settings.axisLabelFont, []],
      axisShowTickLabels: [settings.axisShowTickLabels, []],
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
    return ['showLine', 'showPoints', 'showLabel', 'axisShowLabel',
      'axisShowTickLabels', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLine: boolean = this.radarChartWidgetSettingsForm.get('showLine').value;
    const showPoints: boolean = this.radarChartWidgetSettingsForm.get('showPoints').value;
    const showLabel: boolean = this.radarChartWidgetSettingsForm.get('showLabel').value;
    const axisShowLabel: boolean = this.radarChartWidgetSettingsForm.get('axisShowLabel').value;
    const axisShowTickLabels: boolean = this.radarChartWidgetSettingsForm.get('axisShowTickLabels').value;
    const showLegend: boolean = this.radarChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.radarChartWidgetSettingsForm.get('showTooltip').value;

    if (showLine) {
      this.radarChartWidgetSettingsForm.get('lineType').enable();
      this.radarChartWidgetSettingsForm.get('lineWidth').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('lineType').disable();
      this.radarChartWidgetSettingsForm.get('lineWidth').disable();
    }

    if (showPoints) {
      this.radarChartWidgetSettingsForm.get('pointShape').enable();
      this.radarChartWidgetSettingsForm.get('pointSize').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('pointShape').disable();
      this.radarChartWidgetSettingsForm.get('pointSize').disable();
    }

    if (showLabel) {
      this.radarChartWidgetSettingsForm.get('labelPosition').enable();
      this.radarChartWidgetSettingsForm.get('labelFont').enable();
      this.radarChartWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('labelPosition').disable();
      this.radarChartWidgetSettingsForm.get('labelFont').disable();
      this.radarChartWidgetSettingsForm.get('labelColor').disable();
    }

    if (axisShowLabel) {
      this.radarChartWidgetSettingsForm.get('axisLabelFont').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('axisLabelFont').disable();
    }

    if (axisShowTickLabels) {
      this.radarChartWidgetSettingsForm.get('axisTickLabelFont').enable();
      this.radarChartWidgetSettingsForm.get('axisTickLabelColor').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('axisTickLabelFont').disable();
      this.radarChartWidgetSettingsForm.get('axisTickLabelColor').disable();
    }

    if (showLegend) {
      this.radarChartWidgetSettingsForm.get('legendPosition').enable();
      this.radarChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.radarChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.radarChartWidgetSettingsForm.get('legendValueFont').enable();
      this.radarChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('legendPosition').disable();
      this.radarChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.radarChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.radarChartWidgetSettingsForm.get('legendValueFont').disable();
      this.radarChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.radarChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.radarChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.radarChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.radarChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.radarChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.radarChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.radarChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.radarChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.radarChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.radarChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.radarChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.radarChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.radarChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.radarChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
