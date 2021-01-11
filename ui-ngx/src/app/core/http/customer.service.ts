///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Customer, ShortCustomerInfo } from '@shared/models/customer.model';
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

  public getShortCustomerInfo(customerId: string, config?: RequestConfig): Observable<ShortCustomerInfo> {
    return this.http.get<ShortCustomerInfo>(`/api/customer/${customerId}/shortInfo`, defaultHttpOptionsFromConfig(config));
  }

}
