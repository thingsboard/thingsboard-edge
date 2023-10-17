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
import {
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
import { formatValue, isUndefined } from '@core/utils';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  progressBarDefaultSettings,
  ProgressBarLayout,
  progressBarLayoutImages,
  progressBarLayouts,
  progressBarLayoutTranslations,
  ProgressBarWidgetSettings
} from '@home/components/widget/lib/cards/progress-bar-widget.models';

@Component({
  selector: 'tb-progress-bar-basic-config',
  templateUrl: './progress-bar-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ProgressBarBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.progressBarWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.progressBarWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  progressBarLayout = ProgressBarLayout;

  progressBarLayouts = progressBarLayouts;

  progressBarLayoutTranslationMap = progressBarLayoutTranslations;
  progressBarLayoutImageMap = progressBarLayoutImages;

  progressBarWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.progressBarWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    this.setupDefaultDatasource(configData, [{ name: 'humidity', label: 'humidity', type: DataKeyType.timeseries }]);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ProgressBarWidgetSettings = {...progressBarDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.progressBarWidgetConfigForm = this.fb.group({
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

      tickMin: [settings.tickMin, []],
      tickMax: [settings.tickMax, []],

      showTicks: [settings.showTicks, []],
      ticksFont: [settings.ticksFont, []],
      ticksColor: [settings.ticksColor, []],

      barColor: [settings.barColor, []],
      barBackground: [settings.barBackground, []],

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

    this.widgetConfig.config.settings.tickMin = config.tickMin;
    this.widgetConfig.config.settings.tickMax = config.tickMax;

    this.widgetConfig.config.settings.showTicks = config.showTicks;
    this.widgetConfig.config.settings.ticksFont = config.ticksFont;
    this.widgetConfig.config.settings.ticksColor = config.ticksColor;

    this.widgetConfig.config.settings.barColor = config.barColor;
    this.widgetConfig.config.settings.barBackground = config.barBackground;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showValue', 'showTicks', 'layout'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.progressBarWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.progressBarWidgetConfigForm.get('showIcon').value;
    const showValue: boolean = this.progressBarWidgetConfigForm.get('showValue').value;
    const showTicks: boolean = this.progressBarWidgetConfigForm.get('showTicks').value;
    const layout: ProgressBarLayout = this.progressBarWidgetConfigForm.get('layout').value;

    const ticksEnabled = layout === ProgressBarLayout.default;

    if (showTitle) {
      this.progressBarWidgetConfigForm.get('title').enable();
      this.progressBarWidgetConfigForm.get('titleFont').enable();
      this.progressBarWidgetConfigForm.get('titleColor').enable();
      this.progressBarWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.progressBarWidgetConfigForm.get('iconSize').enable();
        this.progressBarWidgetConfigForm.get('iconSizeUnit').enable();
        this.progressBarWidgetConfigForm.get('icon').enable();
        this.progressBarWidgetConfigForm.get('iconColor').enable();
      } else {
        this.progressBarWidgetConfigForm.get('iconSize').disable();
        this.progressBarWidgetConfigForm.get('iconSizeUnit').disable();
        this.progressBarWidgetConfigForm.get('icon').disable();
        this.progressBarWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.progressBarWidgetConfigForm.get('title').disable();
      this.progressBarWidgetConfigForm.get('titleFont').disable();
      this.progressBarWidgetConfigForm.get('titleColor').disable();
      this.progressBarWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.progressBarWidgetConfigForm.get('iconSize').disable();
      this.progressBarWidgetConfigForm.get('iconSizeUnit').disable();
      this.progressBarWidgetConfigForm.get('icon').disable();
      this.progressBarWidgetConfigForm.get('iconColor').disable();
    }

    if (showValue) {
      this.progressBarWidgetConfigForm.get('units').enable();
      this.progressBarWidgetConfigForm.get('decimals').enable();
      this.progressBarWidgetConfigForm.get('valueFont').enable();
      this.progressBarWidgetConfigForm.get('valueColor').enable();
    } else {
      this.progressBarWidgetConfigForm.get('units').disable();
      this.progressBarWidgetConfigForm.get('decimals').disable();
      this.progressBarWidgetConfigForm.get('valueFont').disable();
      this.progressBarWidgetConfigForm.get('valueColor').disable();
    }

    if (ticksEnabled) {
      this.progressBarWidgetConfigForm.get('showTicks').enable({emitEvent: false});
      if (showTicks) {
        this.progressBarWidgetConfigForm.get('ticksFont').enable();
        this.progressBarWidgetConfigForm.get('ticksColor').enable();
      } else {
        this.progressBarWidgetConfigForm.get('ticksFont').disable();
        this.progressBarWidgetConfigForm.get('ticksColor').disable();
      }
    } else {
      this.progressBarWidgetConfigForm.get('showTicks').disable({emitEvent: false});
      this.progressBarWidgetConfigForm.get('ticksFont').disable();
      this.progressBarWidgetConfigForm.get('ticksColor').disable();
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
    return formatValue(78, decimals, units, true);
  }
}
