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
import { DatasourceType, WidgetConfig, widgetType, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isUndefined } from '@core/utils';
import { getAlarmFilterConfig, setAlarmFilterConfig } from '@shared/models/widget-settings.models';
import { UtilsService } from '@core/services/utils.service';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import {
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';

@Component({
  selector: 'tb-alarm-count-basic-config',
  templateUrl: './alarm-count-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class AlarmCountBasicConfigComponent extends BasicWidgetConfigComponent {

  alarmSearchStatus = AlarmSearchStatus;

  alarmCountWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmCountWidgetConfigForm;
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    let datasources = configData.config.datasources;
    if (!datasources || !datasources.length) {
      datasources = [{}];
      configData.config.datasources = datasources;
    }
    datasources[0].type = DatasourceType.alarmCount;
    datasources[0].alarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
    datasources[0].dataKeys = [this.utils.createKey({name: 'count'}, DataKeyType.count)];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: CountWidgetSettings = {...countDefaultSettings(true), ...(configData.config.settings || {})};
    this.alarmCountWidgetConfigForm = this.fb.group({
      alarmFilterConfig: [getAlarmFilterConfig(configData.config.datasources), []],

      settings: [settings, []],

      backgroundColor: [configData.config.backgroundColor, []],
      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setAlarmFilterConfig(config.alarmFilterConfig, this.widgetConfig.config.datasources);

    this.widgetConfig.config.settings = {...(this.widgetConfig.config.settings || {}), ...config.settings};

    this.widgetConfig.config.backgroundColor = config.backgroundColor;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
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
}
