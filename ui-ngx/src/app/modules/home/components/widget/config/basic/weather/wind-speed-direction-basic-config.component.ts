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
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isDefinedAndNotNull, isUndefined } from '@core/utils';
import { cssSizeToStrSize, getDataKey, resolveCssSize, updateDataKeys } from '@shared/models/widget-settings.models';
import {
  windSpeedDirectionDefaultSettings,
  WindSpeedDirectionLayout,
  windSpeedDirectionLayoutImages,
  windSpeedDirectionLayouts,
  windSpeedDirectionLayoutTranslations,
  WindSpeedDirectionWidgetSettings
} from '@home/components/widget/lib/weather/wind-speed-direction-widget.models';

@Component({
  selector: 'tb-wind-speed-direction-basic-config',
  templateUrl: './wind-speed-direction-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class WindSpeedDirectionBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.windSpeedDirectionWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.windSpeedDirectionWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.windSpeedDirectionWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  windSpeedDirectionLayouts = windSpeedDirectionLayouts;

  windSpeedDirectionLayoutTranslationMap = windSpeedDirectionLayoutTranslations;
  windSpeedDirectionLayoutImageMap = windSpeedDirectionLayoutImages;

  windSpeedDirectionWidgetConfigForm: UntypedFormGroup;

  centerValuePreviewFn = this._centerValuePreviewFn.bind(this);

  get majorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetConfigForm.get('layout').value;
    return [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
  }

  get minorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetConfigForm.get('layout').value;
    return layout === WindSpeedDirectionLayout.advanced;
  }

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.windSpeedDirectionWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'direction', label: 'Wind Direction', type: DataKeyType.timeseries },
      { name: 'speed', label: 'Wind Speed', type: DataKeyType.timeseries,
        units: 'm/s', decimals: 1 }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: WindSpeedDirectionWidgetSettings = {...windSpeedDirectionDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.windSpeedDirectionWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      windDirectionKey: [getDataKey(configData.config.datasources, 0), [Validators.required]],

      centerValueKey: [getDataKey(configData.config.datasources, 1), []],
      centerValueFont: [settings.centerValueFont, []],
      centerValueColor: [settings.centerValueColor, []],

      layout: [settings.layout, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      ticksColor: [settings.ticksColor, []],
      directionalNamesElseDegrees: [settings.directionalNamesElseDegrees, []],

      majorTicksFont: [settings.majorTicksFont, []],
      majorTicksColor: [settings.majorTicksColor, []],

      minorTicksFont: [settings.minorTicksFont, []],
      minorTicksColor: [settings.minorTicksColor, []],

      arrowColor: [settings.arrowColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    const dataKeys: DataKey[] = [];
    if (config.windDirectionKey) {
      dataKeys.push(config.windDirectionKey);
      if (config.centerValueKey) {
        dataKeys.push(config.centerValueKey);
      }
    }

    updateDataKeys(this.widgetConfig.config.datasources, dataKeys);

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

    this.widgetConfig.config.settings.centerValueFont = config.centerValueFont;
    this.widgetConfig.config.settings.centerValueColor = config.centerValueColor;

    this.widgetConfig.config.settings.ticksColor = config.ticksColor;
    this.widgetConfig.config.settings.directionalNamesElseDegrees = config.directionalNamesElseDegrees;

    this.widgetConfig.config.settings.majorTicksFont = config.majorTicksFont;
    this.widgetConfig.config.settings.majorTicksColor = config.majorTicksColor;

    this.widgetConfig.config.settings.minorTicksFont = config.minorTicksFont;
    this.widgetConfig.config.settings.minorTicksColor = config.minorTicksColor;

    this.widgetConfig.config.settings.arrowColor = config.arrowColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['layout', 'showTitle', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetConfigForm.get('layout').value;
    const showTitle: boolean = this.windSpeedDirectionWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.windSpeedDirectionWidgetConfigForm.get('showIcon').value;

    const majorTicksFontEnabled = [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
    const minorTicksFontEnabled = layout === WindSpeedDirectionLayout.advanced;

    if (showTitle) {
      this.windSpeedDirectionWidgetConfigForm.get('title').enable();
      this.windSpeedDirectionWidgetConfigForm.get('titleFont').enable();
      this.windSpeedDirectionWidgetConfigForm.get('titleColor').enable();
      this.windSpeedDirectionWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.windSpeedDirectionWidgetConfigForm.get('iconSize').enable();
        this.windSpeedDirectionWidgetConfigForm.get('iconSizeUnit').enable();
        this.windSpeedDirectionWidgetConfigForm.get('icon').enable();
        this.windSpeedDirectionWidgetConfigForm.get('iconColor').enable();
      } else {
        this.windSpeedDirectionWidgetConfigForm.get('iconSize').disable();
        this.windSpeedDirectionWidgetConfigForm.get('iconSizeUnit').disable();
        this.windSpeedDirectionWidgetConfigForm.get('icon').disable();
        this.windSpeedDirectionWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.windSpeedDirectionWidgetConfigForm.get('title').disable();
      this.windSpeedDirectionWidgetConfigForm.get('titleFont').disable();
      this.windSpeedDirectionWidgetConfigForm.get('titleColor').disable();
      this.windSpeedDirectionWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.windSpeedDirectionWidgetConfigForm.get('iconSize').disable();
      this.windSpeedDirectionWidgetConfigForm.get('iconSizeUnit').disable();
      this.windSpeedDirectionWidgetConfigForm.get('icon').disable();
      this.windSpeedDirectionWidgetConfigForm.get('iconColor').disable();
    }

    if (majorTicksFontEnabled) {
      this.windSpeedDirectionWidgetConfigForm.get('majorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetConfigForm.get('majorTicksFont').disable();
    }

    if (minorTicksFontEnabled) {
      this.windSpeedDirectionWidgetConfigForm.get('minorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetConfigForm.get('minorTicksFont').disable();
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

  private _centerValuePreviewFn(): string {
    const centerValueDataKey: DataKey = this.windSpeedDirectionWidgetConfigForm.get('centerValueKey').value;
    if (centerValueDataKey) {
      let units: string = this.widgetConfig.config.units;
      let decimals: number = this.widgetConfig.config.decimals;
      if (isDefinedAndNotNull(centerValueDataKey?.decimals)) {
        decimals = centerValueDataKey.decimals;
      }
      if (centerValueDataKey?.units) {
        units = centerValueDataKey.units;
      }
      return formatValue(25, decimals, units, true);
    } else {
      return '225°';
    }
  }

}
