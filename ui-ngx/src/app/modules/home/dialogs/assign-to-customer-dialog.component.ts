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
import { DeviceService } from '@core/http/device.service';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable, of } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EdgeService } from '@core/http/edge.service';

export interface AssignToCustomerDialogData {
  entityIds: Array<EntityId>;
  entityType: EntityType;
}

@Component({
  selector: 'tb-assign-to-customer-dialog',
  templateUrl: './assign-to-customer-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AssignToCustomerDialogComponent}],
  styleUrls: []
})
export class AssignToCustomerDialogComponent extends
  DialogComponent<AssignToCustomerDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  assignToCustomerFormGroup: UntypedFormGroup;

  submitted = false;

  entityType = EntityType;

  assignToCustomerTitle: string;
  assignToCustomerText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AssignToCustomerDialogData,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private edgeService: EdgeService,
              private entityViewService: EntityViewService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AssignToCustomerDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.assignToCustomerFormGroup = this.fb.group({
      customerId: [null, [Validators.required]]
    });
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        this.assignToCustomerTitle = 'device.assign-device-to-customer';
        this.assignToCustomerText = 'device.assign-to-customer-text';
        break;
      case EntityType.ASSET:
        this.assignToCustomerTitle = 'asset.assign-asset-to-customer';
        this.assignToCustomerText = 'asset.assign-to-customer-text';
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToCustomerTitle = 'entity-view.assign-entity-view-to-customer';
        this.assignToCustomerText = 'entity-view.assign-to-customer-text';
        break;
      case EntityType.EDGE:
        this.assignToCustomerTitle = 'edge.assign-edge-to-customer';
        this.assignToCustomerText = 'edge.assign-to-customer-text';
        break;
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  assign(): void {
    this.submitted = true;
    const customerId: string = this.assignToCustomerFormGroup.get('customerId').value;
    const tasks: Observable<any>[] = [];
    this.data.entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToCustomerTask(customerId, entityId.id));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  private getAssignToCustomerTask(customerId: string, entityId: string): Observable<any> {
    /*switch (this.data.entityType) {
      case EntityType.DEVICE:
        return this.deviceService.assignDeviceToCustomer(customerId, entityId);
      case EntityType.ASSET:
        return this.assetService.assignAssetToCustomer(customerId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToCustomer(customerId, entityId);
      case EntityType.EDGE:
        return this.edgeService.assignEdgeToCustomer(customerId, entityId);
        break;
    }*/
    return of(null);
  }

}
