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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';

import { SecurityComponent } from './security.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { User } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserService } from '@core/http/user.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Observable } from 'rxjs';
import { TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';

@Injectable()
export class UserProfileResolver implements Resolve<User> {

  constructor(private store: Store<AppState>,
              private userService: UserService) {
  }

  resolve(): Observable<User> {
    const userId = getCurrentAuthUser(this.store).userId;
    return this.userService.getUser(userId);
  }
}

@Injectable()
export class UserTwoFAProvidersResolver implements Resolve<Array<TwoFactorAuthProviderType>> {

  constructor(private twoFactorAuthService: TwoFactorAuthenticationService) {
  }

  resolve(): Observable<Array<TwoFactorAuthProviderType>> {
    return this.twoFactorAuthService.getAvailableTwoFaProviders();
  }
}

const routes: Routes = [
  {
    path: 'security',
    component: SecurityComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'security.security',
      breadcrumb: {
        label: 'security.security',
        icon: 'lock'
      }
    },
    resolve: {
      user: UserProfileResolver,
      providers: UserTwoFAProvidersResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UserProfileResolver,
    UserTwoFAProvidersResolver
  ]
})
export class SecurityRoutingModule { }
