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
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';

@Component({
  selector: 'tb-gateway-events-widget-settings',
  templateUrl: './gateway-events-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class GatewayEventsWidgetSettingsComponent extends WidgetSettingsComponent {

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  gatewayEventsWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayEventsWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      eventsTitle: 'Gateway events form title',
      eventsReg: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayEventsWidgetSettingsForm = this.fb.group({
      eventsTitle: [settings.eventsTitle, []],
      eventsReg: [settings.eventsReg, []]
    });
  }

  removeEventFilter(eventFilter: string) {
    const eventsFilter: string[] = this.gatewayEventsWidgetSettingsForm.get('eventsReg').value;
    const index = eventsFilter.indexOf(eventFilter);
    if (index > -1) {
      eventsFilter.splice(index, 1);
      this.gatewayEventsWidgetSettingsForm.get('eventsReg').setValue(eventsFilter);
      this.gatewayEventsWidgetSettingsForm.get('eventsReg').markAsDirty();
    }
  }

  addEventFilterFromInput(event: MatChipInputEvent) {
    const value = event.value;
    if ((value || '').trim()) {
      const eventsFilter: string[] = this.gatewayEventsWidgetSettingsForm.get('eventsReg').value;
      const index = eventsFilter.indexOf(value);
      if (index === -1) {
        eventsFilter.push(value);
        this.gatewayEventsWidgetSettingsForm.get('eventsReg').setValue(eventsFilter);
        this.gatewayEventsWidgetSettingsForm.get('eventsReg').markAsDirty();
      }
      event.chipInput.clear();
    }
  }
}
