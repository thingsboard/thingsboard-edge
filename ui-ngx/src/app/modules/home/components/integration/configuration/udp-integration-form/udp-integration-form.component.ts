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
  UpdIntegration
} from '@shared/models/integration.models';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-udp-integration-form',
  templateUrl: './udp-integration-form.component.html',
  styleUrls: ['./udp-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UdpIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => UdpIntegrationFormComponent),
    multi: true,
  }]
})
export class UdpIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  @Input() isSetDownlink: boolean;

  updConfigForm: UntypedFormGroup;

  HandlerConfigurationType = HandlerConfigurationType;
  HandlerConfigurationTypeTranslation = HandlerConfigurationTypeTranslation;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.updConfigForm = this.fb.group({
      port: [11560, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soBroadcast: [true],
      soRcvBuf: [64, [Validators.required, Validators.min(1), Validators.max(65535)]],
      cacheSize: [1000, Validators.min(0)],
      timeToLiveInMinutes: [1440, [Validators.min(0), Validators.max(525600)]],
      handlerConfiguration: this.fb.group({
        handlerType: [HandlerConfigurationType.BINARY, Validators.required],
        charsetName: [{value: 'UTF-8', disabled: true}, Validators.required],
        maxFrameLength: [{value: 128, disabled: true}, [Validators.required, Validators.min(1), Validators.max(65535)]]
      })
    });
    this.updConfigForm.get('handlerConfiguration.handlerType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: HandlerConfigurationType) => {
      this.updateHandleConfigurationField(value);
    });
    this.updConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.updConfigForm.getRawValue());
    });
  }

  writeValue(value: UpdIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.updConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.updConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: false});
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
        this.updateModels(this.updConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.updConfigForm.disable({emitEvent: false});
    } else {
      this.updConfigForm.enable({emitEvent: false});
      this.updConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.updConfigForm.valid ? null : {
      updConfigForm: {valid: false}
    };
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  private updateHandleConfigurationField(type: HandlerConfigurationType) {
    this.updConfigForm.get('handlerConfiguration').disable({emitEvent: false});
    switch (type) {
      case HandlerConfigurationType.HEX:
        this.updConfigForm.get('handlerConfiguration.maxFrameLength').enable({emitEvent: false});
        break;
      case HandlerConfigurationType.TEXT:
        this.updConfigForm.get('handlerConfiguration.charsetName').enable({emitEvent: false});
        break;
    }
    this.updConfigForm.get('handlerConfiguration.handlerType').enable({emitEvent: false});
  }
}
