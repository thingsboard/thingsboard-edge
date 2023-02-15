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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { ShareGroupRequest } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { RoleId } from '@shared/models/id/role-id';
import { RoleType } from '@shared/models/security.models';

@Component({
  selector: 'tb-share-entity-group',
  templateUrl: './share-entity-group.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ShareEntityGroupComponent),
    multi: true
  }]
})
export class ShareEntityGroupComponent implements ControlValueAccessor, OnInit {

  entityType = EntityType;

  roleType = RoleType;

  shareEntityGroupFormGroup: FormGroup;

  @Input()
  disabled: boolean;

  private shareGroupRequest: ShareGroupRequest = null;
  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.shareGroupRequest);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.shareEntityGroupFormGroup = this.fb.group({
      ownerId: [null, Validators.required],
      allUserGroup: [null],
      userGroupId: [null],
      permissionType: [null],
      roleIds: [null]
    });
    this.shareEntityGroupFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.shareEntityGroupFormGroup.get('allUserGroup').valueChanges.subscribe(() => {
      this.updateValidators();
    });
    this.shareEntityGroupFormGroup.get('permissionType').valueChanges.subscribe(() => {
      this.updateValidators();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.shareEntityGroupFormGroup.disable({emitEvent: false});
    } else {
      this.shareEntityGroupFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ShareGroupRequest | null): void {
    if (value) {
      const roleIds = value.roleIds ? value.roleIds.map(id => id.id) : [];
      (value as any).roleIds = roleIds;
      this.shareEntityGroupFormGroup.patchValue(value, {emitEvent: false});
      const permissionType = value.roleIds && value.roleIds.length ? 2 : (value.readElseWrite ? 0 : 1);
      this.shareEntityGroupFormGroup.get('permissionType').patchValue(permissionType, {emitEvent: false});
    } else {
      this.shareEntityGroupFormGroup.patchValue({}, {emitEvent: false});
    }
    this.updateValidators();
    this.updateModel();
  }

  private updateValidators() {
    const allUserGroup: boolean = this.shareEntityGroupFormGroup.get('allUserGroup').value;
    if (allUserGroup) {
      this.shareEntityGroupFormGroup.get('userGroupId').clearValidators();
    } else {
      this.shareEntityGroupFormGroup.get('userGroupId').setValidators(Validators.required);
    }
    const permissionType: number = this.shareEntityGroupFormGroup.get('permissionType').value;
    if (permissionType === 2) {
      this.shareEntityGroupFormGroup.get('roleIds').setValidators(Validators.required);
    } else {
      this.shareEntityGroupFormGroup.get('roleIds').clearValidators();
    }
    this.shareEntityGroupFormGroup.get('userGroupId').updateValueAndValidity({emitEvent: false});
    this.shareEntityGroupFormGroup.get('roleIds').updateValueAndValidity({emitEvent: false});
  }

  private updateModel() {
    this.shareGroupRequest = null;
    if (this.shareEntityGroupFormGroup.valid) {
      this.shareGroupRequest = this.shareEntityGroupFormGroup.getRawValue();
      const permissionType = this.shareEntityGroupFormGroup.get('permissionType').value;
      this.shareGroupRequest.readElseWrite = permissionType === 0;
      delete (this.shareGroupRequest as any).permissionType;
      const roleIds: string[] = this.shareEntityGroupFormGroup.get('roleIds').value;
      if (roleIds && roleIds.length) {
        this.shareGroupRequest.roleIds = roleIds.map(id => new RoleId(id));
      }
    }
    if (this.propagateChange) {
      this.propagateChange(this.shareGroupRequest);
    } else {
      this.propagateChangePending = true;
    }
  }
}
