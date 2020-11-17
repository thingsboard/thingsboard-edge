///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import { EdgeService } from "@core/http/edge.service";
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DashboardService } from '@core/http/dashboard.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { RuleChainService } from "@core/http/rule-chain.service";

export interface AddEntitiesToEdgeDialogData {
  edgeId: string;
  entityType: EntityType;
}

@Component({
  selector: 'tb-add-entities-to-edge-dialog',
  templateUrl: './add-entities-to-edge-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEntitiesToEdgeDialogComponent}],
  styleUrls: []
})
export class AddEntitiesToEdgeDialogComponent extends
  DialogComponent<AddEntitiesToEdgeDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  addEntitiesToEdgeFormGroup: FormGroup;

  submitted = false;

  entityType: EntityType;

  assignToEdgeTitle: string;
  assignToEdgeText: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntitiesToEdgeDialogData,
              private deviceService: DeviceService,
              private edgeService: EdgeService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              private dashboardService: DashboardService,
              private ruleChainService: RuleChainService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddEntitiesToEdgeDialogComponent, boolean>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.entityType = data.entityType;
  }

  ngOnInit(): void {
    this.addEntitiesToEdgeFormGroup = this.fb.group({
      entityIds: [null, [Validators.required]]
    });
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        this.assignToEdgeTitle = 'device.assign-device-to-edge-title';
        this.assignToEdgeText = 'device.assign-device-to-edge-text';
        break;
      case EntityType.RULE_CHAIN:
        this.assignToEdgeTitle = 'rulechain.assign-rulechain-to-edge-title';
        this.assignToEdgeText = 'rulechain.assign-rulechain-to-edge-text';
        break;
      case EntityType.ASSET:
        this.assignToEdgeTitle = 'asset.assign-asset-to-edge-title';
        this.assignToEdgeText = 'asset.assign-asset-to-edge-text';
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToEdgeTitle = 'entity-view.assign-entity-view-to-edge-title';
        this.assignToEdgeText = 'entity-view.assign-entity-view-to-edge-text';
        break;
      case EntityType.DASHBOARD:
        this.assignToEdgeTitle = 'dashboard.assign-dashboard-to-edge-title';
        this.assignToEdgeText = 'dashboard.assign-dashboard-to-edge-text';
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
    // TODO: deaflynx
    /*
    this.submitted = true;
    const entityIds: Array<string> = this.addEntitiesToEdgeFormGroup.get('entityIds').value;
    const tasks: Observable<any>[] = [];
    entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToEdgeTask(this.data.edgeId, entityId));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );

     */
  }

  /*
  private getAssignToEdgeTask(edgeId: string, entityId: string): Observable<any> {
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        return this.deviceService.assignDeviceToEdge(edgeId, entityId);
      case EntityType.ASSET:
        return this.assetService.assignAssetToEdge(edgeId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToEdge(edgeId, entityId);
      case EntityType.DASHBOARD:
        return this.dashboardService.assignDashboardToEdge(edgeId, entityId);
      case EntityType.RULE_CHAIN:
        return this.ruleChainService.assignRuleChainToEdge(edgeId, entityId);
    }
  }

   */

}
