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

import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { FeaturesInfo } from '@shared/models/settings.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { TranslateService } from '@ngx-translate/core';
import { MediaBreakpoints } from '@shared/models/constants';

@Component({
  selector: 'tb-configured-features',
  templateUrl: './configured-features.component.html',
  styleUrls: ['./home-page-widget.scss', './configured-features.component.scss']
})
export class ConfiguredFeaturesComponent extends PageComponent implements OnInit, OnDestroy {

  authUser = getCurrentAuthUser(this.store);
  featuresInfo: FeaturesInfo;
  rowHeight = '50px';
  gutterSize = '12px';
  colspan = 2;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private adminService: AdminService,
              private translate: TranslateService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    const isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    const isLtMd = this.breakpointObserver.isMatched(MediaBreakpoints['lt-md']);
    this.rowHeight = isMdLg ? '22px' : '50px';
    this.gutterSize = isMdLg ? '8px' : '12px';
    this.colspan = isLtMd ? 3 : 2;
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe([MediaBreakpoints['md-lg'], MediaBreakpoints['lt-md']])
      .subscribe((state: BreakpointState) => {
          if (state.breakpoints[MediaBreakpoints['md-lg']]) {
            this.rowHeight = '22px';
            this.gutterSize = '8px';
          } else {
            this.rowHeight = '50px';
            this.gutterSize = '12px';
          }
          if (state.breakpoints[MediaBreakpoints['lt-md']]) {
            this.colspan = 3;
          } else {
            this.colspan = 2;
          }
          this.cd.markForCheck();
        }
      );
    (this.authUser.authority === Authority.SYS_ADMIN ?
    this.adminService.getFeaturesInfo() : of(null)).subscribe(
      (featuresInfo) => {
        this.featuresInfo = featuresInfo;
        this.cd.markForCheck();
      }
    );
  }

  ngOnDestroy() {
    if (this.observeBreakpointSubscription) {
      this.observeBreakpointSubscription.unsubscribe();
    }
    super.ngOnDestroy();
  }

  featureTooltip(configured: boolean): string {
    if (configured) {
      return this.translate.instant('widgets.configured-features.feature-configured');
    } else {
      return this.translate.instant('widgets.configured-features.feature-not-configured');
    }
  }
}
