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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { BreakpointId } from '@shared/models/dashboard.models';

export interface AddNewBreakpointDialogData {
  allowBreakpointIds: string[];
  selectedBreakpointIds: string[];
}

export interface AddNewBreakpointDialogResult {
  newBreakpointId: BreakpointId;
  copyFrom: BreakpointId;
}

@Component({
  selector: 'add-new-breakpoint-dialog',
  templateUrl: './add-new-breakpoint-dialog.component.html',
})
export class AddNewBreakpointDialogComponent extends DialogComponent<AddNewBreakpointDialogComponent, AddNewBreakpointDialogResult> {

  addBreakpointFormGroup: FormGroup;

  allowBreakpointIds = [];
  selectedBreakpointIds = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private fb: FormBuilder,
              @Inject(MAT_DIALOG_DATA) private data: AddNewBreakpointDialogData,
              protected dialogRef: MatDialogRef<AddNewBreakpointDialogComponent, AddNewBreakpointDialogResult>,
              private dashboardUtils: DashboardUtilsService,) {

    super(store, router, dialogRef);

    this.allowBreakpointIds = this.data.allowBreakpointIds;
    this.selectedBreakpointIds = this.data.selectedBreakpointIds;

    this.addBreakpointFormGroup = this.fb.group({
      newBreakpointId: [{value: this.allowBreakpointIds[0], disabled: this.allowBreakpointIds.length === 1}],
      copyFrom: [{value: 'default', disabled: this.selectedBreakpointIds.length === 1}],
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.addBreakpointFormGroup.getRawValue());
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
