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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';

import { HomeLinksComponent } from './home-links.component';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { map } from 'rxjs/operators';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import sysAdminHomePageDashboardJson from '!raw-loader!./sys_admin_home_page.raw';
import tenantAdminHomePageDashboardJson from '!raw-loader!./tenant_admin_home_page.raw';
// import customerUserHomePageDashboardJson from '!raw-loader!./customer_user_home_page.raw';

@Injectable()
export class HomeDashboardResolver implements Resolve<HomeDashboard> {

  constructor(private dashboardService: DashboardService,
              private store: Store<AppState>) {
  }

  resolve(): Observable<HomeDashboard> {
    return this.dashboardService.getHomeDashboard().pipe(
      map((dashboard) => {
        if (!dashboard) {
          const authority = getCurrentAuthUser(this.store).authority;
          switch (authority) {
            case Authority.SYS_ADMIN:
              dashboard = JSON.parse(sysAdminHomePageDashboardJson);
              break;
            case Authority.TENANT_ADMIN:
              dashboard = JSON.parse(tenantAdminHomePageDashboardJson);
              break;
            case Authority.CUSTOMER_USER:
              // dashboard = JSON.parse(customerUserHomePageDashboardJson);
              break;
          }
          if (dashboard) {
            dashboard.hideDashboardToolbar = true;
          }
        }
        return dashboard;
      })
    );
  }
}

const routes: Routes = [
  {
    path: 'home',
    component: HomeLinksComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'home.home',
      breadcrumb: {
        label: 'home.home',
        icon: 'home'
      }
    },
    resolve: {
      homeDashboard: HomeDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    HomeDashboardResolver
  ]
})
export class HomeLinksRoutingModule { }
