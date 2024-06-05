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

import { Component, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Subject, takeUntil } from 'rxjs';
import { MobileAppService } from '@core/http/mobile-app.service';
import {
  BadgePosition,
  badgePositionTranslationsMap,
  MobileAppSettings
} from '@shared/models/mobile-app.models';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ActionUpdateMobileQrCodeEnabled } from '@core/auth/auth.actions';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-mobile-app-settings',
  templateUrl: './mobile-app-settings.component.html',
  styleUrls: ['mobile-app-settings.component.scss', './settings-card.scss']
})
export class MobileAppSettingsComponent extends PageComponent implements HasConfirmForm, OnDestroy {

  authUser: AuthUser = getCurrentAuthUser(this.store);

  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.MOBILE_APP_SETTINGS, Operation.WRITE);

  mobileAppSettingsForm: FormGroup;

  mobileAppSettings: MobileAppSettings;

  private readonly destroy$ = new Subject<void>();

  badgePositionTranslationsMap = badgePositionTranslationsMap;

  constructor(protected store: Store<AppState>,
              private mobileAppService: MobileAppService,
              private fb: FormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
    this.buildMobileAppSettingsForm();
    this.mobileAppService.getMobileAppSettings()
      .subscribe(settings => this.processMobileAppSettings(settings));
    if (this.isTenantAdmin()) {
      this.mobileAppSettingsForm.get('useSystemSettings').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(value => {
        if (value) {
          this.mobileAppSettingsForm.get('androidConfig.enabled').disable();
          this.mobileAppSettingsForm.get('iosConfig.enabled').disable();
          this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').disable();
        } else {
          this.mobileAppSettingsForm.get('androidConfig.enabled').enable();
          this.mobileAppSettingsForm.get('iosConfig.enabled').enable();
          this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').enable();
        }
      });
    }
    this.mobileAppSettingsForm.get('useDefaultApp').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
        this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
        this.mobileAppSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
      } else {
        if (this.mobileAppSettingsForm.get('androidConfig.enabled').value) {
          this.mobileAppSettingsForm.get('androidConfig.appPackage').enable({emitEvent: false});
          this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').enable({emitEvent: false});
        }
        if (this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
          this.mobileAppSettingsForm.get('iosConfig.appId').enable({emitEvent: false});
        }
      }
    });
    this.mobileAppSettingsForm.get('androidConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.androidEnableChanged(value);
    });
    this.mobileAppSettingsForm.get('iosConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.iosEnableChanged(value);
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').valueChanges.pipe(
      takeUntil(this.destroy$)
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
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        if (this.mobileAppSettingsForm.get('androidConfig.enabled').value || this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
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
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value && this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  private buildMobileAppSettingsForm() {
    this.mobileAppSettingsForm = this.fb.group({
      useSystemSettings: [false],
      useDefaultApp: [true],
      androidConfig: this.fb.group({
        enabled: [true],
        appPackage: [{value: '', disabled: true}, [Validators.required]],
        sha256CertFingerprints: [{value: '', disabled: true}, [Validators.required]],
        storeLink: ['']
      }),
      iosConfig: this.fb.group({
        enabled: [true],
        appId: [{value: '', disabled: true}, [Validators.required]],
        storeLink: ['']
      }),
      qrCodeConfig: this.fb.group({
        showOnHomePage: [true],
        badgeEnabled: [true],
        badgePosition: [BadgePosition.RIGHT],
        qrCodeLabelEnabled: [true],
        qrCodeLabel: ['', [Validators.required, Validators.maxLength(50)]]
      })
    });
  }

  private processMobileAppSettings(mobileAppSettings: MobileAppSettings): void {
    this.mobileAppSettings = {...mobileAppSettings};
    if (!this.isTenantAdmin()) {
      this.mobileAppSettings.useSystemSettings = false;
    }
    if (this.readonly) {
      this.mobileAppSettingsForm.disable();
    }
    this.mobileAppSettingsForm.reset(this.mobileAppSettings);
  }

  private androidEnableChanged(value: boolean): void {
    if (value) {
      if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
        this.mobileAppSettingsForm.get('androidConfig.appPackage').enable({emitEvent: false});
        this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').enable({emitEvent: false});
      }
    } else {
      this.mobileAppSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
      this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
    }
    this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
  }

  private iosEnableChanged(value: boolean): void {
    if (value) {
      if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
        this.mobileAppSettingsForm.get('iosConfig.appId').enable({emitEvent: false});
      }
    } else {
      this.mobileAppSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
    }
    this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
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
