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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { Subscription } from 'rxjs';
import { BreakpointId } from '@shared/models/dashboard.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

@Component({
  selector: 'tb-select-dashboard-breakpoint',
  templateUrl: './select-dashboard-breakpoint.component.html',
  styleUrls: ['./select-dashboard-breakpoint.component.scss']
})
export class SelectDashboardBreakpointComponent implements OnInit, OnDestroy {

  @Input()
  dashboardCtrl: DashboardPageComponent;

  selectedBreakpoint: BreakpointId = 'default';

  breakpointIds: Array<BreakpointId> = ['default'];

  private layoutDataChanged$: Subscription;

  constructor(private dashboardUtils: DashboardUtilsService) {
  }

  ngOnInit() {
    this.layoutDataChanged$ = this.dashboardCtrl.layouts.main.layoutCtx.layoutDataChanged.subscribe(() => {
      if (this.dashboardCtrl.layouts.main.layoutCtx.layoutData) {
        this.breakpointIds = Object.keys(this.dashboardCtrl.layouts.main.layoutCtx?.layoutData) as BreakpointId[];
        this.breakpointIds.sort((a, b) => {
          const aMaxWidth = this.dashboardUtils.getBreakpointInfoById(a)?.maxWidth || Infinity;
          const bMaxWidth = this.dashboardUtils.getBreakpointInfoById(b)?.maxWidth || Infinity;
          return bMaxWidth - aMaxWidth;
        });
        if (this.breakpointIds.indexOf(this.dashboardCtrl.layouts.main.layoutCtx.breakpoint) > -1) {
          this.selectedBreakpoint = this.dashboardCtrl.layouts.main.layoutCtx.breakpoint;
        } else {
          this.selectedBreakpoint = 'default';
          this.dashboardCtrl.layouts.main.layoutCtx.breakpoint = this.selectedBreakpoint;
        }
      }
    });
  }

  ngOnDestroy() {
    this.layoutDataChanged$.unsubscribe();
  }

  selectLayoutChanged() {
    this.dashboardUtils.updatedLayoutForBreakpoint(this.dashboardCtrl.layouts.main, this.selectedBreakpoint);
    this.dashboardCtrl.updateLayoutSizes();
  }

  getName(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointName(breakpointId);
  }

  getIcon(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointIcon(breakpointId);
  }

  getSizeDescription(breakpointId: BreakpointId): string {
    return this.dashboardUtils.getBreakpointSizeDescription(breakpointId);
  }
}
