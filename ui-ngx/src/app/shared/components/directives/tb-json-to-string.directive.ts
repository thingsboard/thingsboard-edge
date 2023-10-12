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

import { Directive, ElementRef, forwardRef, HostListener, Renderer2, SkipSelf } from '@angular/core';
import {
  ControlValueAccessor,
  FormGroupDirective,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgForm,
  UntypedFormControl,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { isObject } from '@core/utils';

@Directive({
  selector: '[tb-json-to-string]',
  // eslint-disable-next-line @angular-eslint/no-host-metadata-property
  host: {
    '(blur)': 'onTouched()'
  },
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TbJsonToStringDirective),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => TbJsonToStringDirective),
    multi: true,
  },
  {
    provide: ErrorStateMatcher,
    useExisting: TbJsonToStringDirective
  }]
})

export class TbJsonToStringDirective implements ControlValueAccessor, Validator, ErrorStateMatcher {
  private propagateChange = null;
  public onTouched = () => {};
  private parseError: boolean;
  private data: any;

  @HostListener('input', ['$event.target.value']) input(newValue: any): void {
    try {
      if (newValue) {
        this.data = JSON.parse(newValue);
        if (isObject(this.data)) {
          this.parseError = false;
        } else {
          this.data = null;
          this.parseError = true;
        }
      } else {
        this.data = null;
        this.parseError = false;
      }
    } catch (e) {
      this.data = null;
      this.parseError = true;
    }

    this.propagateChange(this.data);
  }

  constructor(private render: Renderer2,
              private element: ElementRef,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {

  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && !Array.isArray(control.value) && control.touched);
  }

  validate(c: UntypedFormControl): ValidationErrors {
    return (!this.parseError) ? null : {
      invalidJSON: {
        valid: false
      }
    };
  }

  writeValue(obj: any): void {
    this.data = obj;
    this.parseError = false;
    this.render.setProperty(this.element.nativeElement, 'value', obj ? JSON.stringify(obj) : '');
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
}
