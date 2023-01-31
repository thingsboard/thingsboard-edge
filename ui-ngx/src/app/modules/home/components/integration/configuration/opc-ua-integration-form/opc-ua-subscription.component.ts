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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { OpcUaSubscription } from '@shared/models/integration.models';

@Component({
  selector: 'tb-opc-ua-subscription',
  templateUrl: './opc-ua-subscription.component.html',
  styleUrls: ['./opc-ua-subscription.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OpcUaSubscriptionComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => OpcUaSubscriptionComponent),
    multi: true,
  }]
})
export class OpcUaSubscriptionComponent implements ControlValueAccessor, Validator, OnDestroy {

  opcSubscriptionForm: FormGroup;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.opcSubscriptionForm = this.fb.group({
      subscription: this.fb.array([], Validators.required)
    });
    this.opcSubscriptionForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.updateModels(value.subscription);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.opcSubscriptionForm.disable({emitEvent: false});
    } else {
      this.opcSubscriptionForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.opcSubscriptionForm.valid && this.opcSubscriptionArray.length ? null : {
      opcSubscriptionForm: {valid: false}
    };
  }

  writeValue(subscriptions: OpcUaSubscription[]) {
    if (isDefinedAndNotNull(subscriptions)) {
      if (this.opcSubscriptionArray.length === subscriptions.length) {
        this.opcSubscriptionForm.get('subscription').patchValue(subscriptions, {emitEvent: false});
      } else {
        const subscriptionControls: Array<FormGroup> = [];
        subscriptions.forEach((subscription) => {
          subscriptionControls.push(this.createFormGroup(subscription));
        });
        this.opcSubscriptionForm.setControl('subscription', this.fb.array(subscriptionControls), {emitEvent: false});
        if (this.disabled) {
          this.opcSubscriptionForm.disable({emitEvent: false});
        }
      }
    } else {
      this.addSubscriptionTag(false);
    }
  }

  get opcSubscriptionArray(): FormArray {
    return this.opcSubscriptionForm.get('subscription') as FormArray;
  }

  get opcSubscriptionArrayControls(): FormGroup[] {
    return this.opcSubscriptionArray.controls as FormGroup[];
  }

  addSubscriptionTag(emitEvent = true) {
    this.opcSubscriptionArray.push(this.createFormGroup(), {emitEvent});
  }

  private createFormGroup(value?: any): FormGroup {
    return this.fb.group(
      {
        key: [value?.key || '', [Validators.required]],
        path: [value?.path || '', [Validators.required]],
        required: [value?.required || false]
      }
    );
  }

  removeSubscriptionTag(index: number) {
    this.opcSubscriptionArray.removeAt(index);
  }

  private updateModels(value) {
    this.propagateChange(value);
  }
}
