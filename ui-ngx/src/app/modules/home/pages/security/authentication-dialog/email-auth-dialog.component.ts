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

import { Component, Inject, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';

export interface EmailAuthDialogData {
  email: string;
}

@Component({
  selector: 'tb-email-auth-dialog',
  templateUrl: './email-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class EmailAuthDialogComponent extends DialogComponent<EmailAuthDialogComponent> {

  private authAccountConfig: TwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  emailConfigForm: UntypedFormGroup;
  emailVerificationForm: UntypedFormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              @Inject(MAT_DIALOG_DATA) public data: EmailAuthDialogData,
              public dialogRef: MatDialogRef<EmailAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.emailConfigForm = this.fb.group({
      email: [this.data.email, [Validators.required, Validators.email]]
    });

    this.emailVerificationForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  nextStep() {
    switch (this.stepper.selectedIndex) {
      case 0:
        if (this.emailConfigForm.valid) {
          this.authAccountConfig = {
            providerType: TwoFactorAuthProviderType.EMAIL,
            useByDefault: true,
            email: this.emailConfigForm.get('email').value as string
          };
          this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => {
            this.stepper.next();
          });
        } else {
          this.showFormErrors(this.emailConfigForm);
        }
        break;
      case 1:
        if (this.emailVerificationForm.valid) {
          this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
            this.emailVerificationForm.get('verificationCode').value).subscribe((config) => {
              this.config = config;
              this.stepper.next();
            });
        } else {
          this.showFormErrors(this.emailVerificationForm);
        }
        break;
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.config);
  }

  private showFormErrors(form: UntypedFormGroup) {
    Object.keys(form.controls).forEach(field => {
      const control = form.get(field);
      control.markAsTouched({onlySelf: true});
    });
  }
}
