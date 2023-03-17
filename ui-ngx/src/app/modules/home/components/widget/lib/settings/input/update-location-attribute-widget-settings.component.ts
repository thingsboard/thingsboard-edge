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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-update-location-attribute-widget-settings',
  templateUrl: './update-location-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateLocationAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateLocationAttributeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateLocationAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      latKeyName: 'latitude',
      lngKeyName: 'longitude',
      showGetLocation: true,
      enableHighAccuracy: false,

      showLabel: true,
      latLabel: '',
      lngLabel: '',
      inputFieldsAlignment: 'column',
      isLatRequired: true,
      isLngRequired: true,
      requiredErrorMessage: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateLocationAttributeWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      latKeyName: [settings.latKeyName, []],
      lngKeyName: [settings.lngKeyName, []],
      showGetLocation: [settings.showGetLocation, []],
      enableHighAccuracy: [settings.enableHighAccuracy, []],

      // Location fields settings

      showLabel: [settings.showLabel, []],
      latLabel: [settings.latLabel, []],
      lngLabel: [settings.lngLabel, []],
      inputFieldsAlignment: [settings.inputFieldsAlignment, []],
      isLatRequired: [settings.isLatRequired, []],
      isLngRequired: [settings.isLngRequired, []],
      requiredErrorMessage: [settings.requiredErrorMessage, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'isLatRequired', 'isLngRequired'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.updateLocationAttributeWidgetSettingsForm.get('showLabel').value;
    const isLatRequired: boolean = this.updateLocationAttributeWidgetSettingsForm.get('isLatRequired').value;
    const isLngRequired: boolean = this.updateLocationAttributeWidgetSettingsForm.get('isLngRequired').value;

    if (showLabel) {
      this.updateLocationAttributeWidgetSettingsForm.get('latLabel').enable();
      this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').enable();
    } else {
      this.updateLocationAttributeWidgetSettingsForm.get('latLabel').disable();
      this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').disable();
    }
    if (isLatRequired || isLngRequired) {
      this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').enable();
    } else {
      this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').disable();
    }
    this.updateLocationAttributeWidgetSettingsForm.get('latLabel').updateValueAndValidity({emitEvent});
    this.updateLocationAttributeWidgetSettingsForm.get('lngLabel').updateValueAndValidity({emitEvent});
    this.updateLocationAttributeWidgetSettingsForm.get('requiredErrorMessage').updateValueAndValidity({emitEvent});
  }
}
