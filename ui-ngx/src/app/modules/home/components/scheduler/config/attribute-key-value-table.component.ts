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
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-attribute-key-value-table',
  templateUrl: './attribute-key-value-table.component.html',
  styleUrls: ['./attribute-key-value-table.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AttributeKeyValueTableComponent),
      multi: true
    }
  ]
})
export class AttributeKeyValueTableComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() titleText: string;

  @Input() requiredPrompt: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  kvListFormGroup: UntypedFormGroup;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.kvListFormGroup = this.fb.group({});
    this.kvListFormGroup.addControl('keyVals',
      this.fb.array([]));
  }

  keyValsFormArray(): UntypedFormArray {
    return this.kvListFormGroup.get('keyVals') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(keyValMap: {[key: string]: string}): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const keyValsControls: Array<AbstractControl> = [];
    if (keyValMap) {
      for (const property of Object.keys(keyValMap)) {
        if (Object.prototype.hasOwnProperty.call(keyValMap, property)) {
          keyValsControls.push(this.fb.group({
            key: [property, [Validators.required]],
            value: [keyValMap[property], [Validators.required]]
          }));
        }
      }
    }
    this.kvListFormGroup.setControl('keyVals', this.fb.array(keyValsControls));
    this.valueChangeSubscription = this.kvListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public removeKeyVal(index: number) {
    (this.kvListFormGroup.get('keyVals') as UntypedFormArray).removeAt(index);
  }

  public addKeyVal() {
    const keyValsFormArray = this.kvListFormGroup.get('keyVals') as UntypedFormArray;
    keyValsFormArray.push(this.fb.group({
      key: ['', [Validators.required]],
      value: ['', [Validators.required]]
    }));
  }

  private updateModel() {
    const kvList: {key: string; value: string}[] = this.kvListFormGroup.get('keyVals').value;
    const keyValMap: {[key: string]: string} = {};
    kvList.forEach((entry) => {
      keyValMap[entry.key.trim()] = entry.value;
    });
    this.propagateChange(keyValMap);
  }
}

export function attributeKeyValueValidator(required: boolean): ValidatorFn {
  return control => {
    const keyValMap: {[key: string]: string} = control.value;
    let requiredError = false;
    let invalidError = false;
    const keys = keyValMap ? Object.keys(keyValMap) : [];
    if (required && !keys.length) {
      requiredError = true;
    }
    if (!requiredError) {
      for (const key of keys) {
        if (!key || !key.length) {
          invalidError = true;
          break;
        }
        const value = keyValMap[key];
        if (value === null) {
          invalidError = true;
          break;
        }
      }
    }
    let errors = null;
    if (requiredError || invalidError) {
      errors = {};
      if (requiredError) {
        errors.required = true;
      }
      if (invalidError) {
        errors.invalid = true;
      }
    }
    return errors;
  };
}
