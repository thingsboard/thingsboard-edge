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

import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CMAssigneeType } from '@shared/models/custom-menu.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

export interface CustomMenuIsAssignedDialogData {
  assigneeType: CMAssigneeType;
  assigneeList: EntityInfoData[];
}

@Component({
  selector: 'tb-custom-menu-is-assigned-dialog',
  templateUrl: './custom-menu-is-assigned-dialog.component.html',
  styleUrls: ['./custom-menu-is-assigned-dialog.component.scss']
})
export class CustomMenuIsAssignedDialogComponent extends
  DialogComponent<CustomMenuIsAssignedDialogComponent, boolean> implements OnInit {

  message: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CustomMenuIsAssignedDialogData,
              public dialogRef: MatDialogRef<CustomMenuIsAssignedDialogComponent, boolean>,
              public translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    if (this.data.assigneeType === CMAssigneeType.USERS) {
      this.message = this.translate.instant('custom-menu.delete-custom-menu-user-list-text');
    } else if (this.data.assigneeType === CMAssigneeType.CUSTOMERS) {
      this.message = this.translate.instant('custom-menu.delete-custom-menu-customer-list-text');
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }

  delete() {
    this.dialogRef.close(true);
  }
}
