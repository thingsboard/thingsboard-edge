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

import { Inject, Injectable } from '@angular/core';
import { defaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Dashboard, DashboardInfo, HomeDashboard, HomeDashboardInfo } from '@shared/models/dashboard.models';
import { WINDOW } from '@core/services/window.service';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, publishReplay, refCount } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { Operation } from '@shared/models/security.models';
import { EntityGroup, ShortEntityView } from '@shared/models/entity-group.models';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  stDiffObservable: Observable<number>;
  currentUrl: string;

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(WINDOW) private window: Window
  ) {
    this.currentUrl = this.router.url.split('?')[0];
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        const newUrl = this.router.url.split('?')[0];
        if (this.currentUrl !== newUrl) {
          this.stDiffObservable = null;
          this.currentUrl = newUrl;
        }
      }
    );
  }

  public getTenantDashboards(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/tenant/dashboards${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantDashboardsByTenantId(tenantId: string, pageLink: PageLink,
                                       config?: RequestConfig): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/tenant/${tenantId}/dashboards${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getDashboards(dashboardIds: string[], config?: RequestConfig): Observable<Array<DashboardInfo>> {
    return this.http.get<Array<DashboardInfo>>(`/api/dashboards?dashboardIds=${dashboardIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((dashboards) => sortEntitiesByIds(dashboards, dashboardIds))
    );
  }

  public getUserDashboards(userId: string, operation: Operation,
                           pageLink: PageLink, config?: RequestConfig): Observable<PageData<DashboardInfo>> {
    let url = `/api/user/dashboards${pageLink.toQuery()}`;
    if (userId) {
      url += `&userId=${userId}`;
    }
    if (operation) {
      url += `&operation=${operation}`;
    }
    return this.http.get<PageData<DashboardInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getGroupDashboards(groupId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/entityGroup/${groupId}/dashboards${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getDashboard(dashboardId: string, config?: RequestConfig): Observable<Dashboard> {
    return this.http.get<Dashboard>(`/api/dashboard/${dashboardId}`, defaultHttpOptionsFromConfig(config));
  }

  public getDashboardInfo(dashboardId: string, config?: RequestConfig): Observable<DashboardInfo> {
    return this.http.get<DashboardInfo>(`/api/dashboard/info/${dashboardId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveDashboard(dashboard: Dashboard, entityGroupId?: string, config?: RequestConfig): Observable<Dashboard> {
    let url = '/api/dashboard';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<Dashboard>(url, dashboard, defaultHttpOptionsFromConfig(config));
  }

  public deleteDashboard(dashboardId: string, config?: RequestConfig) {
    return this.http.delete(`/api/dashboard/${dashboardId}`, defaultHttpOptionsFromConfig(config));
  }

/*  public assignDashboardToCustomer(customerId: string, dashboardId: string,
                                   config?: RequestConfig): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/customer/${customerId}/dashboard/${dashboardId}`,
      null, defaultHttpOptionsFromConfig(config));
  }

  public unassignDashboardFromCustomer(customerId: string, dashboardId: string,
                                       config?: RequestConfig) {
    return this.http.delete(`/api/customer/${customerId}/dashboard/${dashboardId}`, defaultHttpOptionsFromConfig(config));
  }

  public makeDashboardPublic(dashboardId: string, config?: RequestConfig): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/customer/public/dashboard/${dashboardId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public makeDashboardPrivate(dashboardId: string, config?: RequestConfig): Observable<Dashboard> {
    return this.http.delete<Dashboard>(`/api/customer/public/dashboard/${dashboardId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public updateDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                                  config?: RequestConfig): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers`, customerIds,
      defaultHttpOptionsFromConfig(config));
  }

  public addDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                               config?: RequestConfig): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers/add`, customerIds,
      defaultHttpOptionsFromConfig(config));
  }

  public removeDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                                  config?: RequestConfig): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers/remove`, customerIds,
      defaultHttpOptionsFromConfig(config));
  }*/

  public getPublicDashboardLink(dashboard: DashboardInfo | ShortEntityView, entityGroup: EntityGroup): string | null {
      const publicCustomerId = entityGroup.additionalInfo.publicCustomerId;
      let url = this.window.location.protocol + '//' + this.window.location.hostname;
      const port = this.window.location.port;
      if (port && port.length > 0 && port !== '80' && port !== '443') {
         url += ':' + port;
      }
      url += `/dashboard/${dashboard.id.id}?publicId=${publicCustomerId}`;
      return url;
  }

  public getHomeDashboard(config?: RequestConfig): Observable<HomeDashboard> {
    return this.http.get<HomeDashboard>('/api/dashboard/home', defaultHttpOptionsFromConfig(config));
  }

  public getTenantHomeDashboardInfo(config?: RequestConfig): Observable<HomeDashboardInfo> {
    return this.http.get<HomeDashboardInfo>('/api/tenant/dashboard/home/info', defaultHttpOptionsFromConfig(config));
  }

  public getCustomerHomeDashboardInfo(config?: RequestConfig): Observable<HomeDashboardInfo> {
    return this.http.get<HomeDashboardInfo>('/api/customer/dashboard/home/info', defaultHttpOptionsFromConfig(config));
  }

  public setTenantHomeDashboardInfo(homeDashboardInfo: HomeDashboardInfo, config?: RequestConfig): Observable<any> {
    return this.http.post<any>('/api/tenant/dashboard/home/info', homeDashboardInfo,
      defaultHttpOptionsFromConfig(config));
  }

  public setCustomerHomeDashboardInfo(homeDashboardInfo: HomeDashboardInfo, config?: RequestConfig): Observable<any> {
    return this.http.post<any>('/api/customer/dashboard/home/info', homeDashboardInfo,
      defaultHttpOptionsFromConfig(config));
  }

  public getServerTimeDiff(): Observable<number> {
    if (!this.stDiffObservable) {
      const url = '/api/dashboard/serverTime';
      const ct1 = Date.now();
      this.stDiffObservable = this.http.get<number>(url, defaultHttpOptions(true)).pipe(
        map((st) => {
          const ct2 = Date.now();
          const stDiff = Math.ceil(st - (ct1 + ct2) / 2);
          return stDiff;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.stDiffObservable;
  }

}
