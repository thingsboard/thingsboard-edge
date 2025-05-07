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

import { Component } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { WidgetSettings, WidgetSettingsComponent } from "@shared/models/widget.models";
import { AppState } from '@core/core.state';
import { Store } from "@ngrx/store";
import { badgePositionTranslationsMap } from '@shared/models/mobile-app.models';
import { mobileAppQrCodeWidgetDefaultSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService} from '@core/http/user-permissions.service';
import { Operation, Resource} from "@shared/models/security.models";

@Component({
  selector: 'tb-mobile-app-qr-code-widget-settings',
  templateUrl: './mobile-app-qr-code-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class MobileAppQrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  mobileAppQRCodeWidgetSettingsForm: UntypedFormGroup;

  badgePositionTranslationsMap = badgePositionTranslationsMap;

  displayConfigurationHint = false;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
    this.displayConfigurationHint = getCurrentAuthUser(this.store).authority !== Authority.CUSTOMER_USER &&
      (this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.WRITE) ||
      this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.READ));
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mobileAppQRCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mobileAppQrCodeWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mobileAppQRCodeWidgetSettingsForm = this.fb.group({
      useSystemSettings: [settings.useSystemSettings],
      qrCodeConfig: this.fb.group({
        badgeEnabled: [settings.qrCodeConfig.badgeEnabled],
        badgePosition: [settings.qrCodeConfig.badgePosition],
        qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled],
        qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel]
      }),
      background: [settings.background],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSettings', 'qrCodeConfig.badgeEnabled', 'qrCodeConfig.qrCodeLabelEnabled'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSettings = this.mobileAppQRCodeWidgetSettingsForm.get('useSystemSettings').value;
    if (!useSystemSettings) {
      const badgeEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').value;
      const qrCodeLabelEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').value;
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
      if (qrCodeLabelEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
      }
    }
  }

  navigateToMobileAppSettings($event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(window.location.origin + '/settings/mobile-app', '_blank');
  }

}
