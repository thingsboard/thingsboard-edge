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
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import {
  EntityKeyType,
  entityKeyTypeTranslationMap,
  KeyFilterInfo,
  keyFilterInfosToKeyFilters
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { KeyFilterDialogComponent, KeyFilterDialogData } from '@home/components/filter/key-filter-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';

@Component({
  selector: 'tb-key-filter-list',
  templateUrl: './key-filter-list.component.html',
  styleUrls: ['./key-filter-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KeyFilterListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => KeyFilterListComponent),
      multi: true
    }
  ]
})
export class KeyFilterListComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() telemetryKeysOnly = false;

  @Input() entityId: EntityId;

  keyFilterListFormGroup: FormGroup;

  entityKeyTypeTranslations = entityKeyTypeTranslationMap;

  keyFiltersControl: FormControl;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(private fb: FormBuilder,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.keyFilterListFormGroup = this.fb.group({});
    this.keyFilterListFormGroup.addControl('keyFilters',
      this.fb.array([]));
    this.keyFiltersControl = this.fb.control(null);
  }

  keyFiltersFormArray(): FormArray {
    return this.keyFilterListFormGroup.get('keyFilters') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.keyFilterListFormGroup.disable({emitEvent: false});
      this.keyFiltersControl.disable({emitEvent: false});
    } else {
      this.keyFilterListFormGroup.enable({emitEvent: false});
      this.keyFiltersControl.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.keyFilterListFormGroup.valid && this.keyFiltersControl.valid ? null : {
      keyFilterList: {valid: false}
    };
  }

  writeValue(keyFilters: Array<KeyFilterInfo>): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const keyFilterControls: Array<AbstractControl> = [];
    if (keyFilters) {
      for (const keyFilter of keyFilters) {
        keyFilterControls.push(this.fb.control(keyFilter, [Validators.required]));
      }
    }
    this.keyFilterListFormGroup.setControl('keyFilters', this.fb.array(keyFilterControls));
    this.valueChangeSubscription = this.keyFilterListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    if (this.disabled) {
      this.keyFilterListFormGroup.disable({emitEvent: false});
    } else {
      this.keyFilterListFormGroup.enable({emitEvent: false});
    }
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }

  public removeKeyFilter(index: number) {
    (this.keyFilterListFormGroup.get('keyFilters') as FormArray).removeAt(index);
  }

  public addKeyFilter() {
    const keyFiltersFormArray = this.keyFilterListFormGroup.get('keyFilters') as FormArray;
    this.openKeyFilterDialog(null).subscribe((result) => {
      if (result) {
        keyFiltersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editKeyFilter(index: number) {
    const keyFilter: KeyFilterInfo =
      (this.keyFilterListFormGroup.get('keyFilters') as FormArray).at(index).value;
    this.openKeyFilterDialog(keyFilter).subscribe(
      (result) => {
        if (result) {
          (this.keyFilterListFormGroup.get('keyFilters') as FormArray).at(index).patchValue(result);
        }
      }
    );
  }

  private openKeyFilterDialog(keyFilter?: KeyFilterInfo): Observable<KeyFilterInfo> {
    const isAdd = !keyFilter;
    if (!keyFilter) {
      keyFilter = {
        key: {
          key: '',
          type: EntityKeyType.ATTRIBUTE
        },
        valueType: null,
        value: null,
        predicates: []
      };
    }
    return this.dialog.open<KeyFilterDialogComponent, KeyFilterDialogData,
      KeyFilterInfo>(KeyFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilter: keyFilter ? (this.disabled ? keyFilter : deepClone(keyFilter)) : null,
        isAdd,
        readonly: this.disabled,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        telemetryKeysOnly: this.telemetryKeysOnly,
        entityId: this.entityId
      }
    }).afterClosed();
  }

  private updateModel() {
    const keyFilters: Array<KeyFilterInfo> = this.keyFilterListFormGroup.getRawValue().keyFilters;
    if (keyFilters.length) {
      this.propagateChange(keyFilters);
    } else {
      this.propagateChange(null);
    }
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }
}
