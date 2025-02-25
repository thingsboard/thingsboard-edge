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

import { booleanAttribute, Component, Input } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { GroupPermission } from '@shared/models/group-permission.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-user-group-panel',
  templateUrl: './user-group-panel.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: UserGroupPanelComponent,
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: UserGroupPanelComponent,
      multi: true
    }
  ]
})
export class UserGroupPanelComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  groupPermissionsForm = this.fb.array<GroupPermission>([]);

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.groupPermissionsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.groupPermissionsForm.disable({emitEvent: false});
    } else {
      this.groupPermissionsForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (this.groupPermissionsForm.status !== 'DISABLED' && !this.groupPermissionsForm.valid) {
      return {
        invalidGroupPermissionsForm: true
      };
    }
    return null;
  }

  writeValue(groupPermissions: GroupPermission[]) {
    if (this.groupPermissionsForm.length === groupPermissions?.length) {
      this.groupPermissionsForm.patchValue(groupPermissions, {emitEvent: false});
    } else {
      this.groupPermissionsForm.clear({emitEvent: false});
      groupPermissions.forEach(item => {
        this.groupPermissionsForm.push(this.fb.control(item), {emitEvent: false})
      })
    }
  }

  addPermission($event: Event) {
    $event?.stopPropagation();
    this.groupPermissionsForm.push(this.fb.control({
      roleId: null
    }), {emitEvent: false});
  }

  trackByGroup(_index: number, groupControl: AbstractControl): any {
    return groupControl;
  }

  removePermission(index: number) {
    this.groupPermissionsForm.removeAt(index);
  }
}
