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
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { Asset } from '@shared/models/asset.models';
import { EntityType } from '@shared/models/entity-type.models';
import { OwnerAndGroupsData } from '@home/components/group/owner-and-groups.component';
import { EntityInfoData } from '@shared/models/entity.models';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityGroup } from '@shared/models/entity-group.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Observable, throwError } from 'rxjs';
import { Device } from '@shared/models/device.models';
import { deepTrim } from '@core/utils';
import { catchError } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { AssetService } from '@core/http/asset.service';

export interface AddAssetDialogData {
  customerId?: string;
  entityGroup?: EntityGroup;
}

@Component({
  selector: 'tb-add-asset-dialog',
  templateUrl: './add-asset-dialog.component.html',
  styleUrls: ['./add-asset-dialog.component.scss']
})
export class AddAssetDialogComponent extends DialogComponent<AddAssetDialogComponent, Asset>
  implements OnInit {

  assetFormGroup: UntypedFormGroup;

  initialOwnerId: EntityId;
  entityType = EntityType;

  private entityGroup = this.data.entityGroup;
  private customerId = this.data.customerId;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddAssetDialogData,
              private assetService: AssetService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private userPermissionsService: UserPermissionsService,
              public dialogRef: MatDialogRef<AddAssetDialogComponent, Asset>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    let initialGroups: EntityInfoData[] = [];
    if (this.entityGroup) {
      this.initialOwnerId = this.entityGroup.ownerId;
      if (!this.entityGroup.groupAll) {
        initialGroups = [{id: this.entityGroup.id, name: this.entityGroup.name}];
      }
    } else {
      if (this.customerId) {
        this.initialOwnerId = new CustomerId(this.customerId);
      } else {
        this.initialOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    }

    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.initialOwnerId,
      groups: initialGroups
    };

    this.assetFormGroup = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      assetProfileId: [null, [Validators.required]],
      label: ['', Validators.maxLength(255)],
      ownerAndGroups: [ownerAndGroups, [Validators.required]],
      additionalInfo: this.fb.group(
        {
          description: [''],
        }
      )
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.assetFormGroup.valid) {
      this.createAsset().subscribe(asset => this.dialogRef.close(asset));
    }
  }

  private createAsset(): Observable<Asset> {
    const asset: Asset = {
      name: this.assetFormGroup.get('name').value,
      label: this.assetFormGroup.get('label').value,
      assetProfileId: this.assetFormGroup.get('assetProfileId').value,
      additionalInfo: {
        description: this.assetFormGroup.get('additionalInfo.description').value
      },
      customerId: null
    } as Asset;
    const targetOwnerAndGroups: OwnerAndGroupsData = this.assetFormGroup.get('ownerAndGroups').value;
    const targetOwner = targetOwnerAndGroups.owner;
    let targetOwnerId: EntityId;
    if ((targetOwner as EntityInfoData).name) {
      targetOwnerId = (targetOwner as EntityInfoData).id;
    } else {
      targetOwnerId = targetOwner as EntityId;
    }
    if (targetOwnerId.entityType === EntityType.CUSTOMER) {
      asset.customerId = targetOwnerId as CustomerId;
    }
    const entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);

    return this.assetService.saveAsset(deepTrim(asset), entityGroupIds).pipe(
      catchError(e => {
        return throwError(e);
      })
    );
  }

}
