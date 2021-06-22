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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LWM2M_ENUM,
  ATTRIBUTE_LWM2M_MAP
} from './lwm2m-profile-config.models';
import { isDefinedAndNotNull, isEmpty, isEmptyStr, isUndefinedOrNull } from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';


@Component({
  selector: 'tb-lwm2m-attributes-key-list',
  templateUrl: './lwm2m-attributes-key-list.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mAttributesKeyListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mAttributesKeyListComponent),
      multi: true,
    }
  ]
})
export class Lwm2mAttributesKeyListComponent extends PageComponent implements ControlValueAccessor, OnInit, Validator {

  attrKeys = ATTRIBUTE_KEYS;

  attrKey = ATTRIBUTE_LWM2M_ENUM;

  attributeLwm2mMap = ATTRIBUTE_LWM2M_MAP;

  @Input() disabled: boolean;

  @Input() titleText: string;

  @Input() noDataText: string;

  kvListFormGroup: FormGroup;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.kvListFormGroup = this.fb.group({});
    this.kvListFormGroup.addControl('keyVals',
      this.fb.array([]));
  }

  keyValsFormArray(): FormArray {
    return this.kvListFormGroup.get('keyVals') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(keyValMap: { [key: string]: string }): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const keyValsControls: Array<AbstractControl> = [];
    if (keyValMap) {
      for (const property of Object.keys(keyValMap)) {
        if (Object.prototype.hasOwnProperty.call(keyValMap, property)) {
          keyValsControls.push(this.fb.group({
            key: [property, [Validators.required, this.attributeLwm2mKeyValidator]],
            value: [keyValMap[property], this.attributeLwm2mValueValidator(property)]
          }));
        }
      }
    }
    this.kvListFormGroup.setControl('keyVals', this.fb.array(keyValsControls));
    this.valueChangeSubscription = this.kvListFormGroup.valueChanges.subscribe(() => {
      // this.updateValidate();
      this.updateModel();
    });
    if (this.disabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  public removeKeyVal(index: number) {
    (this.kvListFormGroup.get('keyVals') as FormArray).removeAt(index);
  }

  public addKeyVal() {
    const keyValsFormArray = this.kvListFormGroup.get('keyVals') as FormArray;
    keyValsFormArray.push(this.fb.group({
      key: ['', [Validators.required, this.attributeLwm2mKeyValidator]],
      value: ['', []]
    }));
  }

  public validate(c?: FormControl) {
    const kvList: { key: string; value: string }[] = this.kvListFormGroup.get('keyVals').value;
    let valid = true;
    for (const entry of kvList) {
      if (isUndefinedOrNull(entry.key) || isEmptyStr(entry.key) || !ATTRIBUTE_KEYS.includes(entry.key)) {
        valid = false;
        break;
      }
      if (entry.key !== 'ver' && isNaN(Number(entry.value))) {
        valid = false;
        break;
      }
    }
    return (valid) ? null : {
      keyVals: {
        valid: false,
      },
    };
  }

  private updateValidate() {
    const kvList = this.kvListFormGroup.get('keyVals') as FormArray;
    kvList.controls.forEach(fg => {
      if (fg.get('key').value === 'ver') {
        fg.get('value').setValidators(null);
        fg.get('value').setErrors(null);
      }
      else {
        fg.get('value').setValidators(this.attributeLwm2mValueNumberValidator);
        fg.get('value').setErrors(this.attributeLwm2mValueNumberValidator(fg.get('value')));
      }
    });
  }

  private updateModel() {
    this.updateValidate();
    if (this.validate() === null) {
      const kvList: { key: string; value: string }[] = this.kvListFormGroup.get('keyVals').value;
      const keyValMap: { [key: string]: string | number } = {};
      kvList.forEach((entry) => {
        if (isUndefinedOrNull(entry.value) || entry.key === 'ver' || isEmptyStr(entry.value.toString())) {
          keyValMap[entry.key] = entry.value.toString();
        } else {
          keyValMap[entry.key] = Number(entry.value);
        }
      });
      this.propagateChange(keyValMap);
    }
    else {
      this.propagateChange(null);
    }
  }


  private attributeLwm2mKeyValidator = (control: AbstractControl) => {
    const key = control.value as string;
    if (isDefinedAndNotNull(key) && !isEmpty(key)) {
      if (!ATTRIBUTE_KEYS.includes(key)) {
        return {
          validAttributeKey: true
        };
      }
    }
    return null;
  }

  private attributeLwm2mValueNumberValidator = (control: AbstractControl) => {
    if (isNaN(Number(control.value)) || Number(control.value) < 0) {
      return {
        validAttributeValue: true
      };
    }
    return null;
  }

  private attributeLwm2mValueValidator = (property: string): object[] => {
    return property === 'ver' ?  [] : [this.attributeLwm2mValueNumberValidator];
  }
}
