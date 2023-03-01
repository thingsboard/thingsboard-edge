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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { SolutionInstallResponse, TenantSolutionTemplateInstructions } from '@shared/models/solution-template.models';

export interface SolutionInstallDialogData {
  solutionInstallResponse: TenantSolutionTemplateInstructions | SolutionInstallResponse;
  instructions: boolean;
  showMainDashboardButton: boolean;
}

@Component({
  selector: 'tb-solution-install-dialog',
  templateUrl: './solution-install-dialog.component.html',
  styleUrls: ['./solution-install-dialog.component.scss']
})
export class SolutionInstallDialogComponent extends
  DialogComponent<SolutionInstallDialogComponent> implements OnInit {

  solutionInstallResponse = this.data.solutionInstallResponse as SolutionInstallResponse;
  instructions = this.data.instructions;
  showMainDashboardButton = this.data.showMainDashboardButton;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SolutionInstallDialogData,
              public dialogRef: MatDialogRef<SolutionInstallDialogComponent>) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  gotoMainDashboard(): void {
    if (this.solutionInstallResponse.dashboardGroupId && this.solutionInstallResponse.dashboardId) {
      const url = this.router.createUrlTree(['dashboardGroups', this.solutionInstallResponse.dashboardGroupId.id,
        this.solutionInstallResponse.dashboardId.id]);
      this.dialogRef.close();
      this.router.navigateByUrl(url);
    }
  }
}
