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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceArray, coerceBoolean } from '@shared/decorators/coercion';
import { Observable, of } from 'rxjs';
import { filter, mergeMap, share, tap } from 'rxjs/operators';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

export interface StringItemsOption {
  name: string;
  value: any;
}
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
export class StringItemsListComponent implements ControlValueAccessor, OnInit {

  stringItemsForm: FormGroup;

  filteredValues: Observable<Array<StringItemsOption>>;

  searchText = '';

  itemList: StringItemsOption[] = [];

  private modelValue: Array<string> | null;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  @ViewChild('stringItemInput', {static: true}) stringItemInput: ElementRef<HTMLInputElement>;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;

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
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  editable = false;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceArray()
  predefinedValues: StringItemsOption[];

  get itemsControl(): AbstractControl {
    return this.stringItemsForm.get('items');
  }

  get itemControl(): AbstractControl {
    return this.stringItemsForm.get('item');
  }

  onTouched = () => {};
  private propagateChange: (value: any) => void = () => {};
  private dirty = false;

  constructor(private fb: FormBuilder) {
    this.stringItemsForm = this.fb.group({
      item: [null],
      items: [null]
    });
  }

  ngOnInit() {
    if (this.predefinedValues) {
      this.filteredValues = this.itemControl.valueChanges
        .pipe(
          tap((value) => {
            if (value && typeof value !== 'string') {
              this.add(value);
            } else if (value === null) {
              this.clear();
            }
          }),
          filter((value) => typeof value === 'string'),
          mergeMap(name => this.fetchValues(name)),
          share()
        );
    }
  }

  updateValidators() {
    this.itemsControl.setValidators(this.required ? [Validators.required] : []);
    this.itemsControl.updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
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
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.itemList = [];
      if (this.predefinedValues) {
        value.forEach(item => {
          const findItem = this.predefinedValues.find(option => option.value === item);
          if (findItem) {
            this.itemList.push(findItem);
          }
        });
      } else {
        value.forEach(item => this.itemList.push({value: item, name: item}));
      }
      this.itemsControl.setValue(this.itemList, {emitEvents: false});
    } else {
      this.itemsControl.setValue(null, {emitEvents: false});
      this.modelValue = null;
      this.itemList = [];
    }
    this.dirty = true;
  }

  addItem(event: MatChipInputEvent): void {
    const item = event.value?.trim() ?? '';
    if (item) {
      if (this.predefinedValues) {
        const findItems = this.predefinedValues
          .filter(value => value.name.toLowerCase().includes(item.toLowerCase()));
        if (findItems.length === 1) {
          this.add(findItems[0]);
        }
      } else {
        this.add({value: item, name: item});
      }
    }
  }

  removeItems(item: StringItemsOption) {
    const index = this.modelValue.indexOf(item.value);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      this.itemList.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.itemsControl.setValue(this.itemList);
      this.propagateChange(this.modelValue);
      this.autocomplete?.closePanel();
    }
  }

  onFocus() {
    if (this.dirty) {
      this.itemControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayValueFn(values?: StringItemsOption): string | undefined {
    return values ? values.name : undefined;
  }

  private add(item: StringItemsOption) {
    if (!this.modelValue || this.modelValue.indexOf(item.value) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(item.value);
      this.itemList.push(item);
      this.itemsControl.setValue(this.itemList);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  private fetchValues(searchText?: string): Observable<Array<StringItemsOption>> {
    if (!this.predefinedValues?.length) {
      return of([]);
    }
    this.searchText = searchText;
    let result = this.predefinedValues;
    if (searchText && searchText.length) {
      result = this.predefinedValues.filter(option => option.name.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  private clear(value: string = '') {
    this.stringItemInput.nativeElement.value = value;
    this.itemControl.patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.stringItemInput.nativeElement.blur();
      this.stringItemInput.nativeElement.focus();
    }, 0);
  }
}
