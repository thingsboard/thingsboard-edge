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
  selector: 'tb-qrcode-widget-settings',
  templateUrl: './qrcode-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class QrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  qrCodeWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.qrCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      qrCodeTextPattern: '${entityName}',
      useQrCodeTextFunction: false,
      qrCodeTextFunction: 'return data[0][\'entityName\'];'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.qrCodeWidgetSettingsForm = this.fb.group({
      qrCodeTextPattern: [settings.qrCodeTextPattern, [Validators.required]],
      useQrCodeTextFunction: [settings.useQrCodeTextFunction, [Validators.required]],
      qrCodeTextFunction: [settings.qrCodeTextFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useQrCodeTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useQrCodeTextFunction: boolean = this.qrCodeWidgetSettingsForm.get('useQrCodeTextFunction').value;
    if (useQrCodeTextFunction) {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').disable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').enable();
    } else {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').enable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').disable();
    }
    this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').updateValueAndValidity({emitEvent});
    this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').updateValueAndValidity({emitEvent});
  }

}
