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
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  DynamicValueSourceType,
  dynamicValueSourceTypeTranslationMap,
  FilterPredicateValue,
  getDynamicSourcesForAllowUser,
  inheritModeForDynamicValueSourceType
} from '@shared/models/query/query.models';
import { AlarmConditionType } from '@shared/models/device.models';

@Component({
  selector: 'tb-alarm-duration-predicate-value',
  templateUrl: './alarm-duration-predicate-value.component.html',
  styleUrls: ['./alarm-duration-predicate-value.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmDurationPredicateValueComponent),
      multi: true
    }
  ]
})
export class AlarmDurationPredicateValueComponent implements ControlValueAccessor, OnInit {

  private readonly inheritModeForSources = inheritModeForDynamicValueSourceType;

  @Input()
  set alarmConditionType(alarmConditionType: AlarmConditionType) {
    switch (alarmConditionType) {
      case AlarmConditionType.REPEATING:
        this.defaultValuePlaceholder = 'device-profile.condition-repeating-value-required';
        this.defaultValueRequiredError = 'device-profile.condition-repeating-value-range';
        this.defaultValueRangeError = 'device-profile.condition-repeating-value-range';
        this.defaultValuePatternError = 'device-profile.condition-repeating-value-pattern';
        break;
      case AlarmConditionType.DURATION:
        this.defaultValuePlaceholder = 'device-profile.condition-duration-value';
        this.defaultValueRequiredError = 'device-profile.condition-duration-value-required';
        this.defaultValueRangeError = 'device-profile.condition-duration-value-range';
        this.defaultValuePatternError = 'device-profile.condition-duration-value-pattern';
        break;
    }
  }

  defaultValuePlaceholder = '';
  defaultValueRequiredError = '';
  defaultValueRangeError = '';
  defaultValuePatternError = '';

  dynamicValueSourceTypes: DynamicValueSourceType[] = getDynamicSourcesForAllowUser(false);

  dynamicValueSourceTypeTranslations = dynamicValueSourceTypeTranslationMap;

  alarmDurationPredicateValueFormGroup: UntypedFormGroup;

  dynamicMode = false;

  inheritMode = false;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.alarmDurationPredicateValueFormGroup = this.fb.group({
      defaultValue: [0, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]],
      dynamicValue: this.fb.group(
        {
          sourceType: [null],
          sourceAttribute: [null],
          inherit: [false]
        }
      )
    });
    this.alarmDurationPredicateValueFormGroup.get('dynamicValue').get('sourceType').valueChanges.subscribe(
      (sourceType) => {
        if (!sourceType) {
          this.alarmDurationPredicateValueFormGroup.get('dynamicValue').get('sourceAttribute').patchValue(null, {emitEvent: false});
        }
        this.updateShowInheritMode(sourceType);
      }
    );
    this.alarmDurationPredicateValueFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.alarmDurationPredicateValueFormGroup.disable({emitEvent: false});
    } else {
      this.alarmDurationPredicateValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(predicateValue: FilterPredicateValue<string | number | boolean>): void {
    this.alarmDurationPredicateValueFormGroup.patchValue({
      defaultValue: predicateValue ? predicateValue.defaultValue : null,
      dynamicValue: {
        sourceType: predicateValue?.dynamicValue ? predicateValue.dynamicValue.sourceType : null,
        sourceAttribute: predicateValue?.dynamicValue ? predicateValue.dynamicValue.sourceAttribute : null,
        inherit: predicateValue?.dynamicValue ? predicateValue.dynamicValue.inherit : null
      }
    }, {emitEvent: false});

    this.updateShowInheritMode(this.alarmDurationPredicateValueFormGroup.get('dynamicValue').get('sourceType').value);
  }

  private updateModel() {
    let predicateValue: FilterPredicateValue<string | number | boolean> = null;
    if (this.alarmDurationPredicateValueFormGroup.valid) {
      predicateValue = this.alarmDurationPredicateValueFormGroup.getRawValue();
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
      this.alarmDurationPredicateValueFormGroup.get('dynamicValue.inherit').patchValue(false, {emitEvent: false});
      this.inheritMode = false;
    }
  }
}
