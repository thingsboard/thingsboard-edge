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
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { IntegrationCredentialType, IntegrationCredentialTypeTranslation } from '@shared/models/integration.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

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
export class IntegrationCredentialsComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  integrationCredentialForm: FormGroup;
  hideSelectType = false;

  private allowCredentialTypesValue: IntegrationCredentialType[] = [];
  @Input()
  set allowCredentialTypes(types: IntegrationCredentialType[]) {
    this.allowCredentialTypesValue = types;
    this.hideSelectType = types.length === 1;
  }

  get allowCredentialTypes(): IntegrationCredentialType[] {
    return this.allowCredentialTypesValue;
  }

  private ignoreCaCertValue = false;
  @Input()
  set ignoreCaCert(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.ignoreCaCertValue !== newVal && this.integrationCredentialForm) {
      this.ignoreCaCertValue = newVal;
      if (newVal) {
        this.integrationCredentialForm.get('caCertFileName').clearValidators();
        this.integrationCredentialForm.get('caCert').clearValidators();
      } else {
        this.integrationCredentialForm.get('caCertFileName').setValidators(Validators.required);
        this.integrationCredentialForm.get('caCert').setValidators(Validators.required);
      }
      this.integrationCredentialForm.get('caCertFileName').updateValueAndValidity({emitEvent: false});
      this.integrationCredentialForm.get('caCert').updateValueAndValidity({emitEvent: false});
    }
  }

  get ignoreCaCert(): boolean {
    return this.ignoreCaCertValue;
  }

  @Input() userNameLabel = 'integration.username';
  @Input() userNameRequired = 'integration.username-required';
  @Input() passwordLabel = 'integration.password';
  @Input() passwordRequired = 'integration.password-required';
  private passwordOptionalValue = false;
  get passwordOptional(): boolean {
    return this.passwordOptionalValue;
  }
  @Input()
  set passwordOptional(value: boolean) {
    this.passwordOptionalValue = coerceBooleanProperty(value);
  }

  IntegrationCredentialTypeTranslation = IntegrationCredentialTypeTranslation;
  IntegrationCredentialType = IntegrationCredentialType;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.integrationCredentialForm = this.fb.group({
      type: ['', Validators.required],
      username: [{value: '', disabled: true}, Validators.required],
      password: [{value: '', disabled: true}, this.passwordOptional ? null : Validators.required],
      caCertFileName: [{value: '', disabled: true}, this.ignoreCaCert ? null : Validators.required],
      caCert: [{value: '', disabled: true},  this.ignoreCaCert ? null : Validators.required],
      certFileName: [{value: '', disabled: true}, Validators.required],
      cert: [{value: '', disabled: true}, Validators.required],
      privateKeyFileName: [{value: '', disabled: true}, Validators.required],
      privateKey: [{value: '', disabled: true}, Validators.required],
      privateKeyPassword: [{value: '', disabled: true}],
      token: [{value: '', disabled: true}, Validators.required],
      sasKey: [{value: '', disabled: true}, Validators.required],
    });
    this.integrationCredentialForm.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(type => this.updatedValidation(type));
    this.integrationCredentialForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value));
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
    if (this.disabled) {
      this.integrationCredentialForm.disable({emitEvent: false});
    } else {
      this.integrationCredentialForm.enable({emitEvent: false});
      this.integrationCredentialForm.get('type').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value) {
    this.integrationCredentialForm.patchValue(value, {emitEvent: false});
    if (!this.disabled) {
      this.integrationCredentialForm.get('type').updateValueAndValidity({onlySelf: true});
    }
  }

  private updatedValidation(type: IntegrationCredentialType) {
    this.integrationCredentialForm.disable({emitEvent: false});
    switch (type) {
      case IntegrationCredentialType.Anonymous:
        break;
      case IntegrationCredentialType.Basic:
        this.integrationCredentialForm.get('username').enable({emitEvent: false});
        this.integrationCredentialForm.get('password').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.CertPEM:
        this.integrationCredentialForm.get('caCertFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCert').enable({emitEvent: false});
        this.integrationCredentialForm.get('certFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('cert').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKey').enable({emitEvent: false});
        this.integrationCredentialForm.get('privateKeyPassword').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.Token:
        this.integrationCredentialForm.get('token').enable({emitEvent: false});
        break;
      case IntegrationCredentialType.SAS:
        this.integrationCredentialForm.get('sasKey').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCertFileName').enable({emitEvent: false});
        this.integrationCredentialForm.get('caCert').enable({emitEvent: false});
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
