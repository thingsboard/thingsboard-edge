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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  mqttClientIdMaxLengthValidator,
  mqttClientIdPatternValidator,
  privateNetworkAddressValidator
} from '@home/components/integration/integration.models';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { IntegrationCredentialType, MqttIntegration } from '@shared/models/integration.models';

@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MqttIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => MqttIntegrationFormComponent),
    multi: true,
  }]
})
export class MqttIntegrationFormComponent extends IntegrationForm implements OnInit, ControlValueAccessor, Validator {

  @Input() isEdgeTemplate = false;

  mqttIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.mqttIntegrationConfigForm = this.fb.group({
      clientConfiguration: this.fb.group({
        host: ['', Validators.required],
        port: [1883, [Validators.min(1), Validators.max(65535)]],
        cleanSession: [true],
        ssl: [false],
        connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
        clientId: ['', [mqttClientIdPatternValidator, mqttClientIdMaxLengthValidator]],
        maxBytesInMessage: [32368, [Validators.min(1), Validators.max(256000000)]],
        credentials: [{
          type: IntegrationCredentialType.Anonymous
        }],
      }),
      topicFilters: [[{
        filter: '#',
        qos: 0
      }], Validators.required],
      downlinkTopicPattern: ['${topic}', Validators.required]
    });
    this.mqttIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModels(this.mqttIntegrationConfigForm.getRawValue()));
  }

  ngOnInit() {
    if (this.isEdgeTemplate) {
      this.mqttIntegrationConfigForm.get('clientConfiguration.clientId').clearValidators();
      this.mqttIntegrationConfigForm.get('clientConfiguration.clientId').updateValueAndValidity({emitEvent: false});
    }
  }

  writeValue(value: MqttIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.mqttIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mqttIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.mqttIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.mqttIntegrationConfigForm.valid ? null : {
      mqttIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.mqttIntegrationConfigForm.get('clientConfiguration.host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.mqttIntegrationConfigForm.get('clientConfiguration.host').addValidators(privateNetworkAddressValidator);
    }
    this.mqttIntegrationConfigForm.get('clientConfiguration.host').updateValueAndValidity({emitEvent: false});
  }
}
