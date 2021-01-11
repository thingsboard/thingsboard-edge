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
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ShareGroupRequest } from '@shared/models/entity-group.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface ShareEntityGroupDialogData {
  entityGroupId: EntityGroupId;
}

@Component({
  selector: 'tb-share-entity-group-dialog',
  templateUrl: './share-entity-group-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ShareEntityGroupDialogComponent}]
})
export class ShareEntityGroupDialogComponent extends
  DialogComponent<ShareEntityGroupDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  shareEntityGroupFormGroup: FormGroup;

  entityGroupId = this.data.entityGroupId;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ShareEntityGroupDialogData,
              public dialogRef: MatDialogRef<ShareEntityGroupDialogComponent, boolean>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityGroupService: EntityGroupService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    const shareGroupRequest: ShareGroupRequest = {
      ownerId: null,
      isAllUserGroup: true,
      readElseWrite: true
    };

    this.shareEntityGroupFormGroup = this.fb.group({
      shareGroupRequest: [shareGroupRequest, Validators.required]
    });
  }

  ngOnInit(): void {

  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  share(): void {
    this.submitted = true;
    if (this.shareEntityGroupFormGroup.valid) {
      const shareGroupRequest = this.shareEntityGroupFormGroup.get('shareGroupRequest').value;
      this.entityGroupService.shareEntityGroup(this.entityGroupId.id, shareGroupRequest).subscribe(
        () => {
          this.dialogRef.close(true);
        }
      );
    }
  }
}
