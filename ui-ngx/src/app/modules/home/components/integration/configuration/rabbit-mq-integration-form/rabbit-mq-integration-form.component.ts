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
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { RabbitMqIntegration } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
  selector: 'tb-rabbit-mq-integration-form',
  templateUrl: './rabbit-mq-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RabbitMqIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => RabbitMqIntegrationFormComponent),
    multi: true,
  }]
})
export class RabbitMqIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  rabbitMqIntegrationConfigForm: FormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.rabbitMqIntegrationConfigForm = this.fb.group({
      exchangeName: ['', []],
      host: ['', [Validators.required]],
      port: [5672, [Validators.required, Validators.min(1), Validators.max(65535)]],
      virtualHost: ['', []],
      username: ['', []],
      password: ['', []],
      downlinkTopic: ['', []],
      queues: ['my-queue', [Validators.required]],
      routingKeys: ['my-routing-key', []],
      connectionTimeout: [60000, [Validators.min(0)]],
      handshakeTimeout: [10000, [Validators.min(0)]],
      pollPeriod: [5000, [Validators.min(0)]],
      durable: [false, []],
      exclusive: [true, []],
      autoDelete: [true, []],
    });
    this.rabbitMqIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.rabbitMqIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: RabbitMqIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.rabbitMqIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.rabbitMqIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.rabbitMqIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.rabbitMqIntegrationConfigForm.valid ? null : {
      rabbitMqIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.rabbitMqIntegrationConfigForm.get('host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.rabbitMqIntegrationConfigForm.get('host').addValidators(privateNetworkAddressValidator);
    }
    this.rabbitMqIntegrationConfigForm.get('host').updateValueAndValidity({emitEvent: false});
  }
}
