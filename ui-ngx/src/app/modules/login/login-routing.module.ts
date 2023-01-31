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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LoginComponent } from './pages/login/login.component';
import { AuthGuard } from '@core/guards/auth.guard';
import { ResetPasswordRequestComponent } from '@modules/login/pages/login/reset-password-request.component';
import { ResetPasswordComponent } from '@modules/login/pages/login/reset-password.component';
import { CreatePasswordComponent } from '@modules/login/pages/login/create-password.component';
import { TwoFactorAuthLoginComponent } from '@modules/login/pages/login/two-factor-auth-login.component';
import { Authority } from '@shared/models/authority.enum';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    data: {
      title: 'login.login',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetPasswordRequest',
    component: ResetPasswordRequestComponent,
    data: {
      title: 'login.request-password-reset',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetPassword',
    component: ResetPasswordComponent,
    data: {
      title: 'login.reset-password',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetExpiredPassword',
    component: ResetPasswordComponent,
    data: {
      title: 'login.reset-password',
      module: 'public',
      expiredPassword: true
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/createPassword',
    component: CreatePasswordComponent,
    data: {
      title: 'login.create-password',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/mfa',
    component: TwoFactorAuthLoginComponent,
    data: {
      title: 'login.two-factor-authentication',
      auth: [Authority.PRE_VERIFICATION_TOKEN],
      module: 'public'
    },
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LoginRoutingModule { }
