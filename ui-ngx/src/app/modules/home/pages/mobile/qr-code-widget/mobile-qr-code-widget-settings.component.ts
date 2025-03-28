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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MobileApplicationService } from '@core/http/mobile-application.service';
import { BadgePosition, badgePositionTranslationsMap, QrCodeSettings } from '@shared/models/mobile-app.models';
import { ActionUpdateMobileQrCodeEnabled } from '@core/auth/auth.actions';
import { EntityType } from '@shared/models/entity-type.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-mobile-qr-code-widget',
  templateUrl: './mobile-qr-code-widget-settings.component.html',
  styleUrls: ['mobile-qr-code-widget-settings.component.scss', '../../admin/settings-card.scss']
})
export class MobileQrCodeWidgetSettingsComponent extends PageComponent implements HasConfirmForm {

  readonly badgePositionTranslationsMap = badgePositionTranslationsMap;
  readonly entityType = EntityType;

  setBaseURL = true;

  mobileAppSettingsForm = this.fb.group({
    useSystemSettings: [false],
    useDefaultApp: [true],
    mobileAppBundleId: [{value: null, disabled: true}, Validators.required],
    androidEnabled: [true],
    iosEnabled: [true],
    qrCodeConfig: this.fb.group({
      showOnHomePage: [true],
      badgeEnabled: [true],
      badgePosition: [BadgePosition.RIGHT],
      qrCodeLabelEnabled: [true],
      qrCodeLabel: ['', [Validators.required, Validators.maxLength(50)]]
    })
  });

  private authUser = getCurrentAuthUser(this.store);
  private mobileAppSettings: QrCodeSettings;

  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.WRITE);

  constructor(protected store: Store<AppState>,
              private mobileAppService: MobileApplicationService,
              private fb: FormBuilder,
              private userPermissionsService: UserPermissionsService,
              private wl: WhiteLabelingService) {
    super(store);
    this.mobileAppService.getMobileAppSettings()
      .subscribe(settings => this.processMobileAppSettings(settings));

    if(this.isTenantAdmin()) {
      this.wl.getCurrentLoginWhiteLabelParams().subscribe(value => this.setBaseURL = !!value.baseUrl);
    }

    if (this.readonly) {
      this.mobileAppSettingsForm.disable()
    } else {
      if (this.isTenantAdmin()) {
        this.mobileAppSettingsForm.get('useSystemSettings').valueChanges.pipe(
          takeUntilDestroyed()
        ).subscribe(value => {
          if (value) {
            this.mobileAppSettingsForm.get('mobileAppBundleId').disable({emitEvent: false});
            this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
          } else {
            const formValue = this.mobileAppSettingsForm.value;
            if (!formValue.useDefaultApp) {
              this.mobileAppSettingsForm.get('mobileAppBundleId').enable({emitEvent: false});
            }
            if (formValue.qrCodeConfig.qrCodeLabelEnabled && formValue.qrCodeConfig.showOnHomePage) {
              this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
            }
          }
        });
      }
      this.mobileAppSettingsForm.get('useDefaultApp').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(value => {
        if (value) {
          this.mobileAppSettingsForm.get('mobileAppBundleId').disable({emitEvent: false});
        } else {
          this.mobileAppSettingsForm.get('mobileAppBundleId').enable({emitEvent: false});
        }
      });
      this.mobileAppSettingsForm.get('androidEnabled').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(() => {
        this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
      });
      this.mobileAppSettingsForm.get('iosEnabled').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(() => {
        this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
      });
      this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(value => {
        if (value) {
          this.mobileAppSettingsForm.get('qrCodeConfig').enable({emitEvent: false});
        } else {
          this.mobileAppSettingsForm.get('qrCodeConfig').disable({emitEvent: false});
          this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').enable({emitEvent: false});
        }
        this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').updateValueAndValidity({onlySelf: true});
      });
      this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(value => {
        if (value) {
          const formValue = this.mobileAppSettingsForm.getRawValue();
          if (formValue.androidEnabled || formValue.iosEnabled) {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
          } else {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
          }
        } else {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
        }
      });
      this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').valueChanges.pipe(
        takeUntilDestroyed()
      ).subscribe(value => {
        if (value && this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
          this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
        } else {
          this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
        }
      });
    }
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  private processMobileAppSettings(mobileAppSettings: QrCodeSettings): void {
    this.mobileAppSettings = {...mobileAppSettings};
    if (!this.isTenantAdmin()) {
      this.mobileAppSettings.useSystemSettings = false;
    }
    this.mobileAppSettingsForm.reset(this.mobileAppSettings);
  }

  save(): void {
    const showOnHomePagePreviousValue = this.mobileAppSettings.qrCodeConfig.showOnHomePage;
    this.mobileAppSettings = {...this.mobileAppSettings, ...this.mobileAppSettingsForm.getRawValue()};
    this.mobileAppService.saveMobileAppSettings(this.mobileAppSettings)
      .subscribe((settings) => {
        const showOnHomePageValue = settings.qrCodeConfig.showOnHomePage;
        if (showOnHomePagePreviousValue !== showOnHomePageValue) {
          this.store.dispatch(new ActionUpdateMobileQrCodeEnabled({mobileQrEnabled: showOnHomePageValue}));
        }
        this.processMobileAppSettings(settings);
      });
  }

  confirmForm(): FormGroup {
    return this.mobileAppSettingsForm;
  }
}
