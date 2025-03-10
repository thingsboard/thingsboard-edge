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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TargetDevice, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { ValueType } from '@shared/models/constants';
import {
  commandButtonDefaultSettings,
  CommandButtonWidgetSettings
} from '@home/components/widget/lib/button/command-button-widget.models';

@Component({
  selector: 'tb-command-button-basic-config',
  templateUrl: './command-button-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class CommandButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.commandButtonWidgetConfigForm.get('targetDevice').value;
  }

  valueType = ValueType;

  commandButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.commandButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: CommandButtonWidgetSettings = {...commandButtonDefaultSettings, ...(configData.config.settings || {})};
    this.commandButtonWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      onClickState: [settings.onClickState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []],

      borderRadius: [configData.config.borderRadius, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {

    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.onClickState = config.onClickState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.appearance = config.appearance;

    this.widgetConfig.config.borderRadius = config.borderRadius;

    return this.widgetConfig;
  }
}
