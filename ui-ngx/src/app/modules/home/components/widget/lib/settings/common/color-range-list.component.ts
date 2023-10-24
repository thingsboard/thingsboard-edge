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
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ColorRange } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
  selector: 'tb-color-range-list',
  templateUrl: './color-range-list.component.html',
  styleUrls: ['color-settings-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorRangeListComponent),
      multi: true
    }
  ]
})
export class ColorRangeListComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input()
  popover: TbPopoverComponent;

  @Input()
  panelTitle: string;

  modelValue: any;

  colorRangeListFormGroup: UntypedFormGroup;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              public dialog: MatDialog) {

  }

  ngOnInit(): void {
    this.colorRangeListFormGroup = this.fb.group({
        rangeList: this.fb.array([])
    });

    this.colorRangeListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
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
    if (value && value?.length) {
      value.forEach((r) => this.rangeListFormArray.push(this.colorRangeControl(r), {emitEvent: false}));
    }
  }

  private colorRangeControl(range: ColorRange): AbstractControl {
    return this.fb.group({
      from: [range?.from, []],
      to: [range?.to, []],
      color: [range?.color, []]
    });
  }

  get rangeListFormArray(): UntypedFormArray {
    return this.colorRangeListFormGroup.get('rangeList') as UntypedFormArray;
  }

  get rangeListFormGroups(): FormGroup[] {
    return this.rangeListFormArray.controls as FormGroup[];
  }

  trackByRange(index: number, rangeControl: AbstractControl): any {
    return rangeControl;
  }

  removeRange(index: number) {
    this.rangeListFormArray.removeAt(index);
    this.colorRangeListFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  addRange() {
    const newRange: ColorRange = {
      color: 'rgba(0,0,0,0.87)'
    };
    this.rangeListFormArray.push(this.colorRangeControl(newRange));
    this.colorRangeListFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  updateModel() {
    this.propagateChange(this.colorRangeListFormGroup.get('rangeList').value);
  }

}
