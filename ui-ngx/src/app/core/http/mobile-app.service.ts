///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { MobileAppSettings, MobileAppStoreLinks } from '@shared/models/mobile-app.models';

@Injectable({
  providedIn: 'root'
})
export class MobileAppService {

  constructor(
    private http: HttpClient
  ) {
  }

  public getMobileAppSettings(config?: RequestConfig): Observable<MobileAppSettings> {
    return this.http.get<MobileAppSettings>(`/api/mobile/app/settings`, defaultHttpOptionsFromConfig(config));
  }

  public getMergedMobileAppSettings(config?: RequestConfig): Observable<MobileAppSettings> {
    return this.http.get<MobileAppSettings>(`/api/mobile/app/settings/merged`, defaultHttpOptionsFromConfig(config));
  }

  public saveMobileAppSettings(mobileAppSettings: MobileAppSettings, config?: RequestConfig): Observable<MobileAppSettings> {
    return this.http.post<MobileAppSettings>(`/api/mobile/app/settings`, mobileAppSettings, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppDeepLink(config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/mobile/deepLink`, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppStoreLinks(config?: RequestConfig): Observable<MobileAppStoreLinks> {
    return this.http.get<MobileAppStoreLinks>('/api/mobile/app/storeLinks', defaultHttpOptionsFromConfig(config));
  }

}
