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

import { Component, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, NgForm } from '@angular/forms';
import { ValueType, valueTypesMap } from '@shared/models/constants';
import { isObject } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from '@shared/components/dialog/json-object-edit-dialog.component';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-value-input',
  templateUrl: './value-input.component.html',
  styleUrls: ['./value-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ValueInputComponent),
      multi: true
    }
  ]
})
export class ValueInputComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input() requiredText: string;

  @ViewChild('inputForm', {static: true}) inputForm: NgForm;

  private stringNotRequiredValue: boolean;
  get stringNotRequired(): boolean {
    return this.stringNotRequiredValue;
  }
  @Input()
  set stringNotRequired(value: boolean) {
    this.stringNotRequiredValue = coerceBooleanProperty(value);
  }

  modelValue: any;

  valueType: ValueType;

  public valueTypeEnum = ValueType;

  valueTypeKeys = Object.keys(ValueType);

  valueTypes = valueTypesMap;

  private propagateChange = null;

  constructor(
    public dialog: MatDialog,
  ) {

  }

  ngOnInit(): void {
  }

  openEditJSONDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, object>(JsonObjectEditDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: this.modelValue
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.modelValue = res;
          this.inputForm.control.patchValue({value: this.modelValue});
        }
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.modelValue = value;
    if (this.modelValue === true || this.modelValue === false) {
      this.valueType = ValueType.BOOLEAN;
    } else if (typeof this.modelValue === 'number') {
      if (this.modelValue.toString().indexOf('.') === -1) {
        this.valueType = ValueType.INTEGER;
      } else {
        this.valueType = ValueType.DOUBLE;
      }
    } else if (isObject(this.modelValue)) {
      this.valueType = ValueType.JSON;
    } else {
      this.valueType = ValueType.STRING;
    }
  }

  updateView() {
    if (this.inputForm.valid || this.valueType === ValueType.BOOLEAN) {
      let value = this.modelValue;
      if (this.stringNotRequired && this.valueType === ValueType.STRING && !value) {
        value = '';
      }
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  onValueTypeChanged() {
    if (this.valueType === ValueType.BOOLEAN) {
      this.modelValue = false;
    } else if (this.valueType === ValueType.JSON) {
      this.modelValue = {};
      this.inputForm.form.get('value').patchValue({});
    } else if (this.valueType === ValueType.STRING && this.stringNotRequired) {
      this.modelValue = '';
    } else {
      this.modelValue = null;
    }
    setTimeout(() => {
      this.updateView();
    }, 0);
  }

  onValueChanged() {
    this.updateView();
  }

}
