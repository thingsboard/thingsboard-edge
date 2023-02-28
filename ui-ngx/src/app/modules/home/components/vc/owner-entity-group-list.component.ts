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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder, FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityId, entityIdEquals } from '@app/shared/models/id/entity-id';
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-owner-entity-group-list',
  templateUrl: './owner-entity-group-list.component.html',
  styleUrls: ['./owner-entity-group-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OwnerEntityGroupListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OwnerEntityGroupListComponent),
      multi: true
    }
  ]
})
export class OwnerEntityGroupListComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  entityType: EntityType;

  ownerEntityGroupListFormGroup: FormGroup;

  modelValue: Array<string> | null;

  currentUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.ownerEntityGroupListFormGroup = this.fb.group({
      ownerEntityGroups: this.fb.array([], [])
    });
    this.ownerEntityGroupListFormGroup.valueChanges.subscribe(() => {
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
      this.ownerEntityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.ownerEntityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | undefined): void {
    this.modelValue = value;
    this.ownerEntityGroupListFormGroup.setControl('ownerEntityGroups',
      this.prepareOwnerEntityGroupsFormArray(
        [{ownerId: {id: this.currentUser.tenantId, entityType: EntityType.TENANT}, groupIds: []}]), {emitEvent: false});
  }

  public validate(c: FormControl) {
    return this.ownerEntityGroupListFormGroup.valid && this.ownerEntityGroupsArray().length ? null : {
      ownerEntityGroups: {
        valid: false,
      },
    };
  }

  private prepareOwnerEntityGroupsFormArray(ownerGroupsArray: {ownerId: EntityId, groupIds: string[] }[] | undefined): FormArray {
    const ownerEntityGroupsControls: Array<AbstractControl> = [];
    if (ownerGroupsArray) {
      for (const ownerGroups of ownerGroupsArray) {
        ownerEntityGroupsControls.push(this.createOwnerEntityGroupsControl(ownerGroups));
      }
    }
    return this.fb.array(ownerEntityGroupsControls);
  }

  private createOwnerEntityGroupsControl(ownerGroups: {ownerId: EntityId, groupIds: string[] }): AbstractControl {
    const ownerEntityGroupsControl = this.fb.group(
      {
        ownerId: [ownerGroups.ownerId, [Validators.required]],
        groupIds: [ownerGroups.groupIds, [Validators.required]]
      }
    );
    return ownerEntityGroupsControl;
  }

  ownerEntityGroupsArray(): FormGroup[] {
    return (this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as FormArray).controls as FormGroup[];
  }

  entityGroupsTitle(): string {
    return this.entityType ? this.translate.instant(entityGroupsTitle(this.entityType)) : '';
  }

  public trackByOwnerEntityGroups(index: number, ownerEntityGroupsControl: AbstractControl): any {
    return ownerEntityGroupsControl;
  }

  public excludeOwnerIds(ownerEntityGroupsControl: AbstractControl): Array<string> {
    const currentOwnerId: EntityId = ownerEntityGroupsControl.get('ownerId').value;
    const value: {ownerId: EntityId, groupIds: string[] }[] =
      this.ownerEntityGroupListFormGroup.get('ownerEntityGroups').value || [];
    const excludeOwnerIds: string[] = [];
    if (value) {
      for (const ownerGroups of value) {
        if (ownerGroups.ownerId && !entityIdEquals(ownerGroups.ownerId, currentOwnerId)) {
          excludeOwnerIds.push(ownerGroups.ownerId.id);
        }
      }
    }
    return excludeOwnerIds;
  }

  public removeOwnerEntityGroups(index: number) {
    (this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as FormArray).removeAt(index);
  }

  public addOwnerEntityGroup() {
    const ownerEntityGroupsArray = this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as FormArray;
    const ownerGroups: {ownerId: EntityId, groupIds: string[] } = {
      ownerId: null,
      groupIds: []
    };
    const ownerEntityGroupsControl = this.createOwnerEntityGroupsControl(ownerGroups);
    ownerEntityGroupsArray.push(ownerEntityGroupsControl);
    this.ownerEntityGroupListFormGroup.updateValueAndValidity();
  }

  private updateModel() {
    const value: {ownerId: EntityId, groupIds: string[] }[] =
      this.ownerEntityGroupListFormGroup.get('ownerEntityGroups').value || [];
    let modelValue: string[] = null;
    if (value && value.length) {
      modelValue = [];
      for (const ownerGroups of value) {
        if (ownerGroups && ownerGroups.groupIds && ownerGroups.groupIds.length) {
          modelValue.push(...ownerGroups.groupIds);
        }
      }
    }
    this.modelValue = modelValue;
    this.propagateChange(this.modelValue);
  }

}
