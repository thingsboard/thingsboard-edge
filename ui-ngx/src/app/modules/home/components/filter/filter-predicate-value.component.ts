///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  DynamicValueSourceType,
  dynamicValueSourceTypeTranslationMap,
  EntityKeyValueType,
  FilterPredicateValue
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-filter-predicate-value',
  templateUrl: './filter-predicate-value.component.html',
  styleUrls: ['./filter-predicate.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterPredicateValueComponent),
      multi: true
    }
  ]
})
export class FilterPredicateValueComponent implements ControlValueAccessor, OnInit {

  private readonly inheritModeForSources: DynamicValueSourceType[] = [
    DynamicValueSourceType.CURRENT_CUSTOMER,
    DynamicValueSourceType.CURRENT_DEVICE];

  @Input() disabled: boolean;

  @Input()
  set allowUserDynamicSource(allow: boolean) {
    this.dynamicValueSourceTypes = [DynamicValueSourceType.CURRENT_TENANT,
      DynamicValueSourceType.CURRENT_CUSTOMER];
    this.allow = allow;
    if (allow) {
      this.dynamicValueSourceTypes.push(DynamicValueSourceType.CURRENT_USER);
    } else {
      this.dynamicValueSourceTypes.push(DynamicValueSourceType.CURRENT_DEVICE);
    }
  }

  @Input() onlyUserDynamicSource = false;

  @Input()
  valueType: EntityKeyValueType;

  valueTypeEnum = EntityKeyValueType;

  dynamicValueSourceTypes: DynamicValueSourceType[] = [DynamicValueSourceType.CURRENT_TENANT,
    DynamicValueSourceType.CURRENT_CUSTOMER, DynamicValueSourceType.CURRENT_USER];

  dynamicValueSourceTypeTranslations = dynamicValueSourceTypeTranslationMap;

  filterPredicateValueFormGroup: FormGroup;

  dynamicMode = false;

  inheritMode = false;

  allow = true;

  private propagateChange = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    let defaultValue: string | number | boolean;
    let defaultValueValidators: ValidatorFn[];
    switch (this.valueType) {
      case EntityKeyValueType.STRING:
        defaultValue = '';
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.NUMERIC:
        defaultValue = 0;
        defaultValueValidators = [Validators.required];
        break;
      case EntityKeyValueType.BOOLEAN:
        defaultValue = false;
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.DATE_TIME:
        defaultValue = Date.now();
        defaultValueValidators = [Validators.required];
        break;
    }
    this.filterPredicateValueFormGroup = this.fb.group({
      defaultValue: [defaultValue, defaultValueValidators],
      dynamicValue: this.fb.group(
        {
          sourceType: [null],
          sourceAttribute: [null],
          inherit: [false]
        }
      )
    });
    this.filterPredicateValueFormGroup.get('dynamicValue').get('sourceType').valueChanges.subscribe(
      (sourceType) => {
        if (!sourceType) {
          this.filterPredicateValueFormGroup.get('dynamicValue').get('sourceAttribute').patchValue(null, {emitEvent: false});
        }
        this.updateShowInheritMode(sourceType);
      }
    );
    this.filterPredicateValueFormGroup.valueChanges.subscribe(() => {
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
      this.filterPredicateValueFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicateValue: FilterPredicateValue<string | number | boolean>): void {
    this.filterPredicateValueFormGroup.get('defaultValue').patchValue(predicateValue.defaultValue, {emitEvent: false});
    this.filterPredicateValueFormGroup.get('dynamicValue.sourceType').patchValue(predicateValue.dynamicValue ?
      predicateValue.dynamicValue.sourceType : null, {emitEvent: false});
    this.filterPredicateValueFormGroup.get('dynamicValue.sourceAttribute').patchValue(predicateValue.dynamicValue ?
      predicateValue.dynamicValue.sourceAttribute : null, {emitEvent: false});
    this.filterPredicateValueFormGroup.get('dynamicValue.inherit').patchValue(predicateValue.dynamicValue ?
      predicateValue.dynamicValue.inherit : false, {emitEvent: false});
    this.updateShowInheritMode(predicateValue?.dynamicValue?.sourceType);
  }

  private updateModel() {
    let predicateValue: FilterPredicateValue<string | number | boolean> = null;
    if (this.filterPredicateValueFormGroup.valid) {
      predicateValue = this.filterPredicateValueFormGroup.getRawValue();
      if (predicateValue.dynamicValue) {
        if (!predicateValue.dynamicValue.sourceType || !predicateValue.dynamicValue.sourceAttribute) {
          predicateValue.dynamicValue = null;
        }
      }
    }
    this.propagateChange(predicateValue);
  }

  private updateShowInheritMode(sourceType: DynamicValueSourceType) {
    if (this.inheritModeForSources.includes(sourceType)) {
      this.inheritMode = true;
    } else {
      this.filterPredicateValueFormGroup.get('dynamicValue.inherit').patchValue(false, {emitEvent: false});
      this.inheritMode = false;
    }
  }
}
