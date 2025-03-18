///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import {
  CaptchaParams,
  SignUpSelfRegistrationParams,
  WebSelfRegistrationParams
} from '@shared/models/self-register.models';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { isDefinedAndNotNull } from '@core/utils';

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
        this.signUpParams = signUpParams || {} as SignUpSelfRegistrationParams;
        this.signUpParams.activate = isDefinedAndNotNull(signUpParams?.captcha?.siteKey);
        if (!isDefinedAndNotNull(signUpParams?.captcha)) {
          this.signUpParams.captcha = {} as CaptchaParams;
        }
        this.signUpParams.captcha.version = signUpParams?.captcha?.version || 'v3';
      })
    );
  }

  public loadPrivacyPolicy(): Observable<string> {
    return this.http.get<string>('/api/noauth/selfRegistration/privacyPolicy')
  }

  public loadTermsOfUse(): Observable<string> {
    return this.http.get<string>('/api/noauth/selfRegistration/termsOfUse')
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

  public saveSelfRegistrationParams(selfRegistrationParams: WebSelfRegistrationParams,
                                    config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.post<WebSelfRegistrationParams>('/api/selfRegistration/selfRegistrationParams',
      selfRegistrationParams, defaultHttpOptionsFromConfig(config));
  }

  public getSelfRegistrationParams(config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.get<WebSelfRegistrationParams>(`/api/selfRegistration/selfRegistrationParams`, defaultHttpOptionsFromConfig(config));
  }

  public deleteSelfRegistrationParams(config?: RequestConfig) {
    return this.http.delete(`/api/selfRegistration/selfRegistrationParams`, defaultHttpOptionsFromConfig(config));
  }

}
