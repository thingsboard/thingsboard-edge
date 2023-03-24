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
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  BooleanFilterPredicate,
  BooleanOperation,
  booleanOperationTranslationMap,
  EntityKeyValueType,
  FilterPredicateType
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-boolean-filter-predicate',
  templateUrl: './boolean-filter-predicate.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BooleanFilterPredicateComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => BooleanFilterPredicateComponent),
      multi: true
    }
  ]
})
export class BooleanFilterPredicateComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  valueTypeEnum = EntityKeyValueType;

  booleanFilterPredicateFormGroup: UntypedFormGroup;

  booleanOperations = Object.keys(BooleanOperation);
  booleanOperationEnum = BooleanOperation;
  booleanOperationTranslations = booleanOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.booleanFilterPredicateFormGroup = this.fb.group({
      operation: [BooleanOperation.EQUAL, [Validators.required]],
      value: [null, [Validators.required]]
    });
    this.booleanFilterPredicateFormGroup.valueChanges.subscribe(() => {
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
    if (this.disabled) {
      this.booleanFilterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.booleanFilterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.booleanFilterPredicateFormGroup ? null : {
      booleanFilterPredicate: {valid: false}
    };
  }

  writeValue(predicate: BooleanFilterPredicate): void {
    this.booleanFilterPredicateFormGroup.get('operation').patchValue(predicate.operation, {emitEvent: false});
    this.booleanFilterPredicateFormGroup.get('value').patchValue(predicate.value, {emitEvent: false});
  }

  private updateModel() {
    let predicate: BooleanFilterPredicate = null;
    if (this.booleanFilterPredicateFormGroup.valid) {
      predicate = this.booleanFilterPredicateFormGroup.getRawValue();
      predicate.type = FilterPredicateType.BOOLEAN;
    }
    this.propagateChange(predicate);
  }

}
