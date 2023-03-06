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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType,
  TwoFactorAuthSettings
} from '@shared/models/two-factor-auth.models';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class TwoFactorAuthenticationService {

  constructor(
    private http: HttpClient
  ) {
  }

  getTwoFaSettings(config?: RequestConfig): Observable<TwoFactorAuthSettings> {
    return this.http.get<TwoFactorAuthSettings>(`/api/2fa/settings`, defaultHttpOptionsFromConfig(config));
  }

  saveTwoFaSettings(settings: TwoFactorAuthSettings, config?: RequestConfig): Observable<TwoFactorAuthSettings> {
    return this.http.post<TwoFactorAuthSettings>(`/api/2fa/settings`, settings, defaultHttpOptionsFromConfig(config));
  }

  getAvailableTwoFaProviders(config?: RequestConfig): Observable<Array<TwoFactorAuthProviderType>> {
    return this.http.get<Array<TwoFactorAuthProviderType>>(`/api/2fa/providers`, defaultHttpOptionsFromConfig(config));
  }

  generateTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, config?: RequestConfig): Observable<TwoFactorAuthAccountConfig> {
    return this.http.post<TwoFactorAuthAccountConfig>(`/api/2fa/account/config/generate?providerType=${providerType}`,
      defaultHttpOptionsFromConfig(config));
  }

  getAccountTwoFaSettings(config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.get<AccountTwoFaSettings>(`/api/2fa/account/settings`, defaultHttpOptionsFromConfig(config));
  }

  updateTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, useByDefault: boolean,
                           config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.put<AccountTwoFaSettings>(`/api/2fa/account/config?providerType=${providerType}`, {useByDefault},
      defaultHttpOptionsFromConfig(config));
  }

  submitTwoFaAccountConfig(authConfig: TwoFactorAuthAccountConfig, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/2fa/account/config/submit`, authConfig, defaultHttpOptionsFromConfig(config));
  }

  verifyAndSaveTwoFaAccountConfig(authConfig: TwoFactorAuthAccountConfig, verificationCode?: number,
                                  config?: RequestConfig): Observable<AccountTwoFaSettings> {
    let url = '/api/2fa/account/config';
    if (isDefinedAndNotNull(verificationCode)) {
      url += `?verificationCode=${verificationCode}`;
    }
    return this.http.post<AccountTwoFaSettings>(url, authConfig, defaultHttpOptionsFromConfig(config));
  }

  deleteTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.delete<AccountTwoFaSettings>(`/api/2fa/account/config?providerType=${providerType}`,
      defaultHttpOptionsFromConfig(config));
  }

  requestTwoFaVerificationCodeSend(providerType: TwoFactorAuthProviderType, config?: RequestConfig) {
    return this.http.post(`/api/auth/2fa/verification/send?providerType=${providerType}`, defaultHttpOptionsFromConfig(config));
  }

}
