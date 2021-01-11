///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

import { Component, forwardRef, Injector, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  NgControl,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subscription } from 'rxjs';
import { GenericRolePermissions } from '@shared/models/role.models';
import { Operation, Resource } from '@shared/models/security.models';

@Component({
  selector: 'tb-permission-list',
  templateUrl: './permission-list.component.html',
  styleUrls: ['./permission-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PermissionListComponent),
      multi: true
    }
  ]
})
export class PermissionListComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  permissionListFormGroup: FormGroup;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  ngControl: NgControl;

  constructor(protected store: Store<AppState>,
              private injector: Injector,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.ngControl = this.injector.get(NgControl);
    this.permissionListFormGroup = this.fb.group({});
    this.permissionListFormGroup.addControl('permissions',
      this.fb.array([]));
  }

  permissionsFormArray(): FormArray {
    return this.permissionListFormGroup.get('permissions') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.permissionListFormGroup.disable({emitEvent: false});
    } else {
      this.permissionListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(permissions: GenericRolePermissions): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const permissionsControls: Array<AbstractControl> = [];
    if (permissions) {
      for (const resource of Object.keys(permissions)) {
        const permissionControl = this.fb.group({
          resource: [Resource[resource], [Validators.required]],
          operations: [permissions[resource], [Validators.required]]
        });
        if (this.disabled) {
          permissionControl.disable();
        }
        permissionsControls.push(permissionControl);
      }
    }
    this.permissionListFormGroup.setControl('permissions', this.fb.array(permissionsControls));
    this.valueChangeSubscription = this.permissionListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public removePermission(index: number) {
    (this.permissionListFormGroup.get('permissions') as FormArray).removeAt(index);
  }

  public addPermission() {
    const permissionsFormArray = this.permissionListFormGroup.get('permissions') as FormArray;
    permissionsFormArray.push(this.fb.group({
      resource: [null, [Validators.required]],
      operations: [null, [Validators.required]]
    }));
  }

  private updateModel() {
    const permissionList: {resource: Resource; operations: Operation[]}[] = this.permissionListFormGroup.get('permissions').value;
    const permissions: GenericRolePermissions = {};
    permissionList.forEach((entry) => {
      permissions[entry.resource] = entry.operations;
    });
    this.propagateChange(permissions);
  }
}
