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
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subscription } from 'rxjs';
import { isDefinedAndNotNull, isEqual } from '@core/utils';

@Component({
  selector: 'tb-key-val-map',
  templateUrl: './kv-map.component.html',
  styleUrls: ['./kv-map.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KeyValMapComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => KeyValMapComponent),
      multi: true,
    }
  ]
})
export class KeyValMapComponent extends PageComponent implements ControlValueAccessor, OnInit, Validator {

  @Input() disabled: boolean;

  @Input() titleText: string;

  @Input() keyPlaceholderText: string;

  @Input() valuePlaceholderText: string;

  @Input() noDataText: string;

  @Input() singlePredefinedKey: string;

  @Input() isStrokedButton = false;

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
    if (keyValMap && !isEqual(keyValMap, {})) {
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
    if (this.isSinglePredefinedKey && !keyValsControls.length) {
      this.addKeyVal();
    }
    this.valueChangeSubscription = this.kvListFormGroup.valueChanges.subscribe(() => {
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
      key: [this.isSinglePredefinedKey ? this.singlePredefinedKey : '', [Validators.required]],
      value: ['', [Validators.required]]
    }));
  }

  public validate(c: FormControl) {
    const kvList: {key: string; value: string}[] = this.kvListFormGroup.get('keyVals').value;
    let valid = true;
    for (const entry of kvList) {
      if (!entry.key || !entry.value) {
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

  get isSingleMode(): boolean {
    return isDefinedAndNotNull(this.singlePredefinedKey);
  }

  get isSinglePredefinedKey(): boolean {
    return isDefinedAndNotNull(this.singlePredefinedKey);
  }

  private updateModel() {
    const kvList: {key: string; value: string}[] = this.kvListFormGroup.get('keyVals').value;
    const keyValMap: {[key: string]: string} = {};
    kvList.forEach((entry) => {
      keyValMap[entry.key] = entry.value;
    });
    this.propagateChange(keyValMap);
  }
}
