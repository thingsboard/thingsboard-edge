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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Tenant, TenantInfo } from '@shared/models/tenant.model';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class TenantService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenants(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Tenant>> {
    return this.http.get<PageData<Tenant>>(`/api/tenants${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantsByIds(tenantIds: Array<string>, config?: RequestConfig): Observable<Array<Tenant>> {
    return this.http.get<Array<Tenant>>(`/api/tenants?tenantIds=${tenantIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((tenants) => sortEntitiesByIds(tenants, tenantIds))
    );
  }

  public getTenantInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<TenantInfo>> {
    return this.http.get<PageData<TenantInfo>>(`/api/tenantInfos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenant(tenantId: string, config?: RequestConfig): Observable<Tenant> {
    return this.http.get<Tenant>(`/api/tenant/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantInfo(tenantId: string, config?: RequestConfig): Observable<TenantInfo> {
    return this.http.get<TenantInfo>(`/api/tenant/info/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveTenant(tenant: Tenant, config?: RequestConfig): Observable<Tenant> {
    return this.http.post<Tenant>('/api/tenant', tenant, defaultHttpOptionsFromConfig(config));
  }

  public deleteTenant(tenantId: string, config?: RequestConfig) {
    return this.http.delete(`/api/tenant/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

}
