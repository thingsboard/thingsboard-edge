///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
