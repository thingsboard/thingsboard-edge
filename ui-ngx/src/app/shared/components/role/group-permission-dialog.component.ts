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
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { GroupPermission, GroupPermissionFullInfo } from '@shared/models/group-permission.models';
import { RoleService } from '@core/http/role.service';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType } from '@shared/models/entity-type.models';

export interface GroupPermissionDialogData {
  isUserGroup: boolean;
  isAdd: boolean;
  groupPermission: GroupPermissionFullInfo;
  groupPermissionsMode: 'group' | 'registration';
}

@Component({
  selector: 'tb-group-permission-dialog',
  templateUrl: './group-permission-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: GroupPermissionDialogComponent}],
  styleUrls: ['./group-permission-dialog.component.scss']
})
export class GroupPermissionDialogComponent
  extends DialogComponent<GroupPermissionDialogComponent, boolean | GroupPermission> implements OnInit, ErrorStateMatcher {

  groupPermissionFormGroup: FormGroup;

  isAdd = this.data.isAdd;
  isUserGroup = this.data.isUserGroup;
  groupPermission = this.data.groupPermission;

  roleType = RoleType;
  roleTypes = Object.keys(RoleType);
  roleTypeTranslations = roleTypeTranslationMap;

  entityType = EntityType;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: GroupPermissionDialogData,
              private roleService: RoleService,
              private userPermissionsService: UserPermissionsService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<GroupPermissionDialogComponent, boolean | GroupPermission>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    if (this.isUserGroup) {
      if (this.groupPermission.role && this.groupPermission.role.type !== RoleType.GROUP) {
        this.groupPermission.entityGroupId = null;
        this.groupPermission.entityGroupType = null;
      }
      if (this.isAdd || this.data.groupPermissionsMode === 'registration') {
        this.groupPermission.entityGroupOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    } else {
      if (this.isAdd) {
        this.groupPermission.role = {
          type: RoleType.GROUP,
          name: null,
          permissions: null
        }
        this.groupPermission.userGroupOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    }
  }

  ngOnInit(): void {
    this.groupPermissionFormGroup = this.fb.group({
      role: this.fb.group(
        {
          type: [null, Validators.required],
          id: [null, Validators.required]
        }
      ),
      entityGroupOwnerId: [null, Validators.required],
      entityGroupId: [null, Validators.required],
      userGroupOwnerId: [null, Validators.required],
      userGroupId: [null, Validators.required],
    });
    this.groupPermissionFormGroup.reset(this.groupPermission, {emitEvent: false});
    this.updateEnabledState();
    if (this.isUserGroup) {
      this.groupPermissionFormGroup.get('role.type').valueChanges.subscribe(() => {
        this.updateEnabledState();
      });
    }
  }

  private updateEnabledState() {
    if (this.isUserGroup) {
      this.groupPermissionFormGroup.get('role.type').enable({emitEvent: false});
      this.groupPermissionFormGroup.get('userGroupOwnerId').disable({emitEvent: false});
      this.groupPermissionFormGroup.get('userGroupId').disable({emitEvent: false});
    } else {
      this.groupPermissionFormGroup.get('role.type').disable({emitEvent: false});
      this.groupPermissionFormGroup.get('userGroupOwnerId').enable({emitEvent: false});
      this.groupPermissionFormGroup.get('userGroupId').enable({emitEvent: false});
    }
    const roleType: RoleType = this.groupPermissionFormGroup.get('role.type').value;
    if (this.isUserGroup && roleType === RoleType.GROUP) {
      this.groupPermissionFormGroup.get('entityGroupOwnerId').enable({emitEvent: false});
      this.groupPermissionFormGroup.get('entityGroupId').enable({emitEvent: false});
    } else {
      this.groupPermissionFormGroup.get('entityGroupOwnerId').disable({emitEvent: false});
      this.groupPermissionFormGroup.get('entityGroupId').disable({emitEvent: false});
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.submitted = true;
    if (this.groupPermissionFormGroup.valid) {
      this.groupPermission = {...this.groupPermission, ...this.groupPermissionFormGroup.getRawValue()};
      this.groupPermission.roleId = {
        entityType: EntityType.ROLE,
        id: this.groupPermission.role.id.id
      };
      if (this.groupPermission.role.type !== RoleType.GROUP) {
        this.groupPermission.entityGroupId = null;
        this.groupPermission.entityGroupType = null;
      }
      if (this.data.groupPermissionsMode === 'group') {
        this.roleService.saveGroupPermission(this.groupPermission).subscribe(() => {
          this.dialogRef.close(true);
        });
      } else {
        this.dialogRef.close(this.groupPermission);
      }
    }
  }
}
