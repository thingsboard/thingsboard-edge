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

import { ChangeDetectionStrategy, Component, forwardRef, OnDestroy } from '@angular/core';
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
  noLeadTrailSpacesRegex,
  SecurityPolicy,
  SecurityPolicyTypes,
  ServerConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-server-config',
  templateUrl: './server-config.component.html',
  styleUrls: ['./server-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ServerConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ServerConfigComponent),
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
export class ServerConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  securityPolicyTypes = SecurityPolicyTypes;
  serverConfigFormGroup: UntypedFormGroup;

  onChange!: (value: string) => void;
  onTouched!: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.serverConfigFormGroup = this.fb.group({
      name: ['', []],
      url: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      timeoutInMillis: [1000, [Validators.required, Validators.min(1000)]],
      scanPeriodInMillis: [1000, [Validators.required, Validators.min(1000)]],
      enableSubscriptions: [true, []],
      subCheckPeriodInMillis: [10, [Validators.required, Validators.min(10)]],
      showMap: [false, []],
      security: [SecurityPolicy.BASIC128, []],
      identity: [{}, [Validators.required]]
    });

    this.serverConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.serverConfigFormGroup.valid ? null : {
      serverConfigFormGroup: { valid: false }
    };
  }

  writeValue(serverConfig: ServerConfig): void {
    this.serverConfigFormGroup.patchValue(serverConfig, {emitEvent: false});
  }
}
