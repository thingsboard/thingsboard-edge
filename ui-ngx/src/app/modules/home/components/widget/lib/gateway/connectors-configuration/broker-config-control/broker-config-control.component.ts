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
  BrokerConfig,
  MqttVersions,
  noLeadTrailSpacesRegex,
  PortLimits,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { generateSecret } from '@core/utils';
import { SecurityConfigComponent } from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-broker-config-control',
  templateUrl: './broker-config-control.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BrokerConfigControlComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => BrokerConfigControlComponent),
      multi: true
    }
  ]
})
export class BrokerConfigControlComponent implements ControlValueAccessor, Validator, OnDestroy {
  brokerConfigFormGroup: UntypedFormGroup;
  mqttVersions = MqttVersions;
  portLimits = PortLimits;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.brokerConfigFormGroup = this.fb.group({
      name: ['', []],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      version: [5, []],
      clientId: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      security: []
    });

    this.brokerConfigFormGroup.valueChanges.subscribe(value => {
      this.onChange(value);
      this.onTouched();
    });
  }

  get portErrorTooltip(): string {
    if (this.brokerConfigFormGroup.get('port').hasError('required')) {
      return this.translate.instant('gateway.port-required');
    } else if (
      this.brokerConfigFormGroup.get('port').hasError('min') ||
      this.brokerConfigFormGroup.get('port').hasError('max')
    ) {
      return this.translate.instant('gateway.port-limits-error',
        {min: PortLimits.MIN, max: PortLimits.MAX});
    }
    return '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  generate(formControlName: string): void {
    this.brokerConfigFormGroup.get(formControlName)?.patchValue('tb_gw_' + generateSecret(5));
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(brokerConfig: BrokerConfig): void {
    this.brokerConfigFormGroup.patchValue(brokerConfig, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.brokerConfigFormGroup.valid ? null : {
      brokerConfigFormGroup: {valid: false}
    };
  }
}
