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
  FormControl,
  FormGroupDirective,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgForm,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { isObject } from "@core/utils";

@Directive({
  selector: '[tb-json-to-string]',
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
  private parseError: boolean;
  private data: any;

  @HostListener('input', ['$event.target.value']) input(newValue: any): void {
    try {
      this.data = JSON.parse(newValue);
      if (isObject(this.data)) {
        this.parseError = false;
      } else {
        this.parseError = true;
      }
    } catch (e) {
      this.parseError = true;
    }

    this.propagateChange(this.data);
  }

  constructor(private render: Renderer2,
              private element: ElementRef,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {

  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.parseError);
    return originalErrorState || customErrorState;
  }

  validate(c: FormControl): ValidationErrors {
    return (!this.parseError) ? null : {
      invalidJSON: {
        valid: false
      }
    };
  }

  writeValue(obj: any): void {
    if (obj) {
      this.data = obj;
      this.parseError = false;
      this.render.setProperty(this.element.nativeElement, 'value', JSON.stringify(obj));
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }
}
