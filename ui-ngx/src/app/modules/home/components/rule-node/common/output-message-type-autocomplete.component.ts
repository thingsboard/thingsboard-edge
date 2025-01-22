///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/public-api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface MessageType {
  name: string;
  value: string;
}

@Component({
  selector: 'tb-output-message-type-autocomplete',
  templateUrl: './output-message-type-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OutputMessageTypeAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OutputMessageTypeAutocompleteComponent),
      multi: true
    }
  ]
})

export class OutputMessageTypeAutocompleteComponent implements ControlValueAccessor, Validator {

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  set required(value) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  get required() {
    return this.requiredValue;
  }

  messageTypeFormGroup: FormGroup;

  messageTypes: MessageType[] = [
    {
      name: 'Post attributes',
      value: 'POST_ATTRIBUTES_REQUEST'
    },
    {
      name: 'Post telemetry',
      value: 'POST_TELEMETRY_REQUEST'
    },
    {
      name: 'Custom',
      value: ''
    },
  ];

  private modelValue: string | null;
  private requiredValue: boolean;
  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.messageTypeFormGroup = this.fb.group({
      messageTypeAlias: [null, [Validators.required]],
      messageType: [{value: null, disabled: true}, [Validators.maxLength(255)]]
    });
    this.messageTypeFormGroup.get('messageTypeAlias').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.updateMessageTypeValue(value));
    this.messageTypeFormGroup.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.updateView());
  }

  registerOnTouched(_fn: any): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  writeValue(value: string | null): void {
    this.modelValue = value;
    let findMessage = this.messageTypes.find(msgType => msgType.value === value);
    if (!findMessage) {
      findMessage = this.messageTypes.find(msgType => msgType.value === '');
    }
    this.messageTypeFormGroup.get('messageTypeAlias').patchValue(findMessage, {emitEvent: false});
    this.messageTypeFormGroup.get('messageType').patchValue(value, {emitEvent: false});
  }

  validate() {
    if (!this.messageTypeFormGroup.valid) {
      return {
        messageTypeInvalid: true
      };
    }
    return null;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.messageTypeFormGroup.disable({emitEvent: false});
    } else {
      this.messageTypeFormGroup.enable({emitEvent: false});
      if (this.messageTypeFormGroup.get('messageTypeAlias').value?.name !== 'Custom') {
        this.messageTypeFormGroup.get('messageType').disable({emitEvent: false});
      }
    }
  }

  private updateView() {
    const value = this.messageTypeFormGroup.getRawValue().messageType;
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private updateValidators() {
    this.messageTypeFormGroup.get('messageType').setValidators(
        this.required ? [Validators.required, Validators.maxLength(255)] : [Validators.maxLength(255)]
    );
    this.messageTypeFormGroup.get('messageType').updateValueAndValidity({emitEvent: false});
  }

  private updateMessageTypeValue(choseMessageType: MessageType) {
    if (choseMessageType?.name !== 'Custom') {
      this.messageTypeFormGroup.get('messageType').disable({emitEvent: false});
    } else {
      this.messageTypeFormGroup.get('messageType').enable({emitEvent: false});
    }
    this.messageTypeFormGroup.get('messageType').patchValue(choseMessageType.value ?? null);
  }

}
