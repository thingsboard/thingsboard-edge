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
