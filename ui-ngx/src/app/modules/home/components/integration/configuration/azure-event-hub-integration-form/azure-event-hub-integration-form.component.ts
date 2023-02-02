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

import { Component, forwardRef, Input } from '@angular/core';
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
import { AzureEventHubIntegration } from '@shared/models/integration.models';

@Component({
  selector: 'tb-azure-event-hub-integration-form',
  templateUrl: './azure-event-hub-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AzureEventHubIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AzureEventHubIntegrationFormComponent),
    multi: true,
  }]
})
export class AzureEventHubIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator{

  downlinkConverter: boolean;
  @Input()
  set isSetDownlink(value: boolean) {
    if (this.downlinkConverter !== value) {
      this.downlinkConverter = value;
      this.downlinkConverterChanged();
    }
  }

  get isSetDownLink(): boolean {
    return this.downlinkConverter;
  }

  azureEventHubIntegrationConfigForm: FormGroup;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.azureEventHubIntegrationConfigForm = this.fb.group({
      connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
      connectionString: ['', [Validators.required]],
      consumerGroup: [''],
      iotHubName: ['']
    });
    this.azureEventHubIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.azureEventHubIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: AzureEventHubIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.azureEventHubIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.azureEventHubIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.azureEventHubIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.azureEventHubIntegrationConfigForm.valid ? null : {
      azureEventHubIntegrationConfigForm: {valid: false}
    };
  }

  private downlinkConverterChanged() {
    if (this.azureEventHubIntegrationConfigForm) {
      if (this.isSetDownLink) {
        this.azureEventHubIntegrationConfigForm.get('iotHubName').setValidators(Validators.required);
      } else {
        this.azureEventHubIntegrationConfigForm.get('iotHubName').setValidators([]);
      }
      this.azureEventHubIntegrationConfigForm.get('iotHubName').updateValueAndValidity();
    }
  }
}
