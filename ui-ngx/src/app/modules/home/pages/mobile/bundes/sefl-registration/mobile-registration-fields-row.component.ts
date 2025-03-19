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

import { booleanAttribute, Component, EventEmitter, forwardRef, Input, Output, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  alwaysRequiredSignUpFields,
  SignUpField,
  SignUpFieldId,
  SignUpFieldMap
} from '@shared/models/self-register.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-mobile-registration-fields-row',
  templateUrl: 'mobile-registration-fields-row.component.html',
  styleUrls: ['./mobile-registration-fields-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MobileRegistrationFieldsRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MobileRegistrationFieldsRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MobileRegistrationFieldsRowComponent implements ControlValueAccessor, Validator {

  @Input()
  allowFieldIds: SignUpFieldId[];

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Output()
  fieldRemoved = new EventEmitter();

  fieldForm = this.fb.group({
    required: [false],
    id: this.fb.control<SignUpFieldId>(null, Validators.required),
    label: ['', Validators.required]
  });

  SignUpFieldMap = SignUpFieldMap;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.fieldForm.get('id').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      const item = SignUpFieldMap.get(value);
      if (this.fieldForm.get('required').untouched) {
        this.fieldForm.get('required').setValue(item.required);
      }
      if (this.fieldForm.get('label').untouched) {
        this.fieldForm.get('label').setValue(this.translate.instant(item.label));
      }
    })

    this.fieldForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.propagateChange(this.fieldForm.getRawValue()));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.fieldForm.disable({emitEvent: false});
    } else {
      this.fieldForm.enable({emitEvent: false});
      this.updatedDisabledState();
      setTimeout(() => {
        this.fieldForm.updateValueAndValidity();
      }, 0);
    }
  }

  validate(): ValidationErrors | null {
    if (this.fieldForm.status !== 'DISABLED' && !this.fieldForm.valid) {
      return {
        invalidFieldForm: true
      };
    }
    return null;
  }

  writeValue(value: SignUpField) {
    this.fieldForm.patchValue(value, {emitEvent: false});
    this.updatedDisabledState();
  }

  get hideModify(): boolean {
    return alwaysRequiredSignUpFields.includes(this.fieldForm.get('id').value);
  }

  private updatedDisabledState() {
    if (this.hideModify) {
      this.fieldForm.get('required').disable({emitEvent: false});
      this.fieldForm.get('id').disable({emitEvent: false});
    } else {
      this.fieldForm.get('required').enable({emitEvent: false});
      this.fieldForm.get('id').enable({emitEvent: false});
    }
  }
}
