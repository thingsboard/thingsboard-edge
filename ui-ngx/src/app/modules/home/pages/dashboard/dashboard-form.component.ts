///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityComponent} from '../../components/entity/entity.component';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {TranslateService} from '@ngx-translate/core';
import {
  Dashboard,
  isPublicDashboard,
  getDashboardAssignedCustomersText,
  isCurrentPublicDashboardCustomer,
  DashboardInfo
} from '@shared/models/dashboard.models';
import {DashboardService} from '@core/http/dashboard.service';

@Component({
  selector: 'tb-dashboard-form',
  templateUrl: './dashboard-form.component.html',
  styleUrls: ['./dashboard-form.component.scss']
})
export class DashboardFormComponent extends EntityComponent<Dashboard> {

  dashboardScope: 'tenant' | 'customer' | 'customer_user';
  customerId: string;

  publicLink: string;
  assignedCustomersText: string;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dashboardService: DashboardService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.dashboardScope = this.entitiesTableConfig.componentsData.dashboardScope;
    this.customerId = this.entitiesTableConfig.componentsData.customerId;
    super.ngOnInit();
  }

  isPublic(entity: Dashboard): boolean {
    return isPublicDashboard(entity);
  }

  isCurrentPublicCustomer(entity: Dashboard): boolean {
    return isCurrentPublicDashboardCustomer(entity, this.customerId);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Dashboard): FormGroup {
    this.updateFields(entity);
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required]],
        configuration: this.fb.group(
          {
            description: [entity && entity.configuration ? entity.configuration.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: Dashboard) {
    this.updateFields(entity);
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({configuration: {description: entity.configuration ? entity.configuration.description : ''}});
  }

  onPublicLinkCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
     {
        message: this.translate.instant('dashboard.public-link-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private updateFields(entity: Dashboard): void {
    this.assignedCustomersText = getDashboardAssignedCustomersText(entity);
    // this.publicLink = this.dashboardService.getPublicDashboardLink(entity);
  }
}
