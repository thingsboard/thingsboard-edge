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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import {
  phoneNumberPatternTwilio,
  SmsProviderConfiguration,
  SmsProviderType,
  TwilioSmsProviderConfiguration
} from '@shared/models/settings.models';

@Component({
  selector: 'tb-twilio-sms-provider-configuration',
  templateUrl: './twilio-sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TwilioSmsProviderConfigurationComponent),
    multi: true
  }]
})
export class TwilioSmsProviderConfigurationComponent implements ControlValueAccessor, OnInit {

  twilioSmsProviderConfigurationFormGroup: UntypedFormGroup;

  phoneNumberPatternTwilio = phoneNumberPatternTwilio;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.twilioSmsProviderConfigurationFormGroup = this.fb.group({
      numberFrom: [null, [Validators.required, Validators.pattern(phoneNumberPatternTwilio)]],
      accountSid: [null, Validators.required],
      accountToken: [null, Validators.required]
    });
    this.twilioSmsProviderConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.twilioSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.twilioSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TwilioSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.twilioSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: TwilioSmsProviderConfiguration = null;
    if (this.twilioSmsProviderConfigurationFormGroup.valid) {
      configuration = this.twilioSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.TWILIO;
    }
    this.propagateChange(configuration);
  }
}
