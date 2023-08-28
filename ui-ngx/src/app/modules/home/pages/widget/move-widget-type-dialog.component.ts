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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

export interface MoveWidgetTypeDialogResult {
  bundleId: string;
  bundleAlias: string;
}

export interface MoveWidgetTypeDialogData {
  currentBundleId: string;
}

@Component({
  selector: 'tb-move-widget-type-dialog',
  templateUrl: './move-widget-type-dialog.component.html',
  styleUrls: []
})
export class MoveWidgetTypeDialogComponent extends
  DialogComponent<MoveWidgetTypeDialogComponent, MoveWidgetTypeDialogResult> implements OnInit {

  moveWidgetTypeFormGroup: UntypedFormGroup;

  bundlesScope: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MoveWidgetTypeDialogData,
              public dialogRef: MatDialogRef<MoveWidgetTypeDialogComponent, MoveWidgetTypeDialogResult>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    const authUser = getCurrentAuthUser(store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      this.bundlesScope = 'tenant';
    } else {
      this.bundlesScope = 'system';
    }
  }

  ngOnInit(): void {
    this.moveWidgetTypeFormGroup = this.fb.group({
      widgetsBundle: [null, [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  move(): void {
    const widgetsBundle: WidgetsBundle = this.moveWidgetTypeFormGroup.get('widgetsBundle').value;
    const result: MoveWidgetTypeDialogResult = {
      bundleId: widgetsBundle.id.id,
      bundleAlias: widgetsBundle.alias
    };
    this.dialogRef.close(result);
  }
}
