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
import { IbmWatsonIotIntegration, IntegrationCredentialType } from '@shared/models/integration.models';

@Component({
  selector: 'tb-ibm-watson-iot-integration-form',
  templateUrl: './ibm-watson-iot-integration-form.component.html',
  styleUrls: ['./ibm-watson-iot-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IbmWatsonIotIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => IbmWatsonIotIntegrationFormComponent),
    multi: true,
  }]
})
export class IbmWatsonIotIntegrationFormComponent extends IntegrationForm implements OnInit, ControlValueAccessor, Validator {

  @Input() isEdgeTemplate = false;

  ibmWatsonIotIntegrationConfigForm: FormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  private ibmWatsonIotApiKeyPatternValidator = Validators.pattern(/^a-\w+-\w+$/);

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.ibmWatsonIotIntegrationConfigForm = this.fb.group({
      clientConfiguration: this.fb.group({
        connectTimeoutSec: [10, [Validators.required, Validators.min(1), Validators.max(200)]],
        maxBytesInMessage: [32368, [Validators.min(1), Validators.max(256000000)]],
        credentials: this.fb.group({
          type: [IntegrationCredentialType.Basic],
          username: ['', [Validators.required, this.ibmWatsonIotApiKeyPatternValidator]],
          password: ['', Validators.required],
        }),
      }),
      topicFilters: [[{
        filter: 'iot-2/type/+/id/+/evt/+/fmt/+',
        qos: 0
      }], Validators.required],
      downlinkTopicPattern: ['iot-2/type/${device_type}/id/${device_id}/cmd/${command_id}/fmt/${format}', [Validators.required]],
    });

    this.ibmWatsonIotIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModels(this.ibmWatsonIotIntegrationConfigForm.getRawValue()));
  }

  ngOnInit() {
    if (this.isEdgeTemplate) {
      this.ibmWatsonIotIntegrationConfigForm.get('clientConfiguration.credentials.username').setValidators(Validators.required);
      this.ibmWatsonIotIntegrationConfigForm.get('clientConfiguration.credentials.username').updateValueAndValidity({emitEvent: false});
    }
  }

  writeValue(value: IbmWatsonIotIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.ibmWatsonIotIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ibmWatsonIotIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.ibmWatsonIotIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.ibmWatsonIotIntegrationConfigForm.valid ? null : {
      ibmWatsonIotIntegrationConfigForm: {valid: false}
    };
  }
}
