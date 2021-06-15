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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  DEFAULT_PORT_BOOTSTRAP_NO_SEC,
  DEFAULT_PORT_SERVER_NO_SEC,
  KEY_REGEXP_HEX_DEC,
  LEN_MAX_PUBLIC_KEY_RPK,
  LEN_MAX_PUBLIC_KEY_X509,
  securityConfigMode,
  securityConfigModeNames,
  ServerSecurityConfig
} from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { of, Subject } from 'rxjs';
import { map, mergeMap, takeUntil, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-profile-lwm2m-device-config-server',
  templateUrl: './lwm2m-device-config-server.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mDeviceConfigServerComponent),
      multi: true
    }
  ]
})

export class Lwm2mDeviceConfigServerComponent implements OnInit, ControlValueAccessor, OnDestroy {

  private disabled = false;
  private destroy$ = new Subject();

  private securityDefaultConfig: ServerSecurityConfig;

  serverFormGroup: FormGroup;
  securityConfigLwM2MType = securityConfigMode;
  securityConfigLwM2MTypes = Object.keys(securityConfigMode);
  credentialTypeLwM2MNamesMap = securityConfigModeNames;
  maxLengthPublicKey = LEN_MAX_PUBLIC_KEY_RPK;
  currentSecurityMode = null;

  @Input()
  isBootstrapServer = false;

  private propagateChange = (v: any) => { };

  constructor(public fb: FormBuilder,
              private deviceProfileService: DeviceProfileService) {
  }

  ngOnInit(): void {
    this.serverFormGroup = this.fb.group({
      host: ['', Validators.required],
      port: [this.isBootstrapServer ? DEFAULT_PORT_BOOTSTRAP_NO_SEC : DEFAULT_PORT_SERVER_NO_SEC, [Validators.required, Validators.min(0)]],
      securityMode: [securityConfigMode.NO_SEC],
      serverPublicKey: ['', Validators.required],
      clientHoldOffTime: ['', [Validators.required, Validators.min(0)]],
      serverId: ['', [Validators.required, Validators.min(0)]],
      bootstrapServerAccountTimeout: ['', [Validators.required, Validators.min(0)]],
    });
    this.serverFormGroup.get('securityMode').valueChanges.pipe(
      tap(securityMode => this.updateValidate(securityMode)),
      mergeMap(securityMode => this.getLwm2mBootstrapSecurityInfo(securityMode)),
      takeUntil(this.destroy$)
    ).subscribe(serverSecurityConfig => {
      this.serverFormGroup.patchValue(serverSecurityConfig, {emitEvent: false});
    });
    this.serverFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.propagateChangeState(value);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(serverData: ServerSecurityConfig): void {
    if (serverData) {
      this.serverFormGroup.patchValue(serverData, {emitEvent: false});
      this.updateValidate(serverData.securityMode);
    }
    if (!this.securityDefaultConfig){
      this.getLwm2mBootstrapSecurityInfo().subscribe(value => {
        if (!serverData) {
          this.serverFormGroup.patchValue(value);
        }
      });
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.serverFormGroup.disable({emitEvent: false});
    } else {
      this.serverFormGroup.enable({emitEvent: false});
    }
  }

  registerOnTouched(fn: any): void {
  }

  private updateValidate(securityMode: securityConfigMode): void {
    switch (securityMode) {
      case securityConfigMode.NO_SEC:
      case securityConfigMode.PSK:
        this.clearValidators();
        break;
      case securityConfigMode.RPK:
        this.maxLengthPublicKey = LEN_MAX_PUBLIC_KEY_RPK;
        this.setValidators(LEN_MAX_PUBLIC_KEY_RPK);
        break;
      case securityConfigMode.X509:
        this.maxLengthPublicKey = LEN_MAX_PUBLIC_KEY_X509;
        this.setValidators(0);
        break;
    }
    this.serverFormGroup.get('serverPublicKey').updateValueAndValidity({emitEvent: false});
  }

  private clearValidators(): void {
    this.serverFormGroup.get('serverPublicKey').clearValidators();
  }

  private setValidators(minLengthKey: number): void {
    this.serverFormGroup.get('serverPublicKey').setValidators([
      Validators.required,
      Validators.pattern(KEY_REGEXP_HEX_DEC),
      Validators.minLength(minLengthKey),
      Validators.maxLength(this.maxLengthPublicKey)
    ]);
  }

  private propagateChangeState = (value: ServerSecurityConfig): void => {
    if (value !== undefined) {
      if (this.serverFormGroup.valid) {
        this.propagateChange(value);
      } else {
        this.propagateChange(null);
      }
    }
  }

  private getLwm2mBootstrapSecurityInfo(securityMode = securityConfigMode.NO_SEC): Observable<ServerSecurityConfig> {
    if (this.securityDefaultConfig) {
      return of(this.processingBootstrapSecurityInfo(this.securityDefaultConfig, securityMode));
    }
    return this.deviceProfileService.getLwm2mBootstrapSecurityInfo(this.isBootstrapServer).pipe(
      map(securityInfo => {
        this.securityDefaultConfig = securityInfo;
        return this.processingBootstrapSecurityInfo(securityInfo, securityMode);
      })
    );
  }

  private processingBootstrapSecurityInfo(securityConfig: ServerSecurityConfig, securityMode: securityConfigMode): ServerSecurityConfig {
    const config = deepClone(securityConfig);
    switch (securityMode) {
      case securityConfigMode.PSK:
        config.port = config.securityPort;
        config.host = config.securityHost;
        config.serverPublicKey = '';
        break;
      case securityConfigMode.RPK:
      case securityConfigMode.X509:
        config.port = config.securityPort;
        config.host = config.securityHost;
        break;
      case securityConfigMode.NO_SEC:
        config.serverPublicKey = '';
        break;
    }
    return config;
  }
}
