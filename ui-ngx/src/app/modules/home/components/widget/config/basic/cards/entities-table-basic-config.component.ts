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
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation, WidgetConfig
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import { getTimewindowConfig } from '@home/components/widget/config/timewindow-config-panel.component';

@Component({
  selector: 'tb-entities-table-basic-config',
  templateUrl: './entities-table-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class EntitiesTableBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.entitiesTableWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.entitiesTableWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.entitiesTableWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  entitiesTableWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.entitiesTableWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    this.setupDefaultDatasource(configData, [{ name: 'name', type: DataKeyType.entityField }]);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.entitiesTableWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      columns: [this.getColumns(configData.config.datasources), []],
      showTitle: [configData.config.showTitle, []],
      title: [configData.config.settings?.entitiesTitle, []],
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
    this.widgetConfig.config.useDashboardTimewindow = config.timewindowConfig.useDashboardTimewindow;
    this.widgetConfig.config.displayTimewindow = config.timewindowConfig.displayTimewindow;
    this.widgetConfig.config.timewindow = config.timewindowConfig.timewindow;
    this.widgetConfig.config.datasources = config.datasources;
    this.setColumns(config.columns, this.widgetConfig.config.datasources);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.entitiesTitle = config.title;
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
    const showTitle: boolean = this.entitiesTableWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.entitiesTableWidgetConfigForm.get('showTitleIcon').value;
    if (showTitle) {
      this.entitiesTableWidgetConfigForm.get('title').enable();
      this.entitiesTableWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.entitiesTableWidgetConfigForm.get('titleIcon').enable();
        this.entitiesTableWidgetConfigForm.get('iconColor').enable();
      } else {
        this.entitiesTableWidgetConfigForm.get('titleIcon').disable();
        this.entitiesTableWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.entitiesTableWidgetConfigForm.get('title').disable();
      this.entitiesTableWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.entitiesTableWidgetConfigForm.get('titleIcon').disable();
      this.entitiesTableWidgetConfigForm.get('iconColor').disable();
    }
    this.entitiesTableWidgetConfigForm.get('title').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('showTitleIcon').updateValueAndValidity({emitEvent: false});
    this.entitiesTableWidgetConfigForm.get('titleIcon').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
  }

  private getColumns(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setColumns(columns: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = columns;
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.settings?.enableSearch) || config.settings?.enableSearch) {
      buttons.push('search');
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
    config.settings.enableSelectColumnDisplay = buttons.includes('columnsToDisplay');
    config.enableDataExport = buttons.includes('dataExport');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

}
