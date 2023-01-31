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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { deepClone } from '@core/utils';
import {
  createSmsProviderConfiguration,
  SmsProviderConfiguration, smsProviderConfigurationValidator,
  SmsProviderType,
  smsProviderTypeTranslationMap
} from '@shared/models/settings.models';

@Component({
  selector: 'tb-sms-provider-configuration',
  templateUrl: './sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SmsProviderConfigurationComponent),
    multi: true
  }]
})
export class SmsProviderConfigurationComponent implements ControlValueAccessor, OnInit {

  smsProviderType = SmsProviderType;
  smsProviderTypes = Object.keys(SmsProviderType);
  smsProviderTypeTranslations = smsProviderTypeTranslationMap;

  smsProviderConfigurationFormGroup: FormGroup;

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
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.smsProviderConfigurationFormGroup = this.fb.group({
      type: [null, Validators.required],
      configuration: [null, smsProviderConfigurationValidator(true)]
    });
    this.smsProviderConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.smsProviderConfigurationFormGroup.get('type').valueChanges.subscribe(() => {
      this.smsProviderTypeChanged();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.smsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.smsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SmsProviderConfiguration | null): void {
    const configuration = deepClone(value);
    const type = configuration?.type;
    if (configuration) {
      delete configuration.type;
    }
    this.smsProviderConfigurationFormGroup.patchValue({type}, {emitEvent: false});
    this.smsProviderConfigurationFormGroup.patchValue({configuration}, {emitEvent: false});
  }

  private smsProviderTypeChanged() {
    const type: SmsProviderType = this.smsProviderConfigurationFormGroup.get('type').value;
    this.smsProviderConfigurationFormGroup.patchValue({configuration: createSmsProviderConfiguration(type)}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: SmsProviderConfiguration = null;
    if (this.smsProviderConfigurationFormGroup.valid) {
      configuration = this.smsProviderConfigurationFormGroup.getRawValue().configuration;
      configuration.type = this.smsProviderConfigurationFormGroup.getRawValue().type;
    }
    this.propagateChange(configuration);
  }
}
