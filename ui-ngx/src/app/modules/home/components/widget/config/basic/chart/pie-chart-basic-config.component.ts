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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { formatValue, isUndefined, mergeDeep } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  pieChartWidgetDefaultSettings,
  PieChartWidgetSettings
} from '@home/components/widget/lib/chart/pie-chart-widget.models';
import {
  pieChartLabelPositions,
  pieChartLabelPositionTranslations
} from '@home/components/widget/lib/chart/pie-chart.models';

@Component({
  selector: 'tb-pie-chart-basic-config',
  templateUrl: './pie-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class PieChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.pieChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.pieChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.pieChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  pieChartLabelPositions = pieChartLabelPositions;

  pieChartLabelPositionTranslationMap = pieChartLabelPositionTranslations;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  pieChartWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.pieChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
            { name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' },
            { name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30' }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: PieChartWidgetSettings = mergeDeep<PieChartWidgetSettings>({} as PieChartWidgetSettings,
      pieChartWidgetDefaultSettings, configData.config.settings as PieChartWidgetSettings);
    this.pieChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      series: [this.getSeries(configData.config.datasources), []],

      showLabel: [settings.showLabel, []],
      labelPosition: [settings.labelPosition, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      borderWidth: [settings.borderWidth, [Validators.min(0)]],
      borderColor: [settings.borderColor, []],

      radius: [settings.radius, [Validators.min(0), Validators.max(100)]],

      clockwise: [settings.clockwise, []],
      sortSeries: [settings.sortSeries, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],

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

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
    this.setSeries(config.series, this.widgetConfig.config.datasources);

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.showLabel = config.showLabel;
    this.widgetConfig.config.settings.labelPosition = config.labelPosition;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;

    this.widgetConfig.config.settings.borderWidth = config.borderWidth;
    this.widgetConfig.config.settings.borderColor = config.borderColor;

    this.widgetConfig.config.settings.radius = config.radius;

    this.widgetConfig.config.settings.clockwise = config.clockwise;
    this.widgetConfig.config.settings.sortSeries = config.sortSeries;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings.animation = config.animation;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendPosition = config.legendPosition;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;
    this.widgetConfig.config.settings.legendValueFont = config.legendValueFont;
    this.widgetConfig.config.settings.legendValueColor = config.legendValueColor;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.tooltipValueType = config.tooltipValueType;
    this.widgetConfig.config.settings.tooltipValueDecimals = config.tooltipValueDecimals;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;

    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showTitle', 'showTitleIcon', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showLabel: boolean = this.pieChartWidgetConfigForm.get('showLabel').value;
    const showTitle: boolean = this.pieChartWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.pieChartWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.pieChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.pieChartWidgetConfigForm.get('showTooltip').value;

    if (showLabel) {
      this.pieChartWidgetConfigForm.get('labelPosition').enable();
      this.pieChartWidgetConfigForm.get('labelFont').enable();
      this.pieChartWidgetConfigForm.get('labelColor').enable();
    } else {
      this.pieChartWidgetConfigForm.get('labelPosition').disable();
      this.pieChartWidgetConfigForm.get('labelFont').disable();
      this.pieChartWidgetConfigForm.get('labelColor').disable();
    }

    if (showTitle) {
      this.pieChartWidgetConfigForm.get('title').enable();
      this.pieChartWidgetConfigForm.get('titleFont').enable();
      this.pieChartWidgetConfigForm.get('titleColor').enable();
      this.pieChartWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.pieChartWidgetConfigForm.get('titleIcon').enable();
        this.pieChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.pieChartWidgetConfigForm.get('titleIcon').disable();
        this.pieChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.pieChartWidgetConfigForm.get('title').disable();
      this.pieChartWidgetConfigForm.get('titleFont').disable();
      this.pieChartWidgetConfigForm.get('titleColor').disable();
      this.pieChartWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.pieChartWidgetConfigForm.get('titleIcon').disable();
      this.pieChartWidgetConfigForm.get('iconColor').disable();
    }
    if (showLegend) {
      this.pieChartWidgetConfigForm.get('legendPosition').enable();
      this.pieChartWidgetConfigForm.get('legendLabelFont').enable();
      this.pieChartWidgetConfigForm.get('legendLabelColor').enable();
      this.pieChartWidgetConfigForm.get('legendValueFont').enable();
      this.pieChartWidgetConfigForm.get('legendValueColor').enable();
    } else {
      this.pieChartWidgetConfigForm.get('legendPosition').disable();
      this.pieChartWidgetConfigForm.get('legendLabelFont').disable();
      this.pieChartWidgetConfigForm.get('legendLabelColor').disable();
      this.pieChartWidgetConfigForm.get('legendValueFont').disable();
      this.pieChartWidgetConfigForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.pieChartWidgetConfigForm.get('tooltipValueType').enable();
      this.pieChartWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.pieChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.pieChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.pieChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.pieChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.pieChartWidgetConfigForm.get('tooltipValueType').disable();
      this.pieChartWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.pieChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.pieChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.pieChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.pieChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = series;
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableDataExport) || config.enableDataExport) {
      buttons.push('dataExport');
    }
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableDataExport = buttons.includes('dataExport');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

  private _valuePreviewFn(): string {
    const units: string = this.pieChartWidgetConfigForm.get('units').value;
    const decimals: number = this.pieChartWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.pieChartWidgetConfigForm.get('tooltipValueType').value;
    const decimals: number = this.pieChartWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.pieChartWidgetConfigForm.get('units').value;
      return formatValue(110, decimals, units, false);
    }
  }
}
