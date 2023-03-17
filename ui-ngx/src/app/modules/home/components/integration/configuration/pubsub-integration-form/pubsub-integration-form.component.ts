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
import { PubSubIntegration } from '@shared/models/integration.models';

@Component({
  selector: 'tb-pubsub-integration-form',
  templateUrl: './pubsub-integration-form.component.html',
  styleUrls: ['./pubsub-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => PubSubIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => PubSubIntegrationFormComponent),
    multi: true,
  }]
})
export class PubSubIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  pubSubIntegrationConfigForm: UntypedFormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.pubSubIntegrationConfigForm = this.fb.group({
      projectId: [null, Validators.required],
      subscriptionId: [null, Validators.required],
      serviceAccountKey: [null, Validators.required],
      serviceAccountKeyFileName: [null, Validators.required]
    });
    this.pubSubIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.pubSubIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: PubSubIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.pubSubIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.pubSubIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.pubSubIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.pubSubIntegrationConfigForm.valid ? null : {
      pubSubIntegrationConfigForm: {valid: false}
    };
  }
}
