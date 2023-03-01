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

import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '@core/auth/auth.service';
import { of, Subscription } from 'rxjs';
import { Dashboard, HomeDashboard } from '@shared/models/dashboard.models';
import { Observable } from 'rxjs/internal/Observable';
import { isDefinedAndNotNull } from '@core/utils';
import { map } from 'rxjs/operators';
import { DashboardService } from '@core/http/dashboard.service';

@Component({
  selector: 'tb-iframe-view',
  templateUrl: './iframe-view.component.html'
})
export class IFrameViewComponent implements OnInit, OnDestroy {

  @HostBinding('style.width') public width = '100%';
  @HostBinding('style.height') public height = '100%';

  safeIframeUrl: SafeResourceUrl;
  dashboard: HomeDashboard;
  loading = true;

  private sub: Subscription;

  constructor(private sanitizer: DomSanitizer,
              private route: ActivatedRoute,
              private dashboardService: DashboardService) {
  }

  ngOnInit(): void {
    this.sub = this.route.queryParams.subscribe((queryParams) => {
      this.safeIframeUrl = null;
      if (this.isDashboard(queryParams)) {
        this.loading = true;
        this.dashboard = null;
        this.resolveDashboard(queryParams, this.dashboard).subscribe((dashboard) => {
          setTimeout(() => {
            this.dashboard = dashboard;
            this.loading = false;
          });
        });
      } else {
        this.dashboard = null;
        let iframeUrl: string;
        let setAccessToken: string;
        if (queryParams.childIframeUrl) {
          iframeUrl = queryParams.childIframeUrl;
          setAccessToken = queryParams.childSetAccessToken;
        } else {
          iframeUrl = queryParams.iframeUrl;
          setAccessToken = queryParams.setAccessToken;
        }
        if (setAccessToken === 'true') {
          const accessToken = AuthService.getJwtToken();
          if (iframeUrl.indexOf('?') > -1) {
            iframeUrl += '&';
          } else {
            iframeUrl += '?';
          }
          iframeUrl += `accessToken=${accessToken}`;
        }
        this.safeIframeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(iframeUrl);
        this.loading = false;
      }
    });
  }

  private isDashboard(queryParams: Params): boolean {
    if (queryParams.childDashboardId) {
      return true;
    } else if (queryParams.childIframeUrl) {
      return false;
    } else if (queryParams.dashboardId) {
      return true;
    }
    return false;
  }

  private resolveDashboard(queryParams: Params, prevDashboard: HomeDashboard): Observable<HomeDashboard> {
    let dashboardId;
    let hideDashboardToolbar;
    if (queryParams.childDashboardId) {
      dashboardId = queryParams.childDashboardId;
      hideDashboardToolbar = isDefinedAndNotNull(queryParams.childHideDashboardToolbar) ?
        queryParams.childHideDashboardToolbar === 'true' : true;
    } else if (queryParams.dashboardId) {
      dashboardId = queryParams.dashboardId;
      hideDashboardToolbar = isDefinedAndNotNull(queryParams.hideDashboardToolbar) ? queryParams.hideDashboardToolbar === 'true' : true;
    }
    if (dashboardId) {
      if (prevDashboard?.id?.id === dashboardId) {
        return of(prevDashboard);
      } else {
        return this.dashboardService.getDashboard(dashboardId).pipe(
          map((dashboard) => {
            return {...dashboard, hideDashboardToolbar};
          })
        );
      }
    } else {
      return of(null);
    }
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

}
