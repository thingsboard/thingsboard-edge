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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey, Datasource, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined } from '@core/utils';
import { cssSizeToStrSize, getDataKey, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  valueCartCardLayouts,
  valueChartCardDefaultSettings,
  valueChartCardLayoutImages,
  valueChartCardLayoutTranslations,
  ValueChartCardWidgetSettings
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';

@Component({
  selector: 'tb-value-chart-card-basic-config',
  templateUrl: './value-chart-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ValueChartCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.valueChartCardWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  valueChartCardLayouts = valueCartCardLayouts;

  valueChartCardLayoutTranslationMap = valueChartCardLayoutTranslations;
  valueChartCardLayoutImageMap = valueChartCardLayoutImages;

  valueChartCardWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.valueChartCardWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [
      { name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries, color: 'rgba(63, 82, 221, 1)'}
    ];
  }

  protected defaultLatestDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Latest', type: DataKeyType.timeseries}];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ValueChartCardWidgetSettings = {...valueChartCardDefaultSettings, ...(configData.config.settings || {})};
    const dataKey = getDataKey(configData.config.datasources);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.valueChartCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      showValue: [settings.showValue, []],
      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      chartColor: [dataKey?.color, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.showValue = config.showValue;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    const dataKey = getDataKey(this.widgetConfig.config.datasources);
    if (dataKey) {
      dataKey.color = config.chartColor;
      this.updateLatestValues(dataKey, this.widgetConfig.config.datasources);
    }

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showValue'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.valueChartCardWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.valueChartCardWidgetConfigForm.get('showIcon').value;
    const showValue: boolean = this.valueChartCardWidgetConfigForm.get('showValue').value;

    if (showTitle) {
      this.valueChartCardWidgetConfigForm.get('title').enable();
      this.valueChartCardWidgetConfigForm.get('titleFont').enable();
      this.valueChartCardWidgetConfigForm.get('titleColor').enable();
      this.valueChartCardWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.valueChartCardWidgetConfigForm.get('iconSize').enable();
        this.valueChartCardWidgetConfigForm.get('iconSizeUnit').enable();
        this.valueChartCardWidgetConfigForm.get('icon').enable();
        this.valueChartCardWidgetConfigForm.get('iconColor').enable();
      } else {
        this.valueChartCardWidgetConfigForm.get('iconSize').disable();
        this.valueChartCardWidgetConfigForm.get('iconSizeUnit').disable();
        this.valueChartCardWidgetConfigForm.get('icon').disable();
        this.valueChartCardWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.valueChartCardWidgetConfigForm.get('title').disable();
      this.valueChartCardWidgetConfigForm.get('titleFont').disable();
      this.valueChartCardWidgetConfigForm.get('titleColor').disable();
      this.valueChartCardWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.valueChartCardWidgetConfigForm.get('iconSize').disable();
      this.valueChartCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.valueChartCardWidgetConfigForm.get('icon').disable();
      this.valueChartCardWidgetConfigForm.get('iconColor').disable();
    }

    if (showValue) {
      this.valueChartCardWidgetConfigForm.get('units').enable();
      this.valueChartCardWidgetConfigForm.get('decimals').enable();
      this.valueChartCardWidgetConfigForm.get('valueFont').enable();
      this.valueChartCardWidgetConfigForm.get('valueColor').enable();
    } else {
      this.valueChartCardWidgetConfigForm.get('units').disable();
      this.valueChartCardWidgetConfigForm.get('decimals').disable();
      this.valueChartCardWidgetConfigForm.get('valueFont').disable();
      this.valueChartCardWidgetConfigForm.get('valueColor').disable();
    }
  }

  private updateLatestValues(sourceDataKey: DataKey, datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      let latestDataKeys = datasources[0].latestDataKeys;
      if (!latestDataKeys) {
        latestDataKeys = [];
        datasources[0].latestDataKeys = latestDataKeys;
      }
      let dataKey: DataKey;
      if (!latestDataKeys.length) {
        dataKey = {...sourceDataKey};
        latestDataKeys.push(dataKey);
      } else {
        dataKey = latestDataKeys[0];
        dataKey = {...dataKey, ...sourceDataKey};
        latestDataKeys[0] = dataKey;
      }
      dataKey.label = 'Latest';
      dataKey.units = null;
      dataKey.decimals = null;
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
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }
}
