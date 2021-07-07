///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, OnDestroy } from '@angular/core';
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
import {
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PRIVATE_KEY,
  LEN_MAX_PSK,
  LEN_MAX_PUBLIC_KEY_RPK,
  LEN_MAX_PUBLIC_KEY_X509,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap,
  ServerSecurityConfig
} from '@shared/models/lwm2m-security-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-security-config-lwm2m-server',
  templateUrl: './security-config-lwm2m-server.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigLwm2mServerComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SecurityConfigLwm2mServerComponent),
      multi: true
    }
  ]
})

export class SecurityConfigLwm2mServerComponent implements OnDestroy, ControlValueAccessor, Validator {

  serverFormGroup: FormGroup;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.values(Lwm2mSecurityType);
  lwm2mSecurityTypeTranslationMap = Lwm2mSecurityTypeTranslationMap;
  lenMinClientPublicKeyOrId = 0;
  lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
  lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => {};

  constructor(private fb: FormBuilder) {
    this.serverFormGroup = this.fb.group({
      securityMode: [Lwm2mSecurityType.NO_SEC],
      clientPublicKeyOrId: [''],
      clientSecretKey: ['']
    });
    this.serverFormGroup.get('securityMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((securityMode) => {
      this.updateValidate(securityMode);
    });

    this.serverFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(value);
    });
  }

  writeValue(value: any): void {
    if (value) {
      this.updateValueFields(value);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.serverFormGroup.disable({emitEvent: false});
    } else {
      this.serverFormGroup.enable({emitEvent: false});
    }
  }

  validate(control): ValidationErrors | null {
    return this.serverFormGroup.valid ? null : {
      securityConfig: {valid: false}
    };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateValueFields(serverData: ServerSecurityConfig): void {
    this.serverFormGroup.patchValue(serverData, {emitEvent: false});
    this.updateValidate(serverData.securityMode, true);
  }

  private updateValidate(securityMode: Lwm2mSecurityType, initValue = false): void {
    switch (securityMode) {
      case Lwm2mSecurityType.NO_SEC:
        this.serverFormGroup.get('clientPublicKeyOrId').clearValidators();
        this.serverFormGroup.get('clientSecretKey').clearValidators();
        this.serverFormGroup.get('clientPublicKeyOrId').disable({emitEvent: false});
        this.serverFormGroup.get('clientSecretKey').disable();
        break;
      case Lwm2mSecurityType.PSK:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lengthClientSecretKey = LEN_MAX_PSK;
        this.setValidatorsSecurity(securityMode);
        break;
      case Lwm2mSecurityType.RPK:
        this.lenMinClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
      case Lwm2mSecurityType.X509:
        this.lenMinClientPublicKeyOrId = 0;
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lengthClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.setValidatorsSecurity(securityMode);
        break;
    }
    this.serverFormGroup.get('clientPublicKeyOrId').updateValueAndValidity({emitEvent: false});
    this.serverFormGroup.get('clientSecretKey').updateValueAndValidity({emitEvent: !initValue});
  }

  private setValidatorsSecurity = (securityMode: Lwm2mSecurityType): void => {
    if (securityMode === Lwm2mSecurityType.PSK) {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
    } else {
      this.serverFormGroup.get('clientPublicKeyOrId').setValidators([
        Validators.required,
        Validators.pattern(KEY_REGEXP_HEX_DEC),
        Validators.minLength(this.lenMinClientPublicKeyOrId),
        Validators.maxLength(this.lenMaxClientPublicKeyOrId)
      ]);
    }

    this.serverFormGroup.get('clientSecretKey').setValidators([
      Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.minLength(this.lengthClientSecretKey),
      Validators.maxLength(this.lengthClientSecretKey)
    ]);

    this.serverFormGroup.get('clientPublicKeyOrId').enable({emitEvent: false});
    this.serverFormGroup.get('clientSecretKey').enable();
  }
}
