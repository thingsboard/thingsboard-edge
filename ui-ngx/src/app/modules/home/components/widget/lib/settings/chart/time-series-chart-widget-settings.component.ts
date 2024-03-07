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

import { Component, Injector } from '@angular/core';
import {
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, mergeDeep } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  barChartWithLabelsDefaultSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { EChartsTooltipTrigger } from '../../chart/echarts-widget.models';
import {
  timeSeriesChartWidgetDefaultSettings, TimeSeriesChartWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-widget.models';

@Component({
  selector: 'tb-time-series-chart-widget-settings',
  templateUrl: './time-series-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeSeriesChartWidgetSettingsComponent extends WidgetSettingsComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  EChartsTooltipTrigger = EChartsTooltipTrigger;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  timeSeriesChartWidgetSettingsForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeSeriesChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep({} as TimeSeriesChartWidgetSettings, timeSeriesChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeSeriesChartWidgetSettingsForm = this.fb.group({

      thresholds: [settings.thresholds, []],

      dataZoom: [settings.dataZoom, []],
      stack: [settings.stack, []],

      yAxis: [settings.yAxis, []],
      xAxis: [settings.xAxis, []],

      showLegend: [settings.showLegend, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendConfig: [settings.legendConfig, []],

      showTooltip: [settings.showTooltip, []],
      tooltipTrigger: [settings.tooltipTrigger, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipDateInterval: [settings.tooltipDateInterval, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLegend: boolean = this.timeSeriesChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.timeSeriesChartWidgetSettingsForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').value;

    if (showLegend) {
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').enable();
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').disable();
    }

    if (showTooltip) {
      this.timeSeriesChartWidgetSettingsForm.get('tooltipTrigger').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').enable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').enable();
      } else {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').disable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _tooltipValuePreviewFn(): string {
    return formatValue(22, 0, '°C', false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

}
