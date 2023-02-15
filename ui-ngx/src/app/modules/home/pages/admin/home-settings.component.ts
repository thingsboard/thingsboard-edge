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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { DashboardService } from '@core/http/dashboard.service';
import { HomeDashboardInfo } from '@shared/models/dashboard.models';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Observable } from 'rxjs/internal/Observable';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-home-settings',
  templateUrl: './home-settings.component.html',
  styleUrls: ['./home-settings.component.scss', './settings-card.scss']
})
export class HomeSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  homeSettings: FormGroup;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private dashboardService: DashboardService,
              public fb: FormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.homeSettings = this.fb.group({
      dashboardId: [null],
      hideDashboardToolbar: [true]
    });
    if (this.readonly) {
      this.homeSettings.disable({emitEvent: false});
    }

    let homeDashboardInfoObservable: Observable<HomeDashboardInfo>;

    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      homeDashboardInfoObservable = this.dashboardService.getTenantHomeDashboardInfo();
    } else if (this.authUser.authority === Authority.CUSTOMER_USER) {
      homeDashboardInfoObservable = this.dashboardService.getCustomerHomeDashboardInfo();
    }
    if (homeDashboardInfoObservable) {
      homeDashboardInfoObservable.subscribe(
        (homeDashboardInfo) => {
          this.setHomeDashboardInfo(homeDashboardInfo);
        }
      );
    }
  }

  save(): void {
    const strDashboardId = this.homeSettings.get('dashboardId').value;
    const dashboardId: DashboardId = strDashboardId ? new DashboardId(strDashboardId) : null;
    const hideDashboardToolbar = this.homeSettings.get('hideDashboardToolbar').value;
    const homeDashboardInfo: HomeDashboardInfo = {
      dashboardId,
      hideDashboardToolbar
    };
    let setHomeDashboardInfoObservable: Observable<any>;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      setHomeDashboardInfoObservable = this.dashboardService.setTenantHomeDashboardInfo(homeDashboardInfo);
    } else if (this.authUser.authority === Authority.CUSTOMER_USER) {
      setHomeDashboardInfoObservable = this.dashboardService.setCustomerHomeDashboardInfo(homeDashboardInfo);
    }
    if (setHomeDashboardInfoObservable) {
      setHomeDashboardInfoObservable.subscribe(
        () => {
          this.setHomeDashboardInfo(homeDashboardInfo);
        }
      );
    }
  }

  confirmForm(): FormGroup {
    return this.homeSettings;
  }

  private setHomeDashboardInfo(homeDashboardInfo: HomeDashboardInfo) {
    this.homeSettings.reset({
      dashboardId: homeDashboardInfo?.dashboardId?.id,
      hideDashboardToolbar: isDefinedAndNotNull(homeDashboardInfo?.hideDashboardToolbar) ?
        homeDashboardInfo?.hideDashboardToolbar : true
    });
  }

}
