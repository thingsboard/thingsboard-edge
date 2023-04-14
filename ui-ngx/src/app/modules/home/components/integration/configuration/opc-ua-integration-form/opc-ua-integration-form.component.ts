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
import {
  IdentityType,
  IdentityTypeTranslation,
  OpcKeystoreType,
  OpcSecurityType,
  OpcUaIntegration
} from '@shared/models/integration.models';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { privateNetworkAddressValidator } from '@home/components/integration/integration.models';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OpcUaIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => OpcUaIntegrationFormComponent),
    multi: true,
  }]
})
export class OpcUaIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  opcIntegrationConfigForm: UntypedFormGroup;

  identityTypes = Object.values(IdentityType) as IdentityType[];
  IdentityType = IdentityType;
  IdentityTypeTranslation = IdentityTypeTranslation;
  OpcKeystoreType = Object.values(OpcKeystoreType);
  OpcSecurityType = OpcSecurityType;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();

    this.opcIntegrationConfigForm = this.fb.group({
      applicationName: '',
      applicationUri: '',
      host: ['localhost', Validators.required],
      port: [49320, [Validators.required, Validators.min(1), Validators.max(65535)]],
      scanPeriodInSeconds: [10, Validators.required],
      timeoutInMillis: [5000, Validators.required],
      security: [OpcSecurityType.None, Validators.required],
      identity: this.fb.group({
        password: [{value: '', disabled: true}, Validators.required],
        username: [{value: '', disabled: true}, Validators.required],
        type: [IdentityType.Anonymous, Validators.required]
      }),
      mapping: [null, Validators.required],
      keystore: this.fb.group({
        location: [{value: '', disabled: true}, Validators.required],
        type: [{value: OpcKeystoreType.JKS, disabled: true}, Validators.required],
        fileContent: [{value: '', disabled: true}, Validators.required],
        password: [{value: 'secret', disabled: true}, Validators.required],
        alias: [{value: 'opc-ua-extension', disabled: true}, Validators.required],
        keyPassword: [{value: 'secret', disabled: true}, Validators.required]
      })
    });

    this.opcIntegrationConfigForm.get('security').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateSecurityTypeValidation(type);
    });

    this.opcIntegrationConfigForm.get('identity.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      this.updateIdentityTypeValidation(type);
    });

    this.opcIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.opcIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: OpcUaIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.opcIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.opcIntegrationConfigForm.get('security').updateValueAndValidity({onlySelf: true});
        this.opcIntegrationConfigForm.get('identity.type').updateValueAndValidity({onlySelf: true});
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.opcIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.enable({emitEvent: false});
      this.opcIntegrationConfigForm.get('security').updateValueAndValidity({onlySelf: true});
      this.opcIntegrationConfigForm.get('identity.type').updateValueAndValidity({onlySelf: true});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  private updateSecurityTypeValidation(type: OpcSecurityType) {
    if (type === OpcSecurityType.None) {
      this.opcIntegrationConfigForm.get('keystore').disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.get('keystore').enable({emitEvent: false});
    }
  }

  private updateIdentityTypeValidation(type: IdentityType) {
    if (type === IdentityType.Anonymous) {
      this.opcIntegrationConfigForm.get('identity.username').disable({emitEvent: false});
      this.opcIntegrationConfigForm.get('identity.password').disable({emitEvent: false});
    } else {
      this.opcIntegrationConfigForm.get('identity.username').enable({emitEvent: false});
      this.opcIntegrationConfigForm.get('identity.password').enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.opcIntegrationConfigForm.valid ? null : {
      opcUaIntegrationConfigForm: {valid: false}
    };
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.opcIntegrationConfigForm.get('host').removeValidators(privateNetworkAddressValidator);
    } else {
      this.opcIntegrationConfigForm.get('host').addValidators(privateNetworkAddressValidator);
    }
    this.opcIntegrationConfigForm.get('host').updateValueAndValidity({emitEvent: false});
  }
}
