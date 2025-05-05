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
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import { toggleButtonDefaultSettings } from '@home/components/widget/lib/button/toggle-button-widget.models';

type ButtonAppearanceType = 'checked' | 'unchecked';

@Component({
  selector: 'tb-toggle-button-widget-settings',
  templateUrl: './toggle-button-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class ToggleButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  valueType = ValueType;

  buttonAppearanceType: ButtonAppearanceType = 'checked';

  toggleButtonWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.toggleButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return toggleButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.toggleButtonWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      checkState: [settings.checkState, []],
      uncheckState: [settings.uncheckState, []],
      disabledState: [settings.disabledState, []],

      autoScale: [settings.autoScale, []],
      horizontalFill: [settings.horizontalFill, []],
      verticalFill: [settings.verticalFill, []],

      checkedAppearance: [settings.checkedAppearance, []],
      uncheckedAppearance: [settings.uncheckedAppearance, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['autoScale'];
  }

  protected updateValidators(emitEvent: boolean) {
    const autoScale: boolean = this.toggleButtonWidgetSettingsForm.get('autoScale').value;

    if (autoScale) {
      this.toggleButtonWidgetSettingsForm.get('horizontalFill').disable();
      this.toggleButtonWidgetSettingsForm.get('verticalFill').disable();
    } else {
      this.toggleButtonWidgetSettingsForm.get('horizontalFill').enable();
      this.toggleButtonWidgetSettingsForm.get('verticalFill').enable();
    }
  }
}
