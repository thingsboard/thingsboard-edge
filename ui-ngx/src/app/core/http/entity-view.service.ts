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
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { EntityView, EntityViewInfo, EntityViewSearchQuery } from '@app/shared/models/entity-view.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class EntityViewService {

  constructor(
    private http: HttpClient
  ) { }

  /* public getTenantEntityViewInfos(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/tenant/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEntityViewInfos(customerId: string, pageLink: PageLink, type: string = '',
                                    config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    return this.http.get<PageData<EntityViewInfo>>(`/api/customer/${customerId}/entityViewInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public getTenantEntityViews(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EntityView>> {
    return this.http.get<PageData<EntityView>>(`/api/tenant/entityViews${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEntityViews(customerId: string, pageLink: PageLink, type: string = '',
                                    config?: RequestConfig): Observable<PageData<EntityView>> {
    return this.http.get<PageData<EntityView>>(`/api/customer/${customerId}/entityViews${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityView(entityViewId: string, config?: RequestConfig): Observable<EntityView> {
    return this.http.get<EntityView>(`/api/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityViews(entityViewIds: Array<string>, config?: RequestConfig): Observable<Array<EntityView>> {
    return this.http.get<Array<EntityView>>(`/api/entityViews?entityViewIds=${entityViewIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((entityViews) => sortEntitiesByIds(entityViews, entityViewIds))
    );
  }

  public getUserEntityViews(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EntityView>> {
    return this.http.get<PageData<EntityView>>(`/api/user/entityViews${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllEntityViewInfos(includeCustomers: boolean,
                               pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    let url = `/api/entityViewInfos/all${pageLink.toQuery()}&type=${type}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<EntityViewInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEntityViewInfos(includeCustomers: boolean, customerId: string,
                                    pageLink: PageLink, type: string = '',
                                    config?: RequestConfig): Observable<PageData<EntityViewInfo>> {
    let url = `/api/customer/${customerId}/entityViewInfos${pageLink.toQuery()}&type=${type}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<EntityViewInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }


  /* public getEntityViewInfo(entityViewId: string, config?: RequestConfig): Observable<EntityViewInfo> {
    return this.http.get<EntityViewInfo>(`/api/entityView/info/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  } */

  public saveEntityView(entityView: EntityView, entityGroupId?: string, config?: RequestConfig): Observable<EntityView> {
    let url = '/api/entityView';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<EntityView>(url, entityView, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityView(entityViewId: string, config?: RequestConfig) {
    return this.http.delete(`/api/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityViewTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/entityView/types', defaultHttpOptionsFromConfig(config));
  }

  /* public makeEntityViewPublic(entityViewId: string, config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/public/entityView/${entityViewId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEntityViewToCustomer(customerId: string, entityViewId: string,
                                    config?: RequestConfig): Observable<EntityView> {
    return this.http.post<EntityView>(`/api/customer/${customerId}/entityView/${entityViewId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEntityViewFromCustomer(entityViewId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/entityView/${entityViewId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public findByQuery(query: EntityViewSearchQuery,
                     config?: RequestConfig): Observable<Array<EntityView>> {
    return this.http.post<Array<EntityView>>('/api/entityViews', query, defaultHttpOptionsFromConfig(config));
  }

}
