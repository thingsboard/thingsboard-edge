///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { booleanAttribute, Component, Input, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { GroupPermission } from '@shared/models/group-permission.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { isDefinedAndNotNull } from '@core/utils';
import { RoleId } from '@shared/models/id/role-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-user-groups-panel-row',
  templateUrl: './user-groups-panel-row.component.html',
  styleUrls: ['./user-groups-panel-row.component.scss'],
  encapsulation: ViewEncapsulation.None,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: UserGroupsPanelRowComponent,
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: UserGroupsPanelRowComponent,
      multi: true
    }
  ]
})
export class UserGroupsPanelRowComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  groupPermissionForm = this.fb.group({
    roleId: this.fb.control<RoleId>(null, Validators.required),
    entityGroupId: this.fb.control<EntityGroupId>(null, Validators.required)
  });
  groupType = this.fb.control(RoleType.GENERIC);

  readonly entityType = EntityType;
  readonly roleType = RoleType;
  readonly roleTypes = Object.keys(RoleType) as RoleType[];
  readonly roleTypeTranslations = roleTypeTranslationMap;

  entityGroupOwnerId = this.userPermissionsService.getUserOwnerId()

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private userPermissionsService: UserPermissionsService) {
    this.groupPermissionForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      this.propagateChange(value);
    });

    this.groupType.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value === RoleType.GENERIC) {
        this.groupPermissionForm.get('entityGroupId').disable({emitEvent: false});
      } else {
        this.groupPermissionForm.get('entityGroupId').enable({emitEvent: false});
      }
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.groupPermissionForm.disable({emitEvent: false});
      this.groupType.disable({emitEvent: false});
    } else {
      this.groupPermissionForm.enable({emitEvent: false});
      this.groupType.enable();
      if(isDefinedAndNotNull(this.disabled)) {
        setTimeout(() => {
          this.groupPermissionForm.updateValueAndValidity();
        }, 0);
      }
    }
    this.disabled = isDisabled;
  }

  validate(): ValidationErrors | null {
    if (!this.groupPermissionForm.valid) {
      return {
        invalidGroupPermissionForm: true
      };
    }
    return null;
  }

  writeValue(permission: GroupPermission) {
    if (isDefinedAndNotNull(permission.entityGroupId)) {
      this.groupType.setValue(RoleType.GROUP);
    } else {
      this.groupType.setValue(RoleType.GENERIC);
    }
    this.groupPermissionForm.patchValue(permission, {emitEvent: false})
  }
}
