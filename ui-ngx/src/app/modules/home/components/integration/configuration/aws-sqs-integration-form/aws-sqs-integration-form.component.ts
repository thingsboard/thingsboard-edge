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

import { Component, forwardRef } from '@angular/core';
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
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { AwsSqsIntegration } from '@shared/models/integration.models';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
  selector: 'tb-aws-sqs-integration-form',
  templateUrl: './aws-sqs-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AwsSqsIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AwsSqsIntegrationFormComponent),
    multi: true,
  }]
})
export class AwsSqsIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  awsSqsIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.awsSqsIntegrationConfigForm = this.fb.group({
      queueUrl: ['', [Validators.required]],
      pollingPeriodSeconds: [5, [Validators.required, Validators.min(1)]],
      region: ['us-west-2', [Validators.required]],
      accessKeyId: ['', [Validators.required]],
      secretAccessKey: ['', [Validators.required]]
    });
    this.awsSqsIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.awsSqsIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: AwsSqsIntegration) {
    if (isDefinedAndNotNull(value?.sqsConfiguration)) {
      this.awsSqsIntegrationConfigForm.reset(value.sqsConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.awsSqsIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.awsSqsIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({sqsConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.awsSqsIntegrationConfigForm.valid ? null : {
      awsSqsIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.awsSqsIntegrationConfigForm.get('queueUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.awsSqsIntegrationConfigForm.get('queueUrl').addValidators(privateNetworkAddressValidator);
    }
    this.awsSqsIntegrationConfigForm.get('queueUrl').updateValueAndValidity({emitEvent: false});
  }
}
