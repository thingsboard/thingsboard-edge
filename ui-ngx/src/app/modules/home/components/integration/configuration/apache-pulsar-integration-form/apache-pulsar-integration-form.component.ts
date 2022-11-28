///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { ApachePulsarIntegration, IntegrationCredentialType } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
  selector: 'tb-apache-pulsar-integration-form',
  templateUrl: './apache-pulsar-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ApachePulsarIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => ApachePulsarIntegrationFormComponent),
    multi: true,
  }]
})
export class ApachePulsarIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  apachePulsarIntegrationConfigForm: FormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.apachePulsarIntegrationConfigForm = this.fb.group({
      serviceUrl: ['pulsar://localhost:6650', Validators.required],
      topics: ['my-topic', Validators.required],
      subscriptionName: ['my-subscription', Validators.required],
      maxNumMessages: [1000, Validators.required],
      maxNumBytes: [10 * 1024 * 1024, Validators.required],
      timeoutInMs: [100, Validators.required],
      credentials: [{
        type: IntegrationCredentialType.Anonymous
      }]
    });

    this.apachePulsarIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.apachePulsarIntegrationConfigForm.getRawValue());
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.apachePulsarIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.apachePulsarIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.apachePulsarIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.apachePulsarIntegrationConfigForm.valid ? null : {
      apachePulsarIntegrationConfigForm: {valid: false}
    };
  }

  writeValue(value: ApachePulsarIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.apachePulsarIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    } else {
      this.propagateChangePending = true;
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.apachePulsarIntegrationConfigForm.get('serviceUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.apachePulsarIntegrationConfigForm.get('serviceUrl').addValidators(privateNetworkAddressValidator);
    }
    this.apachePulsarIntegrationConfigForm.get('serviceUrl').updateValueAndValidity({emitEvent: false});
  }
}
