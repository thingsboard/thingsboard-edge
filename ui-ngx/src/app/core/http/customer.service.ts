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
import { Customer, CustomerInfo, ShortCustomerInfo } from '@shared/models/customer.model';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  constructor(
    private http: HttpClient
  ) { }

  public getCustomers(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Customer>> {
    return this.http.get<PageData<Customer>>(`/api/customers${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomer(customerId: string, config?: RequestConfig): Observable<Customer> {
    return this.http.get<Customer>(`/api/customer/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveCustomer(customer: Customer, entityGroupId?: string, config?: RequestConfig): Observable<Customer> {
    let url = '/api/customer';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<Customer>(url, customer, defaultHttpOptionsFromConfig(config));
  }

  public deleteCustomer(customerId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomersByIds(customerIds: Array<string>, config?: RequestConfig): Observable<Array<Customer>> {
    return this.http.get<Array<Customer>>(`/api/customers?customerIds=${customerIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((customers) => sortEntitiesByIds(customers, customerIds))
    );
  }

  public getUserCustomers(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Customer>> {
    return this.http.get<PageData<Customer>>(`/api/user/customers${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllCustomerInfos(includeCustomers: boolean,
                             pageLink: PageLink, config?: RequestConfig): Observable<PageData<CustomerInfo>> {
    let url = `/api/customerInfos/all${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<CustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerCustomerInfos(includeCustomers: boolean, customerId: string,
                                  pageLink: PageLink, config?: RequestConfig): Observable<PageData<CustomerInfo>> {
    let url = `/api/customer/${customerId}/customerInfos${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<CustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getShortCustomerInfo(customerId: string, config?: RequestConfig): Observable<ShortCustomerInfo> {
    return this.http.get<ShortCustomerInfo>(`/api/customer/${customerId}/shortInfo`, defaultHttpOptionsFromConfig(config));
  }

}
