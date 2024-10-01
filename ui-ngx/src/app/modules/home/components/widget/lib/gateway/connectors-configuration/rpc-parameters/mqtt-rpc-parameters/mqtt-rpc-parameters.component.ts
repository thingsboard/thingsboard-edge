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
  OnDestroy,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator, Validators,
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import {
  integerRegex,
  noLeadTrailSpacesRegex,
  RPCTemplateConfigMQTT
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Component({
  selector: 'tb-mqtt-rpc-parameters',
  templateUrl: './mqtt-rpc-parameters.component.html',
  styleUrls: ['./mqtt-rpc-parameters.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttRpcParametersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttRpcParametersComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class MqttRpcParametersComponent implements ControlValueAccessor, Validator, OnDestroy {

  rpcParametersFormGroup: UntypedFormGroup;

  private onChange: (value: RPCTemplateConfigMQTT) => void = (_) => {};
  private onTouched: () => void = () => {};

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.rpcParametersFormGroup = this.fb.group({
      methodFilter: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      requestTopicExpression: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      responseTopicExpression: [{ value: null, disabled: true }, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      responseTimeout: [{ value: null, disabled: true }, [Validators.min(10), Validators.pattern(integerRegex)]],
      valueExpression: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      withResponse: [false, []],
    });

    this.observeValueChanges();
    this.observeWithResponse();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: RPCTemplateConfigMQTT) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.rpcParametersFormGroup.valid ? null : {
      rpcParametersFormGroup: { valid: false }
    };
  }

  writeValue(value: RPCTemplateConfigMQTT): void {
    this.rpcParametersFormGroup.patchValue(value, {emitEvent: false});
    this.toggleResponseFields(value.withResponse);
  }

  private observeValueChanges(): void {
    this.rpcParametersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });
  }

  private observeWithResponse(): void {
    this.rpcParametersFormGroup.get('withResponse').valueChanges.pipe(
      tap((isActive: boolean) => this.toggleResponseFields(isActive)),
      takeUntil(this.destroy$),
    ).subscribe();
  }

  private toggleResponseFields(enabled: boolean): void {
    const responseTopicControl = this.rpcParametersFormGroup.get('responseTopicExpression');
    const responseTimeoutControl = this.rpcParametersFormGroup.get('responseTimeout');
    if (enabled) {
      responseTopicControl.enable();
      responseTimeoutControl.enable();
    } else {
      responseTopicControl.disable();
      responseTimeoutControl.disable();
    }
  }
}
