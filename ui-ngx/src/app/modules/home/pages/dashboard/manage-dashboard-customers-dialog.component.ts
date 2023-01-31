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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { DashboardService } from '@core/http/dashboard.service';
import { forkJoin, Observable, of } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export type ManageDashboardCustomersActionType = 'assign' | 'manage' | 'unassign';

export interface ManageDashboardCustomersDialogData {
  actionType: ManageDashboardCustomersActionType;
  dashboardIds: Array<string>;
  assignedCustomersIds?: Array<string>;
}

@Component({
  selector: 'tb-manage-dashboard-customers-dialog',
  templateUrl: './manage-dashboard-customers-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardCustomersDialogComponent}],
  styleUrls: []
})
export class ManageDashboardCustomersDialogComponent extends
  DialogComponent<ManageDashboardCustomersDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  dashboardCustomersFormGroup: FormGroup;

  submitted = false;

  entityType = EntityType;

  titleText: string;
  labelText: string;
  actionName: string;

  assignedCustomersIds: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardCustomersDialogData,
              private dashboardService: DashboardService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDashboardCustomersDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.assignedCustomersIds = data.assignedCustomersIds || [];
    switch (data.actionType) {
      case 'assign':
        this.titleText = 'dashboard.assign-to-customers';
        this.labelText = 'dashboard.assign-to-customers-text';
        this.actionName = 'action.assign';
        break;
      case 'manage':
        this.titleText = 'dashboard.manage-assigned-customers';
        this.labelText = 'dashboard.assigned-customers';
        this.actionName = 'action.update';
        break;
      case 'unassign':
        this.titleText = 'dashboard.unassign-from-customers';
        this.labelText = 'dashboard.unassign-from-customers-text';
        this.actionName = 'action.unassign';
        break;
    }
  }

  ngOnInit(): void {
    this.dashboardCustomersFormGroup = this.fb.group({
      assignedCustomerIds: [[...this.assignedCustomersIds]]
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    this.submitted = true;
    const customerIds: Array<string> = this.dashboardCustomersFormGroup.get('assignedCustomerIds').value;
    const tasks: Observable<any>[] = [];

    this.data.dashboardIds.forEach(
      (dashboardId) => {
        tasks.push(this.getManageDashboardCustomersTask(dashboardId, customerIds));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  private getManageDashboardCustomersTask(dashboardId: string, customerIds: Array<string>): Observable<any> {
    /*switch (this.data.actionType) {
      case 'assign':
        return this.dashboardService.addDashboardCustomers(dashboardId, customerIds);
      case 'manage':
        return this.dashboardService.updateDashboardCustomers(dashboardId, customerIds);
      case 'unassign':
        return this.dashboardService.removeDashboardCustomers(dashboardId, customerIds);
        break;
    }*/
    return of(null);
  }
}
