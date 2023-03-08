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
import { User, UserEmailInfo } from '@shared/models/user.model';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { isDefined } from '@core/utils';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantAdmins(tenantId: string, pageLink: PageLink,
                         config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/tenant/${tenantId}/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerUsers(customerId: string, pageLink: PageLink,
                          config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/customer/${customerId}/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllCustomerUsers(pageLink: PageLink,
                             config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/customer/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getUser(userId: string, config?: RequestConfig): Observable<User> {
    return this.http.get<User>(`/api/user/${userId}`, defaultHttpOptionsFromConfig(config));
  }

  public getUsers(userIds: Array<string>, config?: RequestConfig): Observable<Array<User>> {
    return this.http.get<Array<User>>(`/api/users?userIds=${userIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((users) => sortEntitiesByIds(users, userIds))
    );
  }

  public getUserUsers(pageLink: PageLink,
                      config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/user/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveUser(user: User, sendActivationMail: boolean = false,
                  entityGroupId?: string,
                  config?: RequestConfig): Observable<User> {
    let url = `/api/user?sendActivationMail=${sendActivationMail}`;
    if (entityGroupId) {
      url += `&entityGroupId=${entityGroupId}`;
    }
    return this.http.post<User>(url, user, defaultHttpOptionsFromConfig(config));
  }

  public deleteUser(userId: string, config?: RequestConfig) {
    return this.http.delete(`/api/user/${userId}`, defaultHttpOptionsFromConfig(config));
  }

  public getActivationLink(userId: string, config?: RequestConfig): Observable<string> {
    return this.http.get(`/api/user/${userId}/activationLink`,
      {...{responseType: 'text'}, ...defaultHttpOptionsFromConfig(config)});
  }

  public sendActivationEmail(email: string, config?: RequestConfig) {
    const encodeEmail = encodeURIComponent(email);
    return this.http.post(`/api/user/sendActivationMail?email=${encodeEmail}`, null, defaultHttpOptionsFromConfig(config));
  }

  public setUserCredentialsEnabled(userId: string, userCredentialsEnabled?: boolean, config?: RequestConfig): Observable<any> {
    let url = `/api/user/${userId}/userCredentialsEnabled`;
    if (isDefined(userCredentialsEnabled)) {
      url += `?userCredentialsEnabled=${userCredentialsEnabled}`;
    }
    return this.http.post<User>(url, null, defaultHttpOptionsFromConfig(config));
  }

  public findUsersByQuery(pageLink: PageLink, config?: RequestConfig) : Observable<PageData<UserEmailInfo>> {
    return this.http.get<PageData<UserEmailInfo>>(`/api/users/info${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

}
