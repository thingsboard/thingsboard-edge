///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { AlarmConditionType, AlarmConditionTypeTranslationMap, AlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { TimeUnit, timeUnitTranslationMap } from '@shared/models/time/time.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-alarm-rule',
  templateUrl: './alarm-rule.component.html',
  styleUrls: ['./alarm-rule.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true,
    }
  ]
})
export class AlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  timeUnits = Object.keys(TimeUnit);
  timeUnitTranslations = timeUnitTranslationMap;
  alarmConditionTypes = Object.keys(AlarmConditionType);
  AlarmConditionType = AlarmConditionType;
  alarmConditionTypeTranslation = AlarmConditionTypeTranslationMap;

  @Input()
  disabled: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private modelValue: AlarmRule;

  alarmRuleFormGroup: FormGroup;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleFormGroup = this.fb.group({
      condition:  this.fb.group({
        condition: [null, Validators.required],
        spec: this.fb.group({
          type: [AlarmConditionType.SIMPLE, Validators.required],
          unit: [{value: null, disable: true}, Validators.required],
          value: [{value: null, disable: true}, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]],
          count: [{value: null, disable: true}, [Validators.required, Validators.min(1), Validators.max(2147483647), Validators.pattern('[0-9]*')]]
        })
      }, Validators.required),
      schedule: [null],
      alarmDetails: [null]
    });
    this.alarmRuleFormGroup.get('condition.spec.type').valueChanges.subscribe((type) => {
      this.updateValidators(type, true, true);
    });
    this.alarmRuleFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRule): void {
    this.modelValue = value;
    if (this.modelValue?.condition?.spec === null) {
      this.modelValue.condition.spec = {
        type: AlarmConditionType.SIMPLE
      };
    }
    this.alarmRuleFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    this.updateValidators(this.modelValue?.condition?.spec?.type);
  }

  public validate(c: FormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  private updateValidators(type: AlarmConditionType, resetDuration = false, emitEvent = false) {
    switch (type) {
      case AlarmConditionType.DURATION:
        this.alarmRuleFormGroup.get('condition.spec.value').enable();
        this.alarmRuleFormGroup.get('condition.spec.unit').enable();
        this.alarmRuleFormGroup.get('condition.spec.count').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            count: null
          });
        }
        break;
      case AlarmConditionType.REPEATING:
        this.alarmRuleFormGroup.get('condition.spec.count').enable();
        this.alarmRuleFormGroup.get('condition.spec.value').disable();
        this.alarmRuleFormGroup.get('condition.spec.unit').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            value: null,
            unit: null
          });
        }
        break;
      case AlarmConditionType.SIMPLE:
        this.alarmRuleFormGroup.get('condition.spec.value').disable();
        this.alarmRuleFormGroup.get('condition.spec.unit').disable();
        this.alarmRuleFormGroup.get('condition.spec.count').disable();
        if (resetDuration) {
          this.alarmRuleFormGroup.get('condition.spec').patchValue({
            value: null,
            unit: null,
            count: null
          });
        }
        break;
    }
    this.alarmRuleFormGroup.get('condition.spec.value').updateValueAndValidity({emitEvent});
    this.alarmRuleFormGroup.get('condition.spec.unit').updateValueAndValidity({emitEvent});
    this.alarmRuleFormGroup.get('condition.spec.count').updateValueAndValidity({emitEvent});
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    if (this.modelValue) {
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    }
  }
}
