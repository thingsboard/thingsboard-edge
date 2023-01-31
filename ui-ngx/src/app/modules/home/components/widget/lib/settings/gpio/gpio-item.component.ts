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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, ValidationErrors, ValidatorFn,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

export interface GpioItem {
  pin: number;
  label: string;
  row: number;
  col: number;
  color?: string;
}

export function gpioItemValidator(hasColor: boolean): ValidatorFn {
  return (control: AbstractControl) => {
    const gpioItem: GpioItem = control.value;
    if (!gpioItem
      || !isNumber(gpioItem.pin) || gpioItem.pin < 1
      || !isNumber(gpioItem.row) || gpioItem.row < 0
      || !isNumber(gpioItem.col) || gpioItem.col < 0 || gpioItem.col > 1
      || !gpioItem.label
      || (hasColor && !gpioItem.color)
    ) {
      return {
        gpioItem: true
      };
    }
    return null;
  };
}

@Component({
  selector: 'tb-gpio-item',
  templateUrl: './gpio-item.component.html',
  styleUrls: ['./gpio-item.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GpioItemComponent),
      multi: true
    }
  ]
})
export class GpioItemComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  hasColor = false;

  @Output()
  removeGpioItem = new EventEmitter();

  private modelValue: GpioItem;

  private propagateChange = null;

  public gpioItemFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.gpioItemFormGroup = this.fb.group({
      pin: [null, [Validators.required, Validators.min(1)]],
      label: [null, [Validators.required]],
      row: [null, [Validators.required, Validators.min(0)]],
      col: [null, [Validators.required, Validators.min(0), Validators.max(1)]]
    });
    if (this.hasColor) {
      this.gpioItemFormGroup.addControl('color', this.fb.control(null, [Validators.required]));
    }
    this.gpioItemFormGroup.valueChanges.subscribe(() => {
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
    if (isDisabled) {
      this.gpioItemFormGroup.disable({emitEvent: false});
    } else {
      this.gpioItemFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: GpioItem): void {
    this.modelValue = value;
    this.gpioItemFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  gpioItemHtml(): SafeHtml {
    const value: GpioItem = this.gpioItemFormGroup.value;
    const pin = isNumber(value.pin) && value.pin > 0 ? value.pin : 'Undefined';
    const row = isNumber(value.row) && value.row > -1 ? value.row : 'Undefined';
    const col = isNumber(value.col) && value.col > -1 ? value.col : 'Undefined';
    const label = value.label || 'Undefined';
    return this.domSanitizer.bypassSecurityTrustHtml(`${label} (<small>pin:</small>${pin}) - [<small>row:</small>${row}:<small>col:</small>${col}]`);
  }

  private updateModel() {
    const value: GpioItem = this.gpioItemFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
