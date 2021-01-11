///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { SignupRoutingModule } from '@modules/signup/signup-routing.module';
import { SignupComponent } from '@modules/signup/pages/signup/signup.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { RecaptchaModule, RecaptchaFormsModule, RECAPTCHA_BASE_URL } from 'ng-recaptcha';
import { EmailVerificationComponent } from '@modules/signup/pages/signup/email-verification.component';
import { EmailVerifiedComponent } from '@modules/signup/pages/signup/email-verified.component';
import { PrivacyPolicyDialogComponent } from '@modules/signup/pages/signup/privacy-policy-dialog.component';

@NgModule({
  declarations: [
    SignupComponent,
    PrivacyPolicyDialogComponent,
    EmailVerificationComponent,
    EmailVerifiedComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    RecaptchaModule,
    RecaptchaFormsModule,
    SignupRoutingModule
  ],
  providers: [
    {
      provide: RECAPTCHA_BASE_URL,
      useValue: 'https://recaptcha.net/recaptcha/api.js',
    }
  ]
})
export class SignupModule { }
