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
import { Domain, DomainInfo } from '@shared/models/oauth2.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class DomainService {

  constructor(
    private http: HttpClient
  ) {
  }

  public saveDomain(domain: Domain, oauth2ClientIds?: Array<string>, config?: RequestConfig): Observable<Domain> {
    let url = '/api/domain';
    if (oauth2ClientIds?.length) {
      url += `?oauth2ClientIds=${oauth2ClientIds.join(',')}`;
    }
    return this.http.post<Domain>(url, domain, defaultHttpOptionsFromConfig(config));
  }

  public updateOauth2Clients(id: string, oauth2ClientIds: Array<string>, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/domain/${id}/oauth2Clients`, oauth2ClientIds, defaultHttpOptionsFromConfig(config));
  }

  public getDomainInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DomainInfo>> {
    return this.http.get<PageData<DomainInfo>>(`/api/domain/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getDomainInfoById(id: string, config?: RequestConfig): Observable<DomainInfo> {
    return this.http.get<DomainInfo>(`/api/domain/info/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteDomain(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/domain/${id}`, defaultHttpOptionsFromConfig(config));
  }

}
