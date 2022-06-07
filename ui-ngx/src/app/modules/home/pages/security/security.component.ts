///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { User } from '@shared/models/user.model';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { ActivatedRoute } from '@angular/router';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DatePipe } from '@angular/common';
import { ClipboardService } from 'ngx-clipboard';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettingProviders,
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  EmailTwoFactorAuthAccountConfig,
  SmsTwoFactorAuthAccountConfig,
  twoFactorAuthProvidersData,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { authenticationDialogMap } from '@home/pages/security/authentication-dialog/authentication-dialog.map';
import { takeUntil, tap } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import {Operation, Resource} from "@shared/models/security.models";
import {UserPermissionsService} from "@core/http/user-permissions.service";

@Component({
  selector: 'tb-security',
  templateUrl: './security.component.html',
  styleUrls: ['./security.component.scss']
})
export class SecurityComponent extends PageComponent implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();
  private accountConfig: AccountTwoFaSettingProviders;

  resource = Resource;
  operation = Operation;

  twoFactorAuth: FormGroup;
  user: User;
  allowTwoFactorProviders: TwoFactorAuthProviderType[] = [];
  providersData = twoFactorAuthProvidersData;
  twoFactorAuthProviderType = TwoFactorAuthProviderType;
  useByDefault: TwoFactorAuthProviderType = null;
  activeSingleProvider = true;

  get jwtToken(): string {
    return `Bearer ${localStorage.getItem('jwt_token')}`;
  }

  get jwtTokenExpiration(): string {
    return localStorage.getItem('jwt_token_expiration');
  }

  get expirationJwtData(): string {
    const expirationData = this.datePipe.transform(this.jwtTokenExpiration, 'yyyy-MM-dd HH:mm:ss');
    return this.translate.instant('profile.valid-till', { expirationData });
  }

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private translate: TranslateService,
              private twoFaService: TwoFactorAuthenticationService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder,
              private datePipe: DatePipe,
              private userPermissionsService: UserPermissionsService,
              private clipboardService: ClipboardService) {
    super(store);
  }

  ngOnInit() {
    this.buildTwoFactorForm();
    this.user = this.route.snapshot.data.user;
    this.twoFactorLoad(this.route.snapshot.data.providers);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildTwoFactorForm() {
    this.twoFactorAuth = this.fb.group({
      TOTP: [false],
      SMS: [false],
      EMAIL: [false],
      BACKUP_CODE: [{value: false, disabled: true}]
    });
    this.twoFactorAuth.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: {TwoFactorAuthProviderType: boolean}) => {
      const formActiveValue = Object.keys(value).filter(item => value[item] && item !== TwoFactorAuthProviderType.BACKUP_CODE);
      this.activeSingleProvider = formActiveValue.length < 2;
      if (formActiveValue.length) {
        this.twoFactorAuth.get('BACKUP_CODE').enable({emitEvent: false});
      } else {
        this.twoFactorAuth.get('BACKUP_CODE').disable({emitEvent: false});
      }
    });
  }

  private twoFactorLoad(providers: TwoFactorAuthProviderType[]) {
    if (providers.length && this.userPermissionsService.hasGenericPermission(Resource.PROFILE, Operation.WRITE)) {
      this.twoFaService.getAccountTwoFaSettings().subscribe(data => this.processTwoFactorAuthConfig(data));
      Object.values(TwoFactorAuthProviderType).forEach(type => {
        if (providers.includes(type)) {
          this.allowTwoFactorProviders.push(type);
        }
      });
    }
  }

  private processTwoFactorAuthConfig(setting: AccountTwoFaSettings) {
    this.accountConfig = setting?.configs || {};
    Object.values(TwoFactorAuthProviderType).forEach(provider => {
      if (this.accountConfig[provider]) {
        this.twoFactorAuth.get(provider).setValue(true);
        if (this.accountConfig[provider].useByDefault) {
          this.useByDefault = provider;
        }
      } else {
        this.twoFactorAuth.get(provider).setValue(false);
      }
    });
  }

  trackByProvider(i: number, provider: TwoFactorAuthProviderType) {
    return provider;
  }

  copyToken() {
    if (+this.jwtTokenExpiration < Date.now()) {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('profile.tokenCopiedWarnMessage'),
        type: 'warn',
        duration: 1500,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    } else {
      this.clipboardService.copyFromContent(this.jwtToken);
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('profile.tokenCopiedSuccessMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    }
  }

  confirm2FAChange(event: MouseEvent, provider: TwoFactorAuthProviderType) {
    event.stopPropagation();
    event.preventDefault();
    if (this.twoFactorAuth.get(provider).disabled) {
      return;
    }
    if (this.twoFactorAuth.get(provider).value) {
      const providerName = this.translate.instant(`security.2fa.provider.${provider.toLowerCase()}`);
      this.dialogService.confirm(
        this.translate.instant('security.2fa.disable-2fa-provider-title', {name: providerName}),
        this.translate.instant('security.2fa.disable-2fa-provider-text', {name: providerName}),
      ).subscribe(res => {
        if (res) {
          this.twoFactorAuth.disable({emitEvent: false});
          this.twoFaService.deleteTwoFaAccountConfig(provider)
            .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
            .subscribe(data => this.processTwoFactorAuthConfig(data));
        }
      });
    } else {
      this.createdNewAuthConfig(provider);
    }
  }

  private createdNewAuthConfig(provider: TwoFactorAuthProviderType) {
    const dialogData = provider === TwoFactorAuthProviderType.EMAIL ? {email: this.user.email} : {};
    this.dialog.open(authenticationDialogMap.get(provider), {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: dialogData
    }).afterClosed().subscribe(res => {
      if (isDefinedAndNotNull(res)) {
        this.processTwoFactorAuthConfig(res);
      }
    });
  }

  changeDefaultProvider(event: MouseEvent, provider: TwoFactorAuthProviderType) {
    event.stopPropagation();
    event.preventDefault();
    if (this.useByDefault !== provider) {
      this.twoFactorAuth.disable({emitEvent: false});
      this.twoFaService.updateTwoFaAccountConfig(provider, true)
        .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
        .subscribe(data => this.processTwoFactorAuthConfig(data));
    }
  }

  generateNewBackupCode() {
    const codeLeft = (this.accountConfig[TwoFactorAuthProviderType.BACKUP_CODE] as BackupCodeTwoFactorAuthAccountConfig).codesLeft;
    let subscription: Observable<boolean>;
    if (codeLeft) {
      subscription = this.dialogService.confirm(
        'Get new set of backup codes?',
        `If you get new backup codes, ${codeLeft} remaining codes you have left will be unusable.`,
        '',
        'Get new codes'
      );
    } else {
      subscription = of(true);
    }
    subscription.subscribe(res => {
      if (res) {
        this.twoFactorAuth.disable({emitEvent: false});
        this.twoFaService.deleteTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE)
          .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
          .subscribe(() => this.createdNewAuthConfig(TwoFactorAuthProviderType.BACKUP_CODE));
      }
    });
  }

  providerDataInfo(provider: TwoFactorAuthProviderType) {
    const info = {info: null};
    const providerConfig = this.accountConfig[provider];
    if (isDefinedAndNotNull(providerConfig)) {
      switch (provider) {
        case TwoFactorAuthProviderType.EMAIL:
          info.info = (providerConfig as EmailTwoFactorAuthAccountConfig).email;
          break;
        case TwoFactorAuthProviderType.SMS:
          info.info = (providerConfig as SmsTwoFactorAuthAccountConfig).phoneNumber;
          break;
        case TwoFactorAuthProviderType.BACKUP_CODE:
          info.info = (providerConfig as BackupCodeTwoFactorAuthAccountConfig).codesLeft;
          break;
      }
    }
    return info;
  }
}
