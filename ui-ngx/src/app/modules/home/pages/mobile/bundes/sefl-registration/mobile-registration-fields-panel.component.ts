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

import { booleanAttribute, Component, forwardRef, Input, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { SignUpField, SignUpFieldId, SignUpFieldMap } from '@shared/models/self-register.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-mobile-registration-fields-panel',
  templateUrl: './mobile-registration-fields-panel.component.html',
  styleUrls: ['./mobile-registration-fields-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MobileRegistrationFieldsPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MobileRegistrationFieldsPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MobileRegistrationFieldsPanelComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  registrationFields = this.fb.array<SignUpField>([]);

  allowRegistrationFields: SignUpFieldId[];

  readonly maxFields = Array.from(SignUpFieldMap.keys()).length;
  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.registrationFields.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
      this.calculateAllowFields();
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.registrationFields.disable({emitEvent: false});
    } else {
      this.registrationFields.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (this.registrationFields.status !== 'DISABLED' && !this.registrationFields.valid) {
      return {
        invalidRegistrationFields: true
      };
    }
    return null;
  }

  writeValue(registrationFields: Array<SignUpField>) {
    if (this.registrationFields.length === registrationFields?.length) {
      this.registrationFields.patchValue(registrationFields, {emitEvent: false});
    } else {
      this.registrationFields.clear({emitEvent: false});
      registrationFields.forEach(item => {
        this.registrationFields.push(this.fb.control(item), {emitEvent: false})
      })
    }
    this.calculateAllowFields();
  }

  get dragEnabled(): boolean {
    return this.registrationFields.controls.length > 1;
  }

  addFields() {
    this.registrationFields.push(this.fb.control({} as any));
  }

  trackByFields(_index: number, fieldControl: AbstractControl): any {
    return fieldControl;
  }

  fieldDrop(event: CdkDragDrop<string[]>) {
    const axis = this.registrationFields.at(event.previousIndex);
    this.registrationFields.removeAt(event.previousIndex);
    this.registrationFields.insert(event.currentIndex, axis);
  }

  removeField(index: number) {
    this.registrationFields.removeAt(index);
  }

  private calculateAllowFields() {
    const allFields = Array.from(SignUpFieldMap.keys());
    const selectedFields: SignUpFieldId[] = [];
    this.registrationFields.value.forEach(item => {
      selectedFields.push(item.id);
    });
    this.allowRegistrationFields = allFields.filter(item => !selectedFields.includes(item))
  }
}
