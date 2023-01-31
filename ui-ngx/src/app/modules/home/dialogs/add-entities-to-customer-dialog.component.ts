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
import { DeviceService } from '@core/http/device.service';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable, of } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DashboardService } from '@core/http/dashboard.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EdgeService } from '@core/http/edge.service';

export interface AddEntitiesToCustomerDialogData {
  customerId: string;
  entityType: EntityType;
}

@Component({
  selector: 'tb-add-entities-to-customer-dialog',
  templateUrl: './add-entities-to-customer-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEntitiesToCustomerDialogComponent}],
  styleUrls: []
})
export class AddEntitiesToCustomerDialogComponent extends
  DialogComponent<AddEntitiesToCustomerDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  addEntitiesToCustomerFormGroup: FormGroup;

  submitted = false;

  entityType: EntityType;

  assignToCustomerTitle: string;
  assignToCustomerText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntitiesToCustomerDialogData,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private edgeService: EdgeService,
              private entityViewService: EntityViewService,
              private dashboardService: DashboardService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntitiesToCustomerDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.entityType = data.entityType;
  }

  ngOnInit(): void {
    this.addEntitiesToCustomerFormGroup = this.fb.group({
      entityIds: [null, [Validators.required]]
    });
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        this.assignToCustomerTitle = 'device.assign-device-to-customer';
        this.assignToCustomerText = 'device.assign-device-to-customer-text';
        break;
      case EntityType.ASSET:
        this.assignToCustomerTitle = 'asset.assign-asset-to-customer';
        this.assignToCustomerText = 'asset.assign-asset-to-customer-text';
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToCustomerTitle = 'entity-view.assign-entity-view-to-customer';
        this.assignToCustomerText = 'entity-view.assign-entity-view-to-customer-text';
        break;
      case EntityType.DASHBOARD:
        this.assignToCustomerTitle = 'dashboard.assign-dashboard-to-customer';
        this.assignToCustomerText = 'dashboard.assign-dashboard-to-customer-text';
        break;
      case EntityType.EDGE:
        this.assignToCustomerTitle = 'edge.assign-edge-to-customer';
        this.assignToCustomerText = 'edge.assign-edge-to-customer-text';
        break;
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

  assign(): void {
    this.submitted = true;
    const entityIds: Array<string> = this.addEntitiesToCustomerFormGroup.get('entityIds').value;
    const tasks: Observable<any>[] = [];
    entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToCustomerTask(this.data.customerId, entityId));
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
      case EntityType.EDGE:
        return this.edgeService.assignEdgeToCustomer(customerId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToCustomer(customerId, entityId);
      case EntityType.DASHBOARD:
        return this.dashboardService.assignDashboardToCustomer(customerId, entityId);
        break;
    }*/
    return of(null);
  }

}
