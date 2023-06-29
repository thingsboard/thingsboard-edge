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
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-simple-card-basic-config',
  templateUrl: './simple-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class SimpleCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.simpleCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.simpleCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  simpleCardWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.simpleCardWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    this.setupDefaultDatasource(configData, [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }]);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    this.simpleCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [{
        useDashboardTimewindow: configData.config.useDashboardTimewindow,
        displayTimewindow: configData.config.useDashboardTimewindow,
        timewindow: configData.config.timewindow
      }, []],
      datasources: [configData.config.datasources, []],
      label: [this.getDataKeyLabel(configData.config.datasources), []],
      labelPosition: [configData.config.settings?.labelPosition, []],
      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
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
    this.setDataKeyLabel(config.label, this.widgetConfig.config.datasources);
    this.widgetConfig.config.actions = config.actions;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    this.widgetConfig.config.color = config.color;
    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.labelPosition = config.labelPosition;
    return this.widgetConfig;
  }

  private getDataKeyLabel(datasources?: Datasource[]): string {
    if (datasources && datasources.length) {
      const dataKeys = datasources[0].dataKeys;
      if (dataKeys && dataKeys.length) {
        return dataKeys[0].label;
      }
    }
    return '';
  }

  private setDataKeyLabel(label: string, datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      const dataKeys = datasources[0].dataKeys;
      if (dataKeys && dataKeys.length) {
        dataKeys[0].label = label;
      }
    }
  }

}
