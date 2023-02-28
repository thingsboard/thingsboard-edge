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

import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  twoFactorAuthProvidersLoginData,
  TwoFactorAuthProviderType,
  TwoFaProviderInfo
} from '@shared/models/two-factor-auth.models';
import { TranslateService } from '@ngx-translate/core';
import { interval, Subscription } from 'rxjs';
import { isEqual } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-two-factor-auth-login',
  templateUrl: './two-factor-auth-login.component.html',
  styleUrls: ['./two-factor-auth-login.component.scss']
})
export class TwoFactorAuthLoginComponent extends PageComponent implements OnInit, OnDestroy {

  @HostBinding('class') class = 'tb-custom-css';

  private providersInfo: TwoFaProviderInfo[];
  private prevProvider: TwoFactorAuthProviderType;
  private timer: Subscription;
  private minVerificationPeriod = 0;
  private timerID: NodeJS.Timeout;

  showResendAction = false;
  selectedProvider: TwoFactorAuthProviderType;
  allowProviders: TwoFactorAuthProviderType[] = [];

  providersData = twoFactorAuthProvidersLoginData;
  providerDescription = '';
  hideResendButton = true;
  countDownTime = 0;

  maxLengthInput = 6;
  inputMode = 'numeric';
  pattern = '[0-9]*';

  verificationForm = this.fb.group({
    verificationCode: ['', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(6),
      Validators.pattern(/^\d*$/)
    ]]
  });

  constructor(protected store: Store<AppState>,
              private twoFactorAuthService: TwoFactorAuthenticationService,
              private authService: AuthService,
              private translate: TranslateService,
              private fb: FormBuilder,
              public wl: WhiteLabelingService) {
    super(store);
  }

  ngOnInit() {
    this.providersInfo = this.authService.twoFactorAuthProviders;
    Object.values(TwoFactorAuthProviderType).forEach(provider => {
      const providerConfig = this.providersInfo.find(config => config.type === provider);
      if (providerConfig) {
        if (providerConfig.default) {
          this.selectedProvider = providerConfig.type;
          this.providerDescription = this.translate.instant(this.providersData.get(providerConfig.type).description, {
            contact: providerConfig.contact
          });
          this.minVerificationPeriod = providerConfig?.minVerificationCodeSendPeriod || 30;
        }
        this.allowProviders.push(providerConfig.type);
      }
    });
    if (this.selectedProvider !== TwoFactorAuthProviderType.TOTP) {
      this.sendCode();
      this.showResendAction = true;
    }
    this.timer = interval(1000).subscribe(() => this.updatedTime());
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.timer.unsubscribe();
    clearTimeout(this.timerID);
  }

  sendVerificationCode() {
    if (this.verificationForm.valid && this.selectedProvider) {
      this.authService.checkTwoFaVerificationCode(this.selectedProvider, this.verificationForm.get('verificationCode').value).subscribe(
        () => {},
        (error) => {
          if (error.status === 400) {
            this.verificationForm.get('verificationCode').setErrors({incorrectCode: true});
          } else if (error.status === 429) {
            this.verificationForm.get('verificationCode').setErrors({tooManyRequest: true});
            this.timerID = setTimeout(() => {
              let errors = this.verificationForm.get('verificationCode').errors;
              delete errors.tooManyRequest;
              if (isEqual(errors, {})) {
                errors = null;
              }
              this.verificationForm.get('verificationCode').setErrors(errors);
            }, 5000);
          } else {
            this.store.dispatch(new ActionNotificationShow({
              message: error.error.message,
              type: 'error',
              verticalPosition: 'top',
              horizontalPosition: 'left'
            }));
          }
        }
      );
    }
  }

  selectProvider(type: TwoFactorAuthProviderType) {
    this.prevProvider = type === null ? this.selectedProvider : null;
    this.selectedProvider = type;
    this.showResendAction = false;
    if (type !== null) {
      this.verificationForm.get('verificationCode').reset();
      const providerConfig = this.providersInfo.find(config => config.type === type);
      this.providerDescription = this.translate.instant(this.providersData.get(providerConfig.type).description, {
        contact: providerConfig.contact
      });
      if (type !== TwoFactorAuthProviderType.TOTP && type !== TwoFactorAuthProviderType.BACKUP_CODE) {
        this.sendCode();
        this.showResendAction = true;
        this.minVerificationPeriod = providerConfig?.minVerificationCodeSendPeriod || 30;
      }
      if (type === TwoFactorAuthProviderType.BACKUP_CODE) {
        this.verificationForm.get('verificationCode').setValidators([
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(8),
          Validators.pattern(/^[\dabcdef]*$/)
        ]);
        this.maxLengthInput = 8;
        this.inputMode = 'text';
        this.pattern = '[0-9abcdef]*';
      } else {
        this.verificationForm.get('verificationCode').setValidators([
          Validators.required,
          Validators.minLength(6),
          Validators.maxLength(6),
          Validators.pattern(/^\d*$/)
        ]);
        this.maxLengthInput = 6;
        this.inputMode = 'numeric';
        this.pattern = '[0-9]*';
      }
      this.verificationForm.get('verificationCode').updateValueAndValidity({emitEvent: false});
    }
  }

  sendCode($event?: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.hideResendButton = true;
    this.countDownTime = 0;
    this.twoFactorAuthService.requestTwoFaVerificationCodeSend(this.selectedProvider).subscribe(() => {
      this.countDownTime = this.minVerificationPeriod;
    }, () => {
      this.countDownTime = this.minVerificationPeriod;
    });
  }

  cancelLogin() {
    if (this.prevProvider) {
      this.selectProvider(this.prevProvider);
    } else {
      this.authService.logout();
    }
  }

  private updatedTime() {
    if (this.countDownTime > 0) {
      this.countDownTime--;
      if (this.countDownTime === 0) {
        this.hideResendButton = false;
      }
    }
  }
}
