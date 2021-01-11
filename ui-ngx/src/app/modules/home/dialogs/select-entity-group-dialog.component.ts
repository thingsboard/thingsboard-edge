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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { EntityGroup, EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BroadcastService } from '@core/services/broadcast.service';

export interface SelectEntityGroupDialogResult {
  groupId: string;
  group?: EntityGroupInfo;
  isNew: boolean;
}

export interface SelectEntityGroupDialogData {
  ownerId: EntityId;
  targetGroupType: EntityType;
  selectEntityGroupTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeGroupIds: Array<string>;
  onEntityGroupSelected: (result: SelectEntityGroupDialogResult) => Observable<boolean>;
}

@Component({
  selector: 'tb-select-entity-group-dialog',
  templateUrl: './select-entity-group-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: SelectEntityGroupDialogComponent}],
  styleUrls: ['./select-entity-group-dialog.component.scss']
})
export class SelectEntityGroupDialogComponent extends
  DialogComponent<SelectEntityGroupDialogComponent, SelectEntityGroupDialogResult> implements OnInit, ErrorStateMatcher {

  selectEntityGroupFormGroup: FormGroup;

  submitted = false;

  ownerId: EntityId;
  targetGroupType: EntityType;
  selectEntityGroupTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeGroupIds: Array<string>;
  onEntityGroupSelected: (result: SelectEntityGroupDialogResult) => Observable<boolean>;

  createEnabled: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected userPermissionsService: UserPermissionsService,
              protected entityGroupService: EntityGroupService,
              protected broadcast: BroadcastService,
              @Inject(MAT_DIALOG_DATA) public data: SelectEntityGroupDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SelectEntityGroupDialogComponent, SelectEntityGroupDialogResult>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.ownerId = data.ownerId;
    this.targetGroupType = data.targetGroupType;
    this.selectEntityGroupTitle = data.selectEntityGroupTitle;
    this.confirmSelectTitle = data.confirmSelectTitle;
    this.placeholderText = data.placeholderText;
    this.notFoundText = data.notFoundText;
    this.requiredText = data.requiredText;
    this.excludeGroupIds = data.excludeGroupIds;
    this.onEntityGroupSelected = data.onEntityGroupSelected;

    this.createEnabled = this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.CREATE, this.targetGroupType);
  }

  ngOnInit(): void {
    this.selectEntityGroupFormGroup = this.fb.group({
      addToGroupType: [0],
      targetEntityGroupId: [null, [Validators.required]],
      newEntityGroupName: [null, [Validators.required]]
    });
    this.updateDisabledState();
    if (this.createEnabled) {
      this.selectEntityGroupFormGroup.get('addToGroupType').valueChanges.subscribe(
        () => {
          this.updateDisabledState();
        }
      );
    }
  }

  private updateDisabledState() {
    if (!this.createEnabled) {
      this.selectEntityGroupFormGroup.get('newEntityGroupName').disable();
    } else {
      const addToGroupType: number = this.selectEntityGroupFormGroup.get('addToGroupType').value;
      if (addToGroupType === 0) {
        this.selectEntityGroupFormGroup.get('targetEntityGroupId').enable();
        this.selectEntityGroupFormGroup.get('newEntityGroupName').disable();
      } else {
        this.selectEntityGroupFormGroup.get('targetEntityGroupId').disable();
        this.selectEntityGroupFormGroup.get('newEntityGroupName').enable();
      }
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  selectEntityGroup(): void {
    this.submitted = true;
    const addToGroupType: number = this.selectEntityGroupFormGroup.get('addToGroupType').value;
    if (addToGroupType === 1) {
      const newEntityGroupName: string = this.selectEntityGroupFormGroup.get('newEntityGroupName').value;
      const newEntityGroup: EntityGroup = {
        name: newEntityGroupName,
        type: this.targetGroupType,
        ownerId: this.ownerId
      };
      this.entityGroupService.saveEntityGroup(newEntityGroup).subscribe((entityGroup) => {
        this.groupSelected({groupId: entityGroup.id.id, group: entityGroup, isNew: true});
        if (this.userPermissionsService.isDirectlyOwnedGroup(entityGroup)) {
          this.broadcast.broadcast(`${this.targetGroupType}changed`);
        }
      });
    } else {
      const targetEntityGroupId: string = this.selectEntityGroupFormGroup.get('targetEntityGroupId').value;
      this.groupSelected({groupId: targetEntityGroupId, isNew: false});
    }
  }

  private groupSelected(result: SelectEntityGroupDialogResult) {
    if (this.onEntityGroupSelected) {
      this.onEntityGroupSelected(result).subscribe((res) => {
        if (res) {
          this.dialogRef.close(result);
        }
      });
    } else {
      this.dialogRef.close(result);
    }
  }
}
