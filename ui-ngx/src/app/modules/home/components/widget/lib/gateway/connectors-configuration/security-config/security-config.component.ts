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

import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {
  SecurityType,
  SecurityTypeTranslationsMap,
  ModeType,
  noLeadTrailSpacesRegex,
  ConnectorSecurity
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-security-config',
  templateUrl: './security-config.component.html',
  styleUrls: ['./security-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SecurityConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports:[
    CommonModule,
    SharedModule,
  ]
})
export class SecurityConfigComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  title = 'gateway.security';

  @Input()
  @coerceBoolean()
  extendCertificatesModel = false;

  BrokerSecurityType = SecurityType;
  securityTypes = Object.values(SecurityType) as SecurityType[];
  modeTypes = Object.values(ModeType);
  SecurityTypeTranslationsMap = SecurityTypeTranslationsMap;
  securityFormGroup: UntypedFormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.securityFormGroup = this.fb.group({
      type: [SecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToCACert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToPrivateKey: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      pathToClientCert: ['', [Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    if (this.extendCertificatesModel) {
      this.securityFormGroup.addControl('mode', this.fb.control(ModeType.NONE, []));
    }
    this.securityFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
    this.securityFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type) => this.updateValidators(type));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(securityInfo: ConnectorSecurity): void {
    if (!securityInfo) {
      const defaultSecurity = {type: SecurityType.ANONYMOUS};
      this.securityFormGroup.reset(defaultSecurity, {emitEvent: false});
    } else {
      if (!securityInfo.type) {
        securityInfo.type = SecurityType.ANONYMOUS;
      }
      this.securityFormGroup.reset(securityInfo, {emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.get('type').value !== SecurityType.BASIC || this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  private updateValidators(type: SecurityType): void {
    if (type) {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
      this.securityFormGroup.get('pathToCACert').disable({emitEvent: false});
      this.securityFormGroup.get('pathToPrivateKey').disable({emitEvent: false});
      this.securityFormGroup.get('pathToClientCert').disable({emitEvent: false});
      this.securityFormGroup.get('mode')?.disable({emitEvent: false});
      if (type === SecurityType.BASIC) {
        this.securityFormGroup.get('username').enable({emitEvent: false});
        this.securityFormGroup.get('password').enable({emitEvent: false});
      } else if (type === SecurityType.CERTIFICATES) {
        this.securityFormGroup.get('pathToCACert').enable({emitEvent: false});
        this.securityFormGroup.get('pathToPrivateKey').enable({emitEvent: false});
        this.securityFormGroup.get('pathToClientCert').enable({emitEvent: false});
        if (this.extendCertificatesModel) {
          const modeControl = this.securityFormGroup.get('mode');
          if (modeControl && !modeControl.value) {
            modeControl.setValue(ModeType.NONE, {emitEvent: false});
          }

          modeControl?.enable({emitEvent: false});
          this.securityFormGroup.get('username').enable({emitEvent: false});
          this.securityFormGroup.get('password').enable({emitEvent: false});
        }
      }
    }
  }
}
