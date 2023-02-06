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

import { Component, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
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
  BingingMode, BingingModeTranslationsMap,
  ServerSecurityConfig
} from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { Subject } from 'rxjs';
import { mergeMap, takeUntil, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';
import {
  Lwm2mPublicKeyOrIdTooltipTranslationsMap,
  Lwm2mSecurityType,
  Lwm2mSecurityTypeTranslationMap
} from '@shared/models/lwm2m-security-config.models';

@Component({
  selector: 'tb-profile-lwm2m-device-config-server',
  templateUrl: './lwm2m-device-config-server.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mDeviceConfigServerComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mDeviceConfigServerComponent),
      multi: true
    },
  ]
})

export class Lwm2mDeviceConfigServerComponent implements OnInit, ControlValueAccessor, Validator, OnDestroy {

  public disabled = false;
  private destroy$ = new Subject();

  private isDataLoadedIntoCache = false;

  serverFormGroup: UntypedFormGroup;
  bindingModeTypes = Object.values(BingingMode);
  bindingModeTypeNamesMap = BingingModeTranslationsMap;
  securityConfigLwM2MType = Lwm2mSecurityType;
  securityConfigLwM2MTypes = Object.keys(Lwm2mSecurityType);
  credentialTypeLwM2MNamesMap = Lwm2mSecurityTypeTranslationMap;
  publicKeyOrIdTooltipNamesMap = Lwm2mPublicKeyOrIdTooltipTranslationsMap;
  currentSecurityMode = null;
  bootstrapDisabled = false;

  @Output()
  removeServer = new EventEmitter();

  @Output()
  isTransportWasRunWithBootstrapChange = new EventEmitter<boolean>();

  private propagateChange = (v: any) => { };

  constructor(public fb: UntypedFormBuilder,
              private deviceProfileService: DeviceProfileService) {
  }

  ngOnInit(): void {
    this.serverFormGroup = this.fb.group({
      host: ['', Validators.required],
      port: ['', [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern('[0-9]*')]],
      securityMode: [Lwm2mSecurityType.NO_SEC],
      serverPublicKey: [''],
      clientHoldOffTime: ['', [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      shortServerId: ['', [Validators.required, Validators.min(1), Validators.max(65534), Validators.pattern('[0-9]*')]],
      bootstrapServerAccountTimeout: ['', [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      binding: [''],
      lifetime: [null, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      notifIfDisabled: [],
      defaultMinPeriod: [null, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      bootstrapServerIs: []
    });
    this.serverFormGroup.get('securityMode').valueChanges.pipe(
      tap((securityMode) => {
        this.currentSecurityMode = securityMode;
        this.updateValidate(securityMode);
      }),
      mergeMap(securityMode => this.getLwm2mBootstrapSecurityInfo(securityMode)),
      takeUntil(this.destroy$)
    ).subscribe(serverSecurityConfig => {
      if (this.currentSecurityMode !== Lwm2mSecurityType.NO_SEC) {
        this.changeSecurityHostPortFields(serverSecurityConfig);
      }
      this.serverFormGroup.patchValue(serverSecurityConfig, {emitEvent: false});
      if (this.currentSecurityMode === Lwm2mSecurityType.X509) {
        this.serverFormGroup.get('serverPublicKey').patchValue(serverSecurityConfig.serverCertificate, {emitEvent: false});
      }
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
      if (serverData.securityHost && serverData.securityPort) {
        delete serverData.securityHost;
        delete serverData.securityPort;
        this.propagateChangeState(this.serverFormGroup.getRawValue());
      }
    }
    if (!this.isDataLoadedIntoCache) {
      this.getLwm2mBootstrapSecurityInfo().subscribe(value => {
        if (!serverData) {
          this.serverFormGroup.patchValue(value);
        }
        if (!value && this.serverFormGroup.get('bootstrapServerIs').value === true) {
          this.isTransportWasRunWithBootstrapChange.emit(false);
          this.bootstrapDisabled = true;
          this.serverFormGroup.get('securityMode').disable({emitEvent: false});
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
      if (this.bootstrapDisabled) {
        this.serverFormGroup.get('securityMode').disable({emitEvent: false});
      }
    }
  }

  registerOnTouched(fn: any): void {
  }

  private updateValidate(securityMode: Lwm2mSecurityType): void {
    switch (securityMode) {
      case Lwm2mSecurityType.NO_SEC:
      case Lwm2mSecurityType.PSK:
        this.clearValidators();
        break;
      case Lwm2mSecurityType.RPK:
        this.setValidators();
        break;
      case Lwm2mSecurityType.X509:
        this.setValidators();
        break;
    }
    this.serverFormGroup.get('serverPublicKey').updateValueAndValidity({emitEvent: false});
  }

  private clearValidators(): void {
    this.serverFormGroup.get('serverPublicKey').clearValidators();
  }

  private setValidators(): void {
    this.serverFormGroup.get('serverPublicKey').setValidators([Validators.required]);
  }

  private propagateChangeState = (value: ServerSecurityConfig): void => {
    if (value !== undefined) {
      this.propagateChange(value);
    }
  }

  private getLwm2mBootstrapSecurityInfo(securityMode = Lwm2mSecurityType.NO_SEC): Observable<ServerSecurityConfig> {
    return this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(
      this.serverFormGroup.get('bootstrapServerIs').value, securityMode).pipe(
      tap(() => this.isDataLoadedIntoCache = true)
    );
  }

  private changeSecurityHostPortFields(serverData: ServerSecurityConfig): void {
    serverData.port = serverData.securityPort;
    serverData.host = serverData.securityHost;
  }

  validate(): ValidationErrors | null {
    return this.serverFormGroup.valid ? null : {
      serverFormGroup: true
    };
  }
}
