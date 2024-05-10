///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup, Validators } from "@angular/forms";
import { WidgetSettings, WidgetSettingsComponent } from "@shared/models/widget.models";
import { AppState } from '@core/core.state';
import { Store } from "@ngrx/store";
import { badgePositionTranslationsMap, badgeStyleTranslationsMap } from '@shared/models/mobile-app.models';
import { mobileAppQrCodeWidgetDefaultSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';

@Component({
  selector: 'tb-mobile-app-qr-code-widget-settings',
  templateUrl: './mobile-app-qr-code-widget-settings.component.html',
  styleUrls: ['/mobile-app-qr-code-widget-settings.component.scss', './../widget-settings.scss']
})
export class MobileAppQrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  mobileAppQRCodeWidgetSettingsForm: UntypedFormGroup;

  badgePositionTranslationsMap = badgePositionTranslationsMap;
  badgeStyleTranslationsMap = badgeStyleTranslationsMap;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mobileAppQRCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...mobileAppQrCodeWidgetDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mobileAppQRCodeWidgetSettingsForm = this.fb.group({
      useDefaultApp: [settings.useDefaultApp],
      androidConfig: this.fb.group({
        enabled: [settings.androidConfig.enabled],
        appPackage: [settings.androidConfig.appPackage, [Validators.required]],
        sha256CertFingerprints: [settings.androidConfig.sha256CertFingerprints, [Validators.required]]
      }),
      iosConfig: this.fb.group({
        enabled: [settings.iosConfig.enabled],
        appId: [settings.iosConfig.appId, [Validators.required]]
      }),
      qrCodeConfig: this.fb.group({
        badgeEnabled: [settings.qrCodeConfig.badgeEnabled],
        badgeStyle: [{value: settings.qrCodeConfig.badgeStyle, disabled: true}],
        badgePosition: [{value: settings.qrCodeConfig.badgePosition, disabled: true}],
        qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled],
        qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel, [Validators.required]]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['useDefaultApp', 'androidConfig.enabled', 'iosConfig.enabled', 'qrCodeConfig.badgeEnabled', 'qrCodeConfig.qrCodeLabelEnabled'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useDefaultApp = this.mobileAppQRCodeWidgetSettingsForm.get('useDefaultApp').value;
    const androidEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.enabled').value;
    const iosEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.enabled').value;
    const badgeEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').value;
    const qrCodeLabelEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').value;

    if (useDefaultApp) {
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
      this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
    } else {
      if (androidEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').enable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
      }
      if (iosEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
      }
    }

    if (!androidEnabled && !iosEnabled) {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').disable({emitEvent: false});
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
    }

    if (androidEnabled || iosEnabled) {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').enable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').disable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
    }

    if (qrCodeLabelEnabled) {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
    } else {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
    }
  }

}
