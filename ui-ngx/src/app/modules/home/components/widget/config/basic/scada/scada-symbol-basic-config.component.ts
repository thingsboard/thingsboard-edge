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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TargetDevice, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  scadaSymbolWidgetDefaultSettings,
  ScadaSymbolWidgetSettings
} from '@home/components/widget/lib/scada/scada-symbol-widget.models';
import { isUndefined } from '@core/utils';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-scada-symbol-basic-config',
  templateUrl: './scada-symbol-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ScadaSymbolBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.scadaSymbolWidgetConfigForm.get('targetDevice').value;
  }

  scadaSymbolWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.scadaSymbolWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ScadaSymbolWidgetSettings = {...scadaSymbolWidgetDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.scadaSymbolWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],
      scadaSymbolUrl: [settings.scadaSymbolUrl, []],
      scadaSymbolObjectSettings: [settings.scadaSymbolObjectSettings, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.scadaSymbolUrl = config.scadaSymbolUrl;
    this.widgetConfig.config.settings.scadaSymbolObjectSettings = config.scadaSymbolObjectSettings;
    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.scadaSymbolWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.scadaSymbolWidgetConfigForm.get('showIcon').value;

    if (showTitle) {
      this.scadaSymbolWidgetConfigForm.get('title').enable();
      this.scadaSymbolWidgetConfigForm.get('titleFont').enable();
      this.scadaSymbolWidgetConfigForm.get('titleColor').enable();
      this.scadaSymbolWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.scadaSymbolWidgetConfigForm.get('iconSize').enable();
        this.scadaSymbolWidgetConfigForm.get('iconSizeUnit').enable();
        this.scadaSymbolWidgetConfigForm.get('icon').enable();
        this.scadaSymbolWidgetConfigForm.get('iconColor').enable();
      } else {
        this.scadaSymbolWidgetConfigForm.get('iconSize').disable();
        this.scadaSymbolWidgetConfigForm.get('iconSizeUnit').disable();
        this.scadaSymbolWidgetConfigForm.get('icon').disable();
        this.scadaSymbolWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.scadaSymbolWidgetConfigForm.get('title').disable();
      this.scadaSymbolWidgetConfigForm.get('titleFont').disable();
      this.scadaSymbolWidgetConfigForm.get('titleColor').disable();
      this.scadaSymbolWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.scadaSymbolWidgetConfigForm.get('iconSize').disable();
      this.scadaSymbolWidgetConfigForm.get('iconSizeUnit').disable();
      this.scadaSymbolWidgetConfigForm.get('icon').disable();
      this.scadaSymbolWidgetConfigForm.get('iconColor').disable();
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }
}
