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
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  EntityKeyValueType,
  FilterPredicateType,
  StringFilterPredicate,
  StringOperation,
  stringOperationTranslationMap
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-string-filter-predicate',
  templateUrl: './string-filter-predicate.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringFilterPredicateComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => StringFilterPredicateComponent),
      multi: true
    }
  ]
})
export class StringFilterPredicateComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  valueTypeEnum = EntityKeyValueType;

  stringFilterPredicateFormGroup: FormGroup;

  stringOperations = Object.keys(StringOperation);
  stringOperationEnum = StringOperation;
  stringOperationTranslations = stringOperationTranslationMap;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.stringFilterPredicateFormGroup = this.fb.group({
      operation: [StringOperation.STARTS_WITH, [Validators.required]],
      value: [null, [Validators.required]],
      ignoreCase: [false]
    });
    this.stringFilterPredicateFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.stringFilterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.stringFilterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicate: StringFilterPredicate): void {
    this.stringFilterPredicateFormGroup.get('operation').patchValue(predicate.operation, {emitEvent: false});
    this.stringFilterPredicateFormGroup.get('value').patchValue(predicate.value, {emitEvent: false});
    this.stringFilterPredicateFormGroup.get('ignoreCase').patchValue(predicate.ignoreCase, {emitEvent: false});
  }

  validate(c): ValidationErrors {
    return this.stringFilterPredicateFormGroup.valid ? null : {
      stringFilterPredicate: {valid: false}
    };
  }

  private updateModel() {
    const predicate: StringFilterPredicate = this.stringFilterPredicateFormGroup.getRawValue();
    predicate.type = FilterPredicateType.STRING;
    this.propagateChange(predicate);
  }

}
