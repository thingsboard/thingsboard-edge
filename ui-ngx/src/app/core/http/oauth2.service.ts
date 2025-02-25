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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { OAuth2Client, OAuth2ClientInfo, OAuth2ClientRegistrationTemplate } from '@shared/models/oauth2.models';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class OAuth2Service {

  constructor(
    private http: HttpClient
  ) {
  }

  public getOAuth2Template(config?: RequestConfig): Observable<Array<OAuth2ClientRegistrationTemplate>> {
    return this.http.get<Array<OAuth2ClientRegistrationTemplate>>(`/api/oauth2/config/template`, defaultHttpOptionsFromConfig(config));
  }

  public saveOAuth2Client(oAuth2Client: OAuth2Client, config?: RequestConfig): Observable<OAuth2Client> {
    return this.http.post<OAuth2Client>('/api/oauth2/client', oAuth2Client, defaultHttpOptionsFromConfig(config));
  }

  public findTenantOAuth2ClientInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<OAuth2ClientInfo>> {
    return this.http.get<PageData<OAuth2ClientInfo>>(`/api/oauth2/client/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public findTenantOAuth2ClientInfosByIds(clientIds: Array<string>, config?: RequestConfig): Observable<Array<OAuth2ClientInfo>> {
    return this.http.get<Array<OAuth2ClientInfo>>(`/api/oauth2/client/infos?clientIds=${clientIds.join(',')}`, defaultHttpOptionsFromConfig(config))
  }

  public getOAuth2ClientById(id: string, config?: RequestConfig): Observable<OAuth2Client> {
    return this.http.get<OAuth2Client>(`/api/oauth2/client/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteOauth2Client(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/oauth2/client/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getLoginProcessingUrl(config?: RequestConfig): Observable<string> {
    return this.http.get<string>('/api/oauth2/loginProcessingUrl', defaultHttpOptionsFromConfig(config));
  }

}
