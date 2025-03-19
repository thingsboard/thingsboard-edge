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

import { Component, DestroyRef, forwardRef, HostBinding, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { cssUnit, resolveCssSize } from '@shared/models/widget-settings.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-css-size-input',
  templateUrl: './css-size-input.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CssSizeInputComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CssSizeInputComponent),
      multi: true,
    }
  ]
})
export class CssSizeInputComponent implements OnInit, ControlValueAccessor, Validator {

  @HostBinding('style.width')
  get hostWidth(): string {
    return this.flex ? '100%' : null;
  }

  @HostBinding('style.flex')
  get hostFlex(): string {
    return this.flex ? '1' : null;
  }

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  allowEmptyUnit = false;

  @Input()
  @coerceBoolean()
  flex = false;

  cssSizeFormGroup: UntypedFormGroup;

  modelValue: string;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.cssSizeFormGroup = this.fb.group({
      size: [null, this.required ? [Validators.required, Validators.min(0)] : [Validators.min(0)]],
      unit: [null, []]
    });
    this.cssSizeFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: {size: number; unit: cssUnit}) => {
      this.updateModel(value);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.cssSizeFormGroup.disable({emitEvent: false});
    } else {
      this.cssSizeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    const size = resolveCssSize(value);
    this.cssSizeFormGroup.patchValue({
      size: size[0],
      unit: size[1]
    }, {emitEvent: false});
  }

  validate(_c: UntypedFormControl) {
    return this.cssSizeFormGroup.valid ? null : {
      cssSize: {
        valid: false,
      }
    };
  }

  private updateModel(value: {size: number; unit: cssUnit}): void {
    const result: string = isDefinedAndNotNull(value?.size) && isDefinedAndNotNull(value?.unit)
      ? value.size + value.unit : '';
    if (this.modelValue !== result) {
      this.modelValue = result;
      this.propagateChange(this.modelValue);
    }
  }
}
