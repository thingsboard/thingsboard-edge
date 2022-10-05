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
import { ChangeDetectorRef, Component, forwardRef, Input } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { mqttCredentialTypes } from '@home/components/integration/integration.models';

@Component({
  selector: 'tb-integration-credentials',
  templateUrl: 'integration-credentials.component.html',
  styleUrls: ['integration-credentials.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IntegrationCredentialsComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => IntegrationCredentialsComponent),
    multi: true,
  }]
})
export class IntegrationCredentialsComponent implements ControlValueAccessor, Validator {

  integrationCredentialForm: FormGroup;

  mqttCredentialTypes = mqttCredentialTypes;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private cd: ChangeDetectorRef) {
    this.integrationCredentialForm = this.fb.group({
      type: [mqttCredentialTypes.anonymous.value],
      username: [{value: '', disabled: true}, Validators.required],
      password: [{value: '', disabled: true}, Validators.required],
      caCertFileName: [{value: '', disabled: true}, Validators.required],
      caCert: [{value: '', disabled: true}, Validators.required],
      certFileName: [{value: '', disabled: true}, Validators.required],
      cert: [{value: '', disabled: true}, Validators.required],
      privateKeyFileName: [{value: '', disabled: true}, Validators.required],
      privateKey: [{value: '', disabled: true}, Validators.required],
      privateKeyPassword: [{value: '', disabled: true}]
    });
    this.integrationCredentialForm.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(type => this.updatedValidation(type));
    this.integrationCredentialForm.valueChanges.pipe(
      debounceTime(100),
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.integrationCredentialForm.disable({emitEvent: false});
    } else {
      this.integrationCredentialForm.enable({emitEvent: false});
    }
  }

  writeValue(value) {
    this.integrationCredentialForm.reset(value || {type: mqttCredentialTypes.anonymous.value}, {emitEvent: false});
    this.updatedValidation(value?.type || mqttCredentialTypes.anonymous.value);
    if (!this.disabled && !this.integrationCredentialForm.valid) {
      this.updateModel(this.integrationCredentialForm.value);
    }
  }

  private updatedValidation(type) {
    this.integrationCredentialForm.disable({emitEvent: false});
    switch (type) {
      case 'anonymous':
        break;
      case 'basic':
        this.integrationCredentialForm.get('username').enable({emitEvent: false});
        this.integrationCredentialForm.get('password').enable({emitEvent: false});
        break;
      case 'cert.PEM':
        this.integrationCredentialForm.get('caCertFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCert').enable({emitEvent: false});
        this.integrationCredentialForm.get('certFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('cert').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKey').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyPassword').enable({emitEvent: false});
        break;
    }
    this.integrationCredentialForm.get('type').enable({emitEvent: false});
  }

  private updateModel(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.integrationCredentialForm.valid ? null : {
      integrationCredential: {valid: false}
    };
  }
}
