///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from '@core/guards/auth.guard';
import { SignupComponent } from '@modules/signup/pages/signup/signup.component';
import { EmailVerificationComponent } from '@modules/signup/pages/signup/email-verification.component';
import { EmailVerifiedComponent } from '@modules/signup/pages/signup/email-verified.component';

const routes: Routes = [
  {
    path: 'signup',
    component: SignupComponent,
    data: {
      title: 'signup.signup',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'signup/emailVerification',
    component: EmailVerificationComponent,
    data: {
      title: 'signup.email-verification',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'signup/emailVerified',
    component: EmailVerifiedComponent,
    data: {
      title: 'signup.account-activation-title',
      module: 'public'
    },
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SignupRoutingModule { }
