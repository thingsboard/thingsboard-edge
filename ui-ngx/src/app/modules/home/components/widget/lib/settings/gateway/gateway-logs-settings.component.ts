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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-gateway-logs-settings',
  templateUrl: './gateway-logs-settings.component.html',
  styleUrls: ['../widget-settings.scss']
})
export class GatewayLogsSettingsComponent extends WidgetSettingsComponent {

  gatewayLogSettingForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayLogSettingForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      isConnectorLog: false,
      connectorLogState: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayLogSettingForm = this.fb.group({
      isConnectorLog: [false, []],
      connectorLogState: ['', Validators.required]
    });
  }

  protected validatorTriggers(): string[] {
    return ['isConnectorLog'];
  }

  protected updateValidators(emitEvent: boolean) {
    const isConnectorLog: boolean = this.gatewayLogSettingForm.get('isConnectorLog').value;
    if (isConnectorLog) {
      this.gatewayLogSettingForm.get('connectorLogState').enable({emitEvent});
    } else {
      this.gatewayLogSettingForm.get('connectorLogState').disable({emitEvent});
    }
  }
}
