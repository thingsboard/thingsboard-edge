///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectionStrategy, Component, forwardRef, Input, OnChanges, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  ModbusSecurity,
  noLeadTrailSpacesRegex,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-modbus-security-config',
  templateUrl: './modbus-security-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusSecurityConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusSecurityConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
  ]
})
export class ModbusSecurityConfigComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @Input() isMaster = false;
  @Input() disabled = false;

  securityConfigFormGroup: UntypedFormGroup;

  private onChange: (value: ModbusSecurity) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.securityConfigFormGroup = this.fb.group({
      certfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      keyfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      server_hostname: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
    });

    this.observeValueChanges();
  }

  ngOnChanges(): void {
    if (this.isMaster) {
      this.securityConfigFormGroup = this.fb.group({
        certfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        keyfile: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        reqclicert: [false, []],
      });
      this.observeValueChanges();
    }
    if (this.disabled) {
      this.securityConfigFormGroup.disable({emitEvent:false});
    } else {
      this.securityConfigFormGroup.enable({emitEvent:false});
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: ModbusSecurity) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.securityConfigFormGroup.valid || this.disabled ? null : {
      securityConfigFormGroup: { valid: false }
    };
  }

  writeValue(securityConfig: ModbusSecurity): void {
    const { certfile, password, keyfile, server_hostname } = securityConfig;
    let securityState = {
      certfile: certfile ?? '',
      password: password ?? '',
      keyfile: keyfile ?? '',
      server_hostname: server_hostname?? '',
      reqclicert: !!securityConfig.reqclicert,
    };
    if (this.isMaster) {
      securityState = { ...securityState, reqclicert: !!securityConfig.reqclicert };
    } else {
      securityState = { ...securityState, server_hostname: server_hostname ?? '' };
    }
    this.securityConfigFormGroup.reset(securityState, {emitEvent: false});
  }

  private observeValueChanges(): void {
    this.securityConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: ModbusSecurity) => {
      this.onChange(value);
      this.onTouched();
    });
  }
}
