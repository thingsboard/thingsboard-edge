///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { FunctionData, MathFunction, MathFunctionMap } from '../rule-node-config.models';
import { map, tap } from 'rxjs/operators';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-math-function-autocomplete',
  templateUrl: './math-function-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MathFunctionAutocompleteComponent),
      multi: true
    }
  ]
})
export class MathFunctionAutocompleteComponent implements ControlValueAccessor, OnInit {

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input() disabled: boolean;

  @ViewChild('operationInput', {static: true}) operationInput: ElementRef;

  mathFunctionForm: UntypedFormGroup;

  modelValue: MathFunction | null;

  searchText = '';

  filteredOptions: Observable<FunctionData[]>;

  private dirty = false;

  private mathOperation = [...MathFunctionMap.values()];

  private propagateChange = null;

  constructor(public translate: TranslateService,
              private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.mathFunctionForm = this.fb.group({
      operation: ['']
    });
    this.filteredOptions = this.mathFunctionForm.get('operation').valueChanges.pipe(
      tap(value => {
        let modelValue: MathFunction;
        if (typeof value === 'string' && MathFunction[value]) {
          modelValue = MathFunction[value];
        } else {
          modelValue = null;
        }
        this.updateView(modelValue);
      }),
      map(value => {
        this.searchText = value || '';
        return value ? this._filter(value) : this.mathOperation.slice();
      }),
    );
  }

  private _filter(searchText: string) {
    const filterValue = searchText.toLowerCase();

    return this.mathOperation.filter(option => option.name.toLowerCase().includes(filterValue)
      || option.value.toLowerCase().includes(filterValue));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mathFunctionForm.disable({emitEvent: false});
    } else {
      this.mathFunctionForm.enable({emitEvent: false});
    }
  }

  mathFunctionDisplayFn(value: MathFunction | null) {
    if (value) {
      const funcData = MathFunctionMap.get(value)
      return funcData.value + ' | ' + funcData.name;
    }
    return '';
  }

  writeValue(value: MathFunction | null): void {
    this.modelValue = value;
    this.mathFunctionForm.get('operation').setValue(value, {emitEvent: false});
    this.dirty = true;
  }

  updateView(value: MathFunction | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  onFocus() {
    if (this.dirty) {
      this.mathFunctionForm.get('operation').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.mathFunctionForm.get('operation').patchValue('');
    setTimeout(() => {
      this.operationInput.nativeElement.blur();
      this.operationInput.nativeElement.focus();
    }, 0);
  }

}
