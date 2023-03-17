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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SignupRequest, SignUpResult } from '@shared/models/signup.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, of } from 'rxjs';
import { LoginResponse } from '@shared/models/login.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Injectable({
  providedIn: 'root'
})
export class SignupService {

  constructor(
    private store: Store<AppState>,
    private http: HttpClient
  ) {
  }

  public signup(signupRequest: SignupRequest, config?: RequestConfig): Observable<SignUpResult> {
    return this.http.post<SignUpResult>('/api/noauth/signup', signupRequest, defaultHttpOptionsFromConfig(config));
  }

  public acceptPrivacyPolicy(config?: RequestConfig): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/signup/acceptPrivacyPolicy', null, defaultHttpOptionsFromConfig(config));
  }

  public deleteTenantAccount(config?: RequestConfig): Observable<any> {
    return this.http.delete('/api/signup/tenantAccount', defaultHttpOptionsFromConfig(config));
  }

  public isDisplayWelcome(): Observable<boolean> {
    const authUser = getCurrentAuthUser(this.store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      return this.http.get<boolean>('/api/signup/displayWelcome')
    } else {
      return of(false);
    }
  }

  public setNotDisplayWelcome(): Observable<any> {
    return this.http.post('/api/signup/notDisplayWelcome', null);
  }

}
