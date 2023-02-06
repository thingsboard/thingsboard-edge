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
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';

@Component({
  selector: 'tb-add-entity-groups-to-edge-dialog',
  templateUrl: './add-entity-groups-to-edge-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEntityGroupsToEdgeDialogComponent}],
  styleUrls: []
})
export class AddEntityGroupsToEdgeDialogComponent extends
  DialogComponent<AddEntityGroupsToEdgeDialogComponent> implements OnInit, ErrorStateMatcher {

  addEntityGroupToEdgeFormGroup: FormGroup;

  submitted = false;

  entityType = EntityType;

  groupType: EntityType;
  edgeId: string;
  customerId: string;
  childGroupId: string;
  addEntityGroupsToEdgeTitle: string;
  confirmSelectTitle: string;
  notFoundText: string;
  requiredText: string;

  edgeEntityGroupIds: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected userPermissionsService: UserPermissionsService,
              protected entityGroupService: EntityGroupService,
              protected broadcast: BroadcastService,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityGroupsToEdgeDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntityGroupsToEdgeDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.groupType = data.groupType;
    this.edgeId = data.edgeId;
    this.addEntityGroupsToEdgeTitle = data.addEntityGroupsToEdgeTitle;
    this.confirmSelectTitle = data.confirmSelectTitle;
    this.notFoundText = data.notFoundText;
    this.requiredText = data.requiredText;
    this.edgeEntityGroupIds = [];
  }

  ngOnInit(): void {
    this.addEntityGroupToEdgeFormGroup = this.fb.group({
      edgeEntityGroupIds: [[...this.edgeEntityGroupIds]]
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

  addEntityGroupsToEdge(): void {
    this.submitted = true;
    const edgeEntityGroupIds: Array<string> = this.addEntityGroupToEdgeFormGroup.get('edgeEntityGroupIds').value;
    const tasks: Observable<any>[] = [];
    edgeEntityGroupIds.forEach(
      (entityGroupId) => {
        tasks.push(this.entityGroupService.assignEntityGroupToEdge(this.edgeId, entityGroupId, this.groupType));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

}
