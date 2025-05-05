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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { unreadNotificationDefaultSettings } from '@home/components/widget/lib/cards/unread-notification-widget.models';

@Component({
  selector: 'tb-unread-notification-widget-settings',
  templateUrl: './unread-notification-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UnreadNotificationWidgetSettingsComponent extends WidgetSettingsComponent {

  unreadNotificationWidgetSettingsForm: UntypedFormGroup;

  countPreviewFn = this._countPreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.unreadNotificationWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return unreadNotificationDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.unreadNotificationWidgetSettingsForm = this.fb.group({
      maxNotificationDisplay: [settings?.maxNotificationDisplay, [Validators.required, Validators.min(1)]],
      showCounter: [settings?.showCounter, []],
      counterValueFont: [settings?.counterValueFont, []],
      counterValueColor: [settings?.counterValueColor, []],
      counterColor: [settings?.counterColor, []],

      enableViewAll: [settings?.enableViewAll, []],
      enableFilter: [settings?.enableFilter, []],
      enableMarkAsRead: [settings?.enableMarkAsRead, []],

      background: [settings?.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showCounter'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showCounter: boolean = this.unreadNotificationWidgetSettingsForm.get('showCounter').value;

    if (showCounter) {
      this.unreadNotificationWidgetSettingsForm.get('counterValueFont').enable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterValueColor').enable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterColor').enable({emitEvent});
    } else {
      this.unreadNotificationWidgetSettingsForm.get('counterValueFont').disable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterValueColor').disable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterColor').disable({emitEvent});
    }
  }

  private _countPreviewFn(): string {
    return this.unreadNotificationWidgetSettingsForm.get('maxNotificationDisplay').value?.toString() || '6';
  }

}
