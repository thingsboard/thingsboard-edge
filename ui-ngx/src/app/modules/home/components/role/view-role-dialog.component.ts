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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { Role } from '@shared/models/role.models';

export interface ViewRoleDialogData {
  role: Role;
}

@Component({
  selector: 'tb-view-role-dialog',
  templateUrl: './view-role-dialog.component.html',
  styleUrls: []
})
export class ViewRoleDialogComponent
  extends DialogComponent<ViewRoleDialogComponent> implements OnInit {

  roleFormGroup: FormGroup;

  role = this.data.role;

  roleType = RoleType;
  roleTypes = Object.keys(RoleType);
  roleTypeTranslations = roleTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ViewRoleDialogData,
              public dialogRef: MatDialogRef<ViewRoleDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.roleFormGroup = this.fb.group(
      {
        name: [this.role.name, []],
        type: [this.role.type, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [this.role.additionalInfo ? this.role.additionalInfo.description : ''],
          }
        ),
        genericPermissions: [this.role.type === RoleType.GENERIC ? this.role.permissions : null, []],
        groupPermissions: [this.role.type === RoleType.GROUP ? this.role.permissions : null, []]
      }
    );
    this.roleFormGroup.disable();
  }

  close(): void {
    this.dialogRef.close();
  }

}
