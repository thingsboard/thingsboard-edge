///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

export interface SelectOwnerDialogData {
  selectOwnerTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeOwnerIds: Array<string>;
  onOwnerSelected: (targetOwnerId: EntityId) => Observable<boolean>;
}

@Component({
  selector: 'tb-select-owner-dialog',
  templateUrl: './select-owner-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: SelectOwnerDialogComponent}],
  styleUrls: []
})
export class SelectOwnerDialogComponent extends
  DialogComponent<SelectOwnerDialogComponent, EntityId> implements OnInit, ErrorStateMatcher {

  selectOwnerFormGroup: FormGroup;

  submitted = false;

  selectOwnerTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeOwnerIds: Array<string>;
  onOwnerSelected: (targetOwnerId: EntityId) => Observable<boolean>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SelectOwnerDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SelectOwnerDialogComponent, EntityId>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.selectOwnerTitle = data.selectOwnerTitle;
    this.confirmSelectTitle = data.confirmSelectTitle;
    this.placeholderText = data.placeholderText;
    this.notFoundText = data.notFoundText;
    this.requiredText = data.requiredText;
    this.excludeOwnerIds = data.excludeOwnerIds;
    this.onOwnerSelected = data.onOwnerSelected;
  }

  ngOnInit(): void {
    this.selectOwnerFormGroup = this.fb.group({
      targetOwnerId: [null, [Validators.required]]
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  selectOwner(): void {
    this.submitted = true;
    const targetOwnerId: EntityId = this.selectOwnerFormGroup.get('targetOwnerId').value;
    if (this.onOwnerSelected) {
      this.onOwnerSelected(targetOwnerId).subscribe((res) => {
        if (res) {
          this.dialogRef.close(targetOwnerId);
        }
      },
        () => {}
      );
    } else {
      this.dialogRef.close(targetOwnerId);
    }
  }
}
