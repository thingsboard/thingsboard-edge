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
      isAllUserGroup: [null],
      userGroupId: [null],
      permissionType: [null],
      roleIds: [null]
    });
    this.shareEntityGroupFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.shareEntityGroupFormGroup.get('isAllUserGroup').valueChanges.subscribe(() => {
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
    const isAllUserGroup: boolean = this.shareEntityGroupFormGroup.get('isAllUserGroup').value;
    if (isAllUserGroup) {
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
