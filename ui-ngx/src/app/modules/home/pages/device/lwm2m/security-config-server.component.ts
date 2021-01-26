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

import {Component, forwardRef, Inject, Input, OnInit, ViewChild} from '@angular/core';

import {
  ControlValueAccessor,
  FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators
} from '@angular/forms';
import {
  SECURITY_CONFIG_MODE,
  SECURITY_CONFIG_MODE_NAMES,
  KEY_IDENT_REGEXP_PSK,
  ServerSecurityConfig,
  DeviceCredentialsDialogLwm2mData,
  LEN_MAX_PSK,
  LEN_MAX_PRIVATE_KEY, LEN_MAX_PUBLIC_KEY_RPK, KEY_PRIVATE_REGEXP, LEN_MAX_PUBLIC_KEY_X509, KEY_PUBLIC_REGEXP_X509
} from '@home/pages/device/lwm2m/security-config.models';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PageComponent} from '@shared/components/page.component';
import {MatPaginator} from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-security-config-server-lwm2m',
  templateUrl: './security-config-server.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigServerComponent),
      multi: true
    }
  ]
})

export class SecurityConfigServerComponent extends PageComponent implements OnInit, ControlValueAccessor {

  securityConfigLwM2MType = SECURITY_CONFIG_MODE;
  securityConfigLwM2MTypes = Object.keys(SECURITY_CONFIG_MODE);
  credentialTypeLwM2MNamesMap = SECURITY_CONFIG_MODE_NAMES;
  lenMaxClientPublicKeyOrId = LEN_MAX_PSK;
  lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;

  @Input() serverFormGroup: FormGroup;

  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigServerComponent, object>,
              public translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.registerDisableOnLoadFormControl(this.serverFormGroup.get('securityMode'));
  }

  updateValueFields(serverData: ServerSecurityConfig): void {
    this.serverFormGroup.patchValue(serverData, {emitEvent: false});
    const securityMode = this.serverFormGroup.get('securityMode').value as SECURITY_CONFIG_MODE;
    this.updateValidate(securityMode);
  }

  updateValidate(securityMode: SECURITY_CONFIG_MODE): void {
    switch (securityMode) {
      case SECURITY_CONFIG_MODE.NO_SEC:
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([]);
        this.serverFormGroup.get('clientSecretKey').setValidators([]);
        break;
      case SECURITY_CONFIG_MODE.PSK:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_RPK;
        this.lenMaxClientSecretKey = LEN_MAX_PSK;
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required]);
        this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required, Validators.pattern(KEY_IDENT_REGEXP_PSK)]);
        break;
      case SECURITY_CONFIG_MODE.RPK:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP_X509)]);
        this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required, Validators.pattern(KEY_PRIVATE_REGEXP)]);
        break;
      case SECURITY_CONFIG_MODE.X509:
        this.lenMaxClientPublicKeyOrId = LEN_MAX_PUBLIC_KEY_X509;
        this.lenMaxClientSecretKey = LEN_MAX_PRIVATE_KEY;
        this.serverFormGroup.get('clientPublicKeyOrId').setValidators([Validators.required, Validators.pattern(KEY_PUBLIC_REGEXP_X509)]);
        this.serverFormGroup.get('clientSecretKey').setValidators([Validators.required, Validators.pattern(KEY_PRIVATE_REGEXP)]);
        break;
    }
    this.serverFormGroup.updateValueAndValidity();
  }

  securityModeChanged(securityMode: SECURITY_CONFIG_MODE): void {
    this.updateValidate(securityMode);
  }

  writeValue(value: any): void {
    if (value) {
      this.updateValueFields(value);
    }
  }

  registerOnChange(fn: (value: any) => any): void {
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
  }
}
