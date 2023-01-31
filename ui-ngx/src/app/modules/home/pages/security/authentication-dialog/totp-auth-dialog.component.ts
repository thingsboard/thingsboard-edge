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

import { Component, ElementRef, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TotpTwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';

@Component({
  selector: 'tb-totp-auth-dialog',
  templateUrl: './totp-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class TotpAuthDialogComponent extends DialogComponent<TotpAuthDialogComponent> {

  private authAccountConfig: TotpTwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  totpConfigForm: FormGroup;
  totpAuthURL: string;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;
  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              public dialogRef: MatDialogRef<TotpAuthDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.TOTP).subscribe(accountConfig => {
      this.authAccountConfig = accountConfig as TotpTwoFactorAuthAccountConfig;
      this.totpAuthURL = this.authAccountConfig.authUrl;
      this.authAccountConfig.useByDefault = true;
      import('qrcode').then((QRCode) => {
        QRCode.toCanvas(this.canvasRef.nativeElement, this.totpAuthURL);
        this.canvasRef.nativeElement.style.width = 'auto';
        this.canvasRef.nativeElement.style.height = 'auto';
      });
    });
    this.totpConfigForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  onSaveConfig() {
    if (this.totpConfigForm.valid) {
      this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
        this.totpConfigForm.get('verificationCode').value).subscribe((config) => {
          this.config = config;
          this.stepper.next();
        });
    } else {
      Object.keys(this.totpConfigForm.controls).forEach(field => {
        const control = this.totpConfigForm.get(field);
        control.markAsTouched({onlySelf: true});
      });
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.config);
  }

}
