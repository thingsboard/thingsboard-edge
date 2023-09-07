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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey, Datasource, WidgetConfig } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';

@Component({
  selector: 'tb-alarms-table-basic-config',
  templateUrl: './alarms-table-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class AlarmsTableBasicConfigComponent extends BasicWidgetConfigComponent {

  public get alarmSource(): Datasource {
    const datasources: Datasource[] = this.alarmsTableWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  alarmsTableWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmsTableWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.alarmsTableWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      alarmFilterConfig: [configData.config.alarmFilterConfig, []],
      datasources: [[configData.config.alarmSource], []],
      columns: [this.getColumns(configData.config.alarmSource), []],
      showTitle: [configData.config.showTitle, []],
      title: [configData.config.settings?.alarmsTitle, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      color: [configData.config.color, []],
      backgroundColor: [configData.config.backgroundColor, []],
      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.alarmFilterConfig = config.alarmFilterConfig;
    this.widgetConfig.config.alarmSource = config.datasources[0];
    this.setColumns(config.columns, this.widgetConfig.config.alarmSource);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.alarmsTitle = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;
    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.color = config.color;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.alarmsTableWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.alarmsTableWidgetConfigForm.get('showTitleIcon').value;
    if (showTitle) {
      this.alarmsTableWidgetConfigForm.get('title').enable();
      this.alarmsTableWidgetConfigForm.get('titleFont').enable();
      this.alarmsTableWidgetConfigForm.get('titleColor').enable();
      this.alarmsTableWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.alarmsTableWidgetConfigForm.get('titleIcon').enable();
        this.alarmsTableWidgetConfigForm.get('iconColor').enable();
      } else {
        this.alarmsTableWidgetConfigForm.get('titleIcon').disable();
        this.alarmsTableWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.alarmsTableWidgetConfigForm.get('title').disable();
      this.alarmsTableWidgetConfigForm.get('titleFont').disable();
      this.alarmsTableWidgetConfigForm.get('titleColor').disable();
      this.alarmsTableWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.alarmsTableWidgetConfigForm.get('titleIcon').disable();
      this.alarmsTableWidgetConfigForm.get('iconColor').disable();
    }
    this.alarmsTableWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('titleColor').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.alarmsTableWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.alarmsTableWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
  }

  private getColumns(alarmSource?: Datasource): DataKey[] {
    if (alarmSource) {
      return alarmSource.dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], alarmSource?: Datasource) {
    if (alarmSource) {
      alarmSource.dataKeys = columns;
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.settings?.enableSearch) || config.settings?.enableSearch) {
      buttons.push('search');
    }
    if (isUndefined(config.settings?.enableFilter) || config.settings?.enableFilter) {
      buttons.push('filter');
    }
    if (isUndefined(config.settings?.enableSelectColumnDisplay) || config.settings?.enableSelectColumnDisplay) {
      buttons.push('columnsToDisplay');
    }
    if (isUndefined(config.enableDataExport) || config.enableDataExport) {
      buttons.push('dataExport');
    }
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.settings.enableSearch = buttons.includes('search');
    config.settings.enableFilter = buttons.includes('filter');
    config.settings.enableSelectColumnDisplay = buttons.includes('columnsToDisplay');
    config.enableDataExport = buttons.includes('dataExport');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
