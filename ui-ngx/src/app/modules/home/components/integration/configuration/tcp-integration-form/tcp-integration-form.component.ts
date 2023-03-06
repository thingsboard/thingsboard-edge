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

import { Component, forwardRef, Input } from '@angular/core';
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
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import {
  HandlerConfigurationType,
  HandlerConfigurationTypeTranslation,
  TcpBinaryByteOrder,
  TcpHandlerConfigurationType,
  TcpIntegration,
  TcpTextMessageSeparator
} from '@shared/models/integration.models';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-tcp-integration-form',
  templateUrl: './tcp-integration-form.component.html',
  styleUrls: ['./tcp-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TcpIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => TcpIntegrationFormComponent),
    multi: true,
  }]
})
export class TcpIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  @Input() isSetDownlink: boolean;

  tcpConfigForm: UntypedFormGroup;

  HandlerConfigurationType = TcpHandlerConfigurationType;
  HandlerConfigurationTypeTranslation = HandlerConfigurationTypeTranslation;

  TcpBinaryByteOrder = TcpBinaryByteOrder;
  TcpTextMessageSeparator = TcpTextMessageSeparator;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.tcpConfigForm = this.fb.group({
      port: [10560, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soBacklogOption: [128, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soRcvBuf: [64, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soSndBuf: [64, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soKeepaliveOption: [false],
      tcpNoDelay: [true],
      cacheSize: [1000, Validators.min(0)],
      timeToLiveInMinutes: [1440, [Validators.min(0), Validators.max(525600)]],
      handlerConfiguration: this.fb.group({
        handlerType: [HandlerConfigurationType.BINARY, Validators.required],
        byteOrder: [TcpBinaryByteOrder.LITTLE_ENDIAN],
        maxFrameLength: [128, [Validators.required, Validators.min(1), Validators.max(65535)]],
        lengthFieldOffset: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        lengthFieldLength: [2, [Validators.required, Validators.min(0), Validators.max(8)]],
        lengthAdjustment: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        initialBytesToStrip: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        failFast: [false],
        stripDelimiter: [{value: true, disabled: true}],
        messageSeparator: [{value: TcpTextMessageSeparator.SYSTEM_LINE_SEPARATOR, disabled: true}]
      })
    });
    this.tcpConfigForm.get('handlerConfiguration.handlerType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: TcpHandlerConfigurationType) => {
      this.updateHandleConfigurationField(value);
    });
    this.tcpConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.tcpConfigForm.getRawValue());
    });
  }

  writeValue(value: TcpIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.tcpConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.tcpConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: false});
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.tcpConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tcpConfigForm.disable({emitEvent: false});
    } else {
      this.tcpConfigForm.enable({emitEvent: false});
      this.tcpConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.tcpConfigForm.valid ? null : {
      updConfigForm: {valid: false}
    };
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  private updateHandleConfigurationField(type: TcpHandlerConfigurationType) {
    this.tcpConfigForm.get('handlerConfiguration').disable({emitEvent: false});
    switch (type) {
      case HandlerConfigurationType.BINARY:
        this.tcpConfigForm.get('handlerConfiguration.byteOrder').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.maxFrameLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthFieldOffset').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthFieldLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthAdjustment').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.initialBytesToStrip').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.failFast').enable({emitEvent: false});
        break;
      case HandlerConfigurationType.TEXT:
        this.tcpConfigForm.get('handlerConfiguration.maxFrameLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.stripDelimiter').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.messageSeparator').enable({emitEvent: false});
        break;
    }
    this.tcpConfigForm.get('handlerConfiguration.handlerType').enable({emitEvent: false});
  }
}
