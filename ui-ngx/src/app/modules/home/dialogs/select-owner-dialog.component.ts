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
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
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

  selectOwnerFormGroup: UntypedFormGroup;

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
              public fb: UntypedFormBuilder) {
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

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
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
