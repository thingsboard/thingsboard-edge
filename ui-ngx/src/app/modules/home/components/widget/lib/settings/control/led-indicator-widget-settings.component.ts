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
import { WidgetService } from '@core/http/widget.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-led-indicator-widget-settings',
  templateUrl: './led-indicator-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class LedIndicatorWidgetSettingsComponent extends WidgetSettingsComponent {

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  get targetDeviceAliasId(): string {
    const aliasIds = this.widget?.config?.targetDeviceAliasIds;
    if (aliasIds && aliasIds.length) {
      return aliasIds[0];
    }
    return null;
  }

  dataKeyType = DataKeyType;

  ledIndicatorWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.ledIndicatorWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      ledColor: 'green',
      initialValue: false,
      performCheckStatus: true,
      checkStatusMethod: 'checkStatus',
      retrieveValueMethod: 'attribute',
      valueAttribute: 'value',
      parseValueFunction: 'return data ? true : false;',
      requestTimeout: 500,
      requestPersistent: false,
      persistentPollingInterval: 5000
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.ledIndicatorWidgetSettingsForm = this.fb.group({

      // Common settings

      title: [settings.title, []],
      ledColor: [settings.ledColor, []],

      // Value settings

      initialValue: [settings.initialValue, []],

      // --> Check status settings

      performCheckStatus: [settings.performCheckStatus, []],
      retrieveValueMethod: [settings.retrieveValueMethod, []],
      checkStatusMethod: [settings.checkStatusMethod, [Validators.required]],
      valueAttribute: [settings.valueAttribute, [Validators.required]],
      parseValueFunction: [settings.parseValueFunction, []],

      // RPC settings

      requestTimeout: [settings.requestTimeout, [Validators.min(0)]],

      // --> Persistent RPC settings

      requestPersistent: [settings.requestPersistent, []],
      persistentPollingInterval: [settings.persistentPollingInterval, [Validators.min(1000)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['performCheckStatus', 'requestPersistent'];
  }

  protected updateValidators(emitEvent: boolean): void {
    const performCheckStatus: boolean = this.ledIndicatorWidgetSettingsForm.get('performCheckStatus').value;
    const requestPersistent: boolean = this.ledIndicatorWidgetSettingsForm.get('requestPersistent').value;
    if (performCheckStatus) {
      this.ledIndicatorWidgetSettingsForm.get('valueAttribute').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestTimeout').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestPersistent').enable({emitEvent: false});
      if (requestPersistent) {
        this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
      } else {
        this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
      }
    } else {
      this.ledIndicatorWidgetSettingsForm.get('valueAttribute').enable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestTimeout').disable({emitEvent});
      this.ledIndicatorWidgetSettingsForm.get('requestPersistent').disable({emitEvent: false});
      this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.ledIndicatorWidgetSettingsForm.get('valueAttribute').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('checkStatusMethod').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('requestTimeout').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('requestPersistent').updateValueAndValidity({emitEvent: false});
    this.ledIndicatorWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}
