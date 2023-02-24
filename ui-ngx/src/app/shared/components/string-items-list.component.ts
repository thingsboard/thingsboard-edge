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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { FloatLabelType, MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coerce-boolean';

@Component({
  selector: 'tb-string-items-list',
  templateUrl: './string-items-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringItemsListComponent),
      multi: true
    }
  ]
})
export class StringItemsListComponent implements ControlValueAccessor{

  stringItemsForm: FormGroup;
  private modelValue: Array<string> | null;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  @coerceBoolean()
  set required(value: boolean) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  label: string;

  @Input()
  placeholder: string;

  @Input()
  hint: string;

  @Input()
  requiredText: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'standard';

  @Input()
  @coerceBoolean()
  editable: boolean = false

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.stringItemsForm = this.fb.group({
      items: [null, this.required ? [Validators.required] : []]
    });
  }

  updateValidators() {
    this.stringItemsForm.get('items').setValidators(this.required ? [Validators.required] : []);
    this.stringItemsForm.get('items').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stringItemsForm.disable({emitEvent: false});
    } else {
      this.stringItemsForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.stringItemsForm.get('items').setValue(value);
    } else {
      this.stringItemsForm.get('items').setValue(null);
      this.modelValue = null;
    }
  }

  addItem(event: MatChipInputEvent): void {
    let item = event.value || '';
    const input = event.chipInput.inputElement;
    item = item.trim();
    if (item) {
      if (!this.modelValue || this.modelValue.indexOf(item) === -1) {
        if (!this.modelValue) {
          this.modelValue = [];
        }
        this.modelValue.push(item);
        this.stringItemsForm.get('items').setValue(this.modelValue);
      }
      this.propagateChange(this.modelValue);
      if (input) {
        input.value = '';
      }
    }
  }

  removeItems(item: string) {
    const index = this.modelValue.indexOf(item);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.stringItemsForm.get('items').setValue(this.modelValue);
      this.propagateChange(this.modelValue);
    }
  }

  get stringItemsList(): string[] {
    return this.stringItemsForm.get('items').value;
  }

}
