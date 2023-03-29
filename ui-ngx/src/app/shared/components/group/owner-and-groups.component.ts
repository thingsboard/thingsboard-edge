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
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TranslateService } from '@ngx-translate/core';
import { EntityInfoData } from '@shared/models/entity.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { EntityId } from '@app/shared/models/id/entity-id';

export interface OwnerAndGroupsData {
  owner?: EntityId | EntityInfoData;
  groups?: EntityInfoData[];
}

@Component({
  selector: 'tb-owner-and-groups',
  templateUrl: './owner-and-groups.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OwnerAndGroupsComponent),
      multi: true
    }
  ]
})
export class OwnerAndGroupsComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  entityType: EntityType;

  @Input()
  defaultOwnerId: EntityId | null;

  @Input()
  skipDefaultPermissionCheck = false;

  ownerAndGroupsFormGroup: UntypedFormGroup;

  modelValue: OwnerAndGroupsData | null;

  currentUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  private ownerDisabled = false;
  private groupsDisabled = false;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit(): void {
    if (!this.skipDefaultPermissionCheck) {
      this.ownerDisabled = !this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, this.entityType);
      this.groupsDisabled = !this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.ADD_TO_GROUP, this.entityType) ||
                            !this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.REMOVE_FROM_GROUP, this.entityType);
    }

    this.ownerAndGroupsFormGroup = this.fb.group({
      owner: this.fb.control({value: null,
        disabled: this.ownerDisabled},
        [Validators.required]),
      groups: this.fb.control({value: null,
        disabled: this.groupsDisabled
      }, [])
    });
    this.ownerAndGroupsFormGroup.valueChanges.subscribe((value: OwnerAndGroupsData) => {
      if (!value.owner) {
        this.ownerAndGroupsFormGroup.get('groups').disable({emitEvent: false});
      } else if (!this.groupsDisabled) {
        this.ownerAndGroupsFormGroup.get('groups').enable({emitEvent: false});
      }
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ownerAndGroupsFormGroup.disable({emitEvent: false});
    } else {
      this.ownerAndGroupsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: OwnerAndGroupsData | undefined): void {
    this.modelValue = value;
    this.ownerAndGroupsFormGroup.patchValue(value || {}, {emitEvent: false});
  }

  ownerId(): EntityId | null {
    const ownerValue: EntityId | EntityInfoData = this.ownerAndGroupsFormGroup.get('owner').value;
    if (ownerValue) {
      return (ownerValue as EntityInfoData).name ? (ownerValue as EntityInfoData).id : ownerValue as EntityId;
    } else {
      return null;
    }
  }

  private updateModel() {
    this.modelValue = this.ownerAndGroupsFormGroup.value || {};
    if (this.ownerAndGroupsFormGroup.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
