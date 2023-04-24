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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { of } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { UsageInfo } from '@shared/models/usage.models';
import { UsageInfoService } from '@core/http/usage-info.service';
import { ShortNumberPipe } from '@shared/pipe/short-number.pipe';

@Component({
  selector: 'tb-usage-info-widget',
  templateUrl: './usage-info-widget.component.html',
  styleUrls: ['./home-page-widget.scss', './usage-info-widget.component.scss']
})
export class UsageInfoWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  usageInfo: UsageInfo;
  authUser = getCurrentAuthUser(this.store);

  toggleValue: 'entities' | 'apiCalls' = 'entities';

  entityItemCritical: {[key: string]: boolean} = {};
  apiCallItemCritical: {[key: string]: boolean} = {};

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private shortNumberPipe: ShortNumberPipe,
              private usageInfoService: UsageInfoService) {
    super(store);
  }

  ngOnInit() {
    (this.authUser.authority === Authority.TENANT_ADMIN ?
      this.usageInfoService.getUsageInfo() : of(null)).subscribe(
      (usageInfo) => {
        this.usageInfo = usageInfo;
        this.entityItemCritical.devices = this.isItemCritical(this.usageInfo?.devices, this.usageInfo?.maxDevices);
        this.entityItemCritical.assets = this.isItemCritical(this.usageInfo?.assets, this.usageInfo?.maxAssets);
        this.entityItemCritical.users = this.isItemCritical(this.usageInfo?.users, this.usageInfo?.maxUsers);
        this.entityItemCritical.dashboards = this.isItemCritical(this.usageInfo?.dashboards, this.usageInfo?.maxDashboards);
        this.entityItemCritical.customers = this.isItemCritical(this.usageInfo?.customers, this.usageInfo?.maxCustomers);
        this.apiCallItemCritical.transportMessages = this.isItemCritical(this.usageInfo?.transportMessages,
          this.usageInfo?.maxTransportMessages);
        this.apiCallItemCritical.jsExecutions = this.isItemCritical(this.usageInfo?.jsExecutions, this.usageInfo?.maxJsExecutions);
        this.apiCallItemCritical.alarms = this.isItemCritical(this.usageInfo?.alarms, this.usageInfo?.maxAlarms);
        this.apiCallItemCritical.emails = this.isItemCritical(this.usageInfo?.emails, this.usageInfo?.maxEmails);
        this.apiCallItemCritical.sms = this.isItemCritical(this.usageInfo?.sms, this.usageInfo?.maxSms);
        let entitiesHasCriticalItem = false;
        let apiCallsHasCriticalItem = false;
        for (const key of Object.keys(this.entityItemCritical)) {
          if (this.entityItemCritical[key]) {
            entitiesHasCriticalItem = true;
            break;
          }
        }
        for (const key of Object.keys(this.apiCallItemCritical)) {
          if (this.apiCallItemCritical[key]) {
            apiCallsHasCriticalItem = true;
            break;
          }
        }
        if (apiCallsHasCriticalItem && !entitiesHasCriticalItem) {
          this.toggleValue = 'apiCalls';
        }
        this.cd.markForCheck();
      }
    );
  }

  maxValue(max: number): number | string {
    return max ? this.shortNumberPipe.transform(max) : '∞';
  }

  progressValue(value: number, max: number): number {
    if (max && value) {
      return (value / max) * 100;
    }
    return 0;
  }

  private isItemCritical(value: number, max: number): boolean {
    if (max && value) {
      return (value / max) >= 0.85;
    } else {
      return false;
    }
  }

}
