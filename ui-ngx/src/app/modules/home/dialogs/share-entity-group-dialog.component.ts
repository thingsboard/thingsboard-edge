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
      allUserGroup: true,
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
