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
import { SelfRegistrationParams, SignUpSelfRegistrationParams } from '@shared/models/self-register.models';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class SelfRegistrationService {

  signUpParams: SignUpSelfRegistrationParams = null;

  constructor(
    private router: Router,
    private http: HttpClient,
  ) {
  }

  public getRegistrationLink(domainName: string): string {
    return `${domainName}/signup`;
  }

  public loadSelfRegistrationParams(): Observable<SignUpSelfRegistrationParams> {
    return this.http.get<SignUpSelfRegistrationParams>('/api/noauth/selfRegistration/signUpSelfRegistrationParams').pipe(
      tap((signUpParams) => {
        this.signUpParams = signUpParams;
        this.signUpParams.activate = this.signUpParams.captchaSiteKey !== null;
      })
    );
  }

  public loadPrivacyPolicy(): Observable<string> {
    return this.http.get<string>('/api/noauth/selfRegistration/privacyPolicy')
  }

  public isAvailablePage(): Observable<any> {
    if (this.signUpParams) {
      if (!this.signUpParams.activate) {
        this.router.navigateByUrl('login');
      }
      return of(null);
    } else {
      return this.loadSelfRegistrationParams().pipe(
        tap(() => {
          if (!this.signUpParams.activate) {
            this.router.navigateByUrl('login');
          }
        })
      );
    }
  }

  public saveSelfRegistrationParams(selfRegistrationParams: SelfRegistrationParams,
                                    config?: RequestConfig): Observable<SelfRegistrationParams> {
    return this.http.post<SelfRegistrationParams>('/api/selfRegistration/selfRegistrationParams',
      selfRegistrationParams, defaultHttpOptionsFromConfig(config));
  }

  public getSelfRegistrationParams(config?: RequestConfig): Observable<SelfRegistrationParams> {
    return this.http.get<SelfRegistrationParams>(`/api/selfRegistration/selfRegistrationParams`, defaultHttpOptionsFromConfig(config));
  }

}
