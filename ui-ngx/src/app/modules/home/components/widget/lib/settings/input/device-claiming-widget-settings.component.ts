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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-device-claiming-widget-settings',
  templateUrl: './device-claiming-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class DeviceClaimingWidgetSettingsComponent extends WidgetSettingsComponent {

  deviceClaimingWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.deviceClaimingWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      labelClaimButon: '',
      deviceSecret: false,
      showLabel: true,
      deviceLabel: '',
      secretKeyLabel: '',
      successfulClaimDevice: '',
      deviceNotFound: '',
      failedClaimDevice: '',
      requiredErrorDevice: '',
      requiredErrorSecretKey: '',
      relateDevice: false,
      relateDirection: 'from',
      relateType: 'Contains'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.deviceClaimingWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      labelClaimButon: [settings.labelClaimButon, []],
      deviceSecret: [settings.deviceSecret, []],

      // Labels settings

      showLabel: [settings.showLabel, []],
      deviceLabel: [settings.deviceLabel, []],
      secretKeyLabel: [settings.secretKeyLabel, []],

      // Message settings

      successfulClaimDevice: [settings.successfulClaimDevice, []],
      deviceNotFound: [settings.deviceNotFound, []],
      failedClaimDevice: [settings.failedClaimDevice, []],
      requiredErrorDevice: [settings.requiredErrorDevice, []],
      requiredErrorSecretKey: [settings.requiredErrorSecretKey, []],

      // Relations settings

      relateDevice: [settings.relateDevice, []],
      relateDirection: [settings.relateDirection, []],
      relateType: [settings.relateType, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['deviceSecret', 'showLabel', 'relateDevice'];
  }

  protected updateValidators(emitEvent: boolean) {
    const deviceSecret: boolean = this.deviceClaimingWidgetSettingsForm.get('deviceSecret').value;
    const showLabel: boolean = this.deviceClaimingWidgetSettingsForm.get('showLabel').value;
    const relateDevice: boolean = this.deviceClaimingWidgetSettingsForm.get('relateDevice').value;
    if (deviceSecret) {
      if (showLabel) {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').enable();
      } else {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
      }
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').disable();
      this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
    }
    if (showLabel) {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').disable();
    }
    if (relateDevice) {
      this.deviceClaimingWidgetSettingsForm.get('relateDirection').enable();
      this.deviceClaimingWidgetSettingsForm.get('relateType').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('relateDirection').disable();
      this.deviceClaimingWidgetSettingsForm.get('relateType').disable();
    }
    this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('deviceLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('relateDirection').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('relateType').updateValueAndValidity({emitEvent});
  }

}
