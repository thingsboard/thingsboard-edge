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

import { Directive, TemplateRef } from '@angular/core';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  DoughnutLayout,
  doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations,
  pieChartLabelPositions,
  pieChartLabelPositionTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { radarChartShapes, radarChartShapeTranslations } from '@home/components/widget/lib/chart/radar-chart.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class LatestChartWidgetSettingsComponent<S extends LatestChartWidgetSettings> extends WidgetSettingsComponent {

  doughnutLayouts = doughnutLayouts;

  doughnutLayoutTranslationMap = doughnutLayoutTranslations;

  doughnutHorizontal = false;

  doughnutLayoutImageMap: Map<DoughnutLayout, string>;

  pieChartLabelPositions = pieChartLabelPositions;

  pieChartLabelPositionTranslationMap = pieChartLabelPositionTranslations;

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

  latestChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  get doughnutTotalEnabled(): boolean {
    const layout: DoughnutLayout = this.latestChartWidgetSettingsForm.get('layout').value;
    return layout === DoughnutLayout.with_total;
  }

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.latestChartWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.doughnutHorizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.doughnutLayoutImageMap = this.doughnutHorizontal ? horizontalDoughnutLayoutImages : doughnutLayoutImages;
  }

  protected defaultSettings(): WidgetSettings {
    return this.defaultLatestChartSettings();
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.latestChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

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

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
    this.setupLatestChartControls(this.latestChartWidgetSettingsForm, settings);
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip'].concat(this.latestChartValidatorTriggers());
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showLegend: boolean = this.latestChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.latestChartWidgetSettingsForm.get('showTooltip').value;

    if (showLegend) {
      this.latestChartWidgetSettingsForm.get('legendPosition').enable();
      this.latestChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.latestChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.latestChartWidgetSettingsForm.get('legendValueFont').enable();
      this.latestChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.latestChartWidgetSettingsForm.get('legendPosition').disable();
      this.latestChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.latestChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.latestChartWidgetSettingsForm.get('legendValueFont').disable();
      this.latestChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.latestChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.latestChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
    this.updateLatestChartValidators(this.latestChartWidgetSettingsForm, emitEvent, trigger);
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {}

  protected latestChartValidatorTriggers(): string[] {
    return [];
  }

  protected updateLatestChartValidators(latestChartWidgetSettingsForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
  }

  protected abstract defaultLatestChartSettings(): S;

  public abstract latestChartConfigTemplate(): TemplateRef<any>;

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.latestChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }
}
