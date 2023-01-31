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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { JwtSettings, SecuritySettings } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { mergeMap, tap } from 'rxjs/operators';
import { randomAlphanumeric } from '@core/utils';
import { AuthService } from '@core/auth/auth.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'tb-security-settings',
  templateUrl: './security-settings.component.html',
  styleUrls: ['./security-settings.component.scss', './settings-card.scss']
})
export class SecuritySettingsComponent extends PageComponent implements HasConfirmForm {

  securitySettingsFormGroup: FormGroup;
  jwtSecuritySettingsFormGroup: FormGroup;

  private securitySettings: SecuritySettings;
  private jwtSettings: JwtSettings;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private authService: AuthService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
    this.buildSecuritySettingsForm();
    this.buildJwtSecuritySettingsForm();
    this.adminService.getSecuritySettings().subscribe(
      securitySettings => this.processSecuritySettings(securitySettings)
    );
    this.adminService.getJwtSettings().subscribe(
      jwtSettings => this.processJwtSettings(jwtSettings)
    );
  }

  buildSecuritySettingsForm() {
    this.securitySettingsFormGroup = this.fb.group({
      maxFailedLoginAttempts: [null, [Validators.min(0)]],
      userLockoutNotificationEmail: ['', []],
      passwordPolicy: this.fb.group(
        {
          minimumLength: [null, [Validators.required, Validators.min(5), Validators.max(50)]],
          minimumUppercaseLetters: [null, Validators.min(0)],
          minimumLowercaseLetters: [null, Validators.min(0)],
          minimumDigits: [null, Validators.min(0)],
          minimumSpecialCharacters: [null, Validators.min(0)],
          passwordExpirationPeriodDays: [null, Validators.min(0)],
          passwordReuseFrequencyDays: [null, Validators.min(0)],
          allowWhitespaces: [true]
        }
      )
    });
  }

  buildJwtSecuritySettingsForm() {
    this.jwtSecuritySettingsFormGroup = this.fb.group({
      tokenIssuer: ['', Validators.required],
      tokenSigningKey: ['', [Validators.required, this.base64Format]],
      tokenExpirationTime: [0, [Validators.required, Validators.pattern('[0-9]*'), Validators.min(60)]],
      refreshTokenExpTime: [0, [Validators.required, Validators.pattern('[0-9]*'), Validators.min(900)]]
    }, {validators: this.refreshTokenTimeGreatTokenTime.bind(this)});
    this.jwtSecuritySettingsFormGroup.get('tokenExpirationTime').valueChanges.subscribe(
      () => this.jwtSecuritySettingsFormGroup.get('refreshTokenExpTime').updateValueAndValidity({onlySelf: true})
    );
  }

  save(): void {
    this.securitySettings = {...this.securitySettings, ...this.securitySettingsFormGroup.value};
    this.adminService.saveSecuritySettings(this.securitySettings).subscribe(
      securitySettings => this.processSecuritySettings(securitySettings)
    );
  }

  saveJwtSettings() {
    const jwtFormSettings = this.jwtSecuritySettingsFormGroup.value;
    this.confirmChangeJWTSettings().pipe(mergeMap(value => {
      if (value) {
        return this.adminService.saveJwtSettings(jwtFormSettings).pipe(
          tap((data) => this.authService.setUserFromJwtToken(data.token, data.refreshToken, false)),
          mergeMap(() => this.adminService.getJwtSettings()),
          tap(jwtSettings => this.processJwtSettings(jwtSettings))
        );
      }
      return of(null);
    })).subscribe(() => {});
  }

  discardSetting() {
    this.securitySettingsFormGroup.reset(this.securitySettings);
  }

  discardJwtSetting() {
    this.jwtSecuritySettingsFormGroup.reset(this.jwtSettings);
  }

  markAsTouched() {
    this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').markAsTouched();
  }

  private confirmChangeJWTSettings(): Observable<boolean> {
    if (this.jwtSecuritySettingsFormGroup.get('tokenIssuer').value !== (this.jwtSettings?.tokenIssuer || '') ||
      this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').value !== (this.jwtSettings?.tokenSigningKey || '')) {
      return this.dialogService.confirm(
        this.translate.instant('admin.jwt.info-header'),
        `<div style="max-width: 640px">${this.translate.instant('admin.jwt.info-message')}</div>`,
        this.translate.instant('action.discard-changes'),
        this.translate.instant('action.confirm')
      );
    }
    return of(true);
  }

  generateSigningKey() {
    this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').setValue(btoa(randomAlphanumeric(64)));
    if (this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').pristine) {
      this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').markAsDirty();
      this.jwtSecuritySettingsFormGroup.get('tokenSigningKey').markAsTouched();
    }
  }

  private processSecuritySettings(securitySettings: SecuritySettings) {
    this.securitySettings = securitySettings;
    this.securitySettingsFormGroup.reset(this.securitySettings);
  }

  private processJwtSettings(jwtSettings: JwtSettings) {
    this.jwtSettings = jwtSettings;
    this.jwtSecuritySettingsFormGroup.reset(jwtSettings);
  }

  private refreshTokenTimeGreatTokenTime(formGroup: FormGroup): { [key: string]: boolean } | null {
    if (formGroup) {
      const tokenTime = formGroup.value.tokenExpirationTime;
      const refreshTokenTime = formGroup.value.refreshTokenExpTime;
      if (tokenTime >= refreshTokenTime ) {
        if (formGroup.get('refreshTokenExpTime').untouched) {
          formGroup.get('refreshTokenExpTime').markAsTouched();
        }
        formGroup.get('refreshTokenExpTime').setErrors({lessToken: true});
        return {lessToken: true};
      }
    }
    return null;
  }

  private base64Format(control: FormControl): { [key: string]: boolean } | null {
    if (control.value === '' || control.value === 'thingsboardDefaultSigningKey') {
      return null;
    }
    try {
      const value = atob(control.value);
      if (value.length < 32) {
        return {minLength: true};
      }
      return null;
    } catch (e) {
      return {base64: true};
    }
  }

  confirmForm(): FormGroup {
    return this.securitySettingsFormGroup.dirty ? this.securitySettingsFormGroup : this.jwtSecuritySettingsFormGroup;
  }

}
