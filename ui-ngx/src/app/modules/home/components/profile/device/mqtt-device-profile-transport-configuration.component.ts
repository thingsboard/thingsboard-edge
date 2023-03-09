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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  defaultAttributesSchema,
  defaultRpcRequestSchema,
  defaultRpcResponseSchema,
  defaultTelemetrySchema,
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  MqttDeviceProfileTransportConfiguration,
  TransportPayloadType,
  transportPayloadTypeTranslationMap
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';

@Component({
  selector: 'tb-mqtt-device-profile-transport-configuration',
  templateUrl: './mqtt-device-profile-transport-configuration.component.html',
  styleUrls: ['./mqtt-device-profile-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MqttDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class MqttDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  transportPayloadTypes = Object.keys(TransportPayloadType);

  transportPayloadTypeTranslations = transportPayloadTypeTranslationMap;

  mqttDeviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
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

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.mqttDeviceProfileTransportConfigurationFormGroup = this.fb.group({
        deviceAttributesTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        deviceTelemetryTopic: [null, [Validators.required, this.validationMQTTTopic()]],
        sparkplug: [false],
        sparkplugAttributesMetricNames: [null],
        sendAckOnValidationException: [false, Validators.required],
        transportPayloadTypeConfiguration: this.fb.group({
          transportPayloadType: [TransportPayloadType.JSON, Validators.required],
          deviceTelemetryProtoSchema: [defaultTelemetrySchema, Validators.required],
          deviceAttributesProtoSchema: [defaultAttributesSchema, Validators.required],
          deviceRpcRequestProtoSchema: [defaultRpcRequestSchema, Validators.required],
          deviceRpcResponseProtoSchema: [defaultRpcResponseSchema, Validators.required],
          enableCompatibilityWithJsonPayloadFormat: [false, Validators.required],
          useJsonPayloadFormatForDefaultDownlinkTopics: [false, Validators.required]
        })
      }, {validators: this.uniqueDeviceTopicValidator}
    );
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.transportPayloadType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(payloadType => {
      this.updateTransportPayloadBasedControls(payloadType, true);
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.enableCompatibilityWithJsonPayloadFormat')
      .valueChanges.pipe(takeUntil(this.destroy$)
    ).subscribe(compatibilityWithJsonPayloadFormatEnabled => {
      if (!compatibilityWithJsonPayloadFormatEnabled) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.useJsonPayloadFormatForDefaultDownlinkTopics')
          .patchValue(false, {emitEvent: false});
      }
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').enable({emitEvent: false});
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').enable({emitEvent: false});
      } else {
        this.mqttDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
      }
    });
    this.mqttDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.mqttDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
      this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').updateValueAndValidity({onlySelf: true});
    }
  }

  get protoPayloadType(): boolean {
    const transportPayloadType = this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.transportPayloadType').value;
    return transportPayloadType === TransportPayloadType.PROTOBUF;
  }

  get compatibilityWithJsonPayloadFormatEnabled(): boolean {
    return this.mqttDeviceProfileTransportConfigurationFormGroup.get('transportPayloadTypeConfiguration.enableCompatibilityWithJsonPayloadFormat').value;
  }

  writeValue(value: MqttDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.mqttDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
      this.updateTransportPayloadBasedControls(value.transportPayloadTypeConfiguration?.transportPayloadType);
      if (!this.disabled) {
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplug').updateValueAndValidity({onlySelf: true});
      }
    }
  }

  removeAttributeMetricName(name: string): void {
    const names: string[] = this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').value;
    const index = names.indexOf(name);
    if (index >= 0) {
      names.splice(index, 1);
      this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').setValue(names);
    }
  }

  addAttributeMetricName(event: MatChipInputEvent): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let names: string[] = this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').value;
      if (!names || names.indexOf(value) === -1) {
        if (!names) {
          names = [];
        }
        names.push(value);
        this.mqttDeviceProfileTransportConfigurationFormGroup.get('sparkplugAttributesMetricNames').setValue(names, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.mqttDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.mqttDeviceProfileTransportConfigurationFormGroup.getRawValue();
      configuration.type = DeviceTransportType.MQTT;
    }
    this.propagateChange(configuration);
  }

  private updateTransportPayloadBasedControls(type: TransportPayloadType, forceUpdated = false) {
    const transportPayloadTypeForm = this.mqttDeviceProfileTransportConfigurationFormGroup
      .get('transportPayloadTypeConfiguration') as UntypedFormGroup;
    if (forceUpdated) {
      transportPayloadTypeForm.patchValue({
        deviceTelemetryProtoSchema: defaultTelemetrySchema,
        deviceAttributesProtoSchema: defaultAttributesSchema,
        deviceRpcRequestProtoSchema: defaultRpcRequestSchema,
        deviceRpcResponseProtoSchema: defaultRpcResponseSchema,
        enableCompatibilityWithJsonPayloadFormat: false,
        useJsonPayloadFormatForDefaultDownlinkTopics: false
      }, {emitEvent: false});
    }
    if (type === TransportPayloadType.PROTOBUF && !this.disabled) {
      transportPayloadTypeForm.get('deviceTelemetryProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceAttributesProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcRequestProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcResponseProtoSchema').enable({emitEvent: false});
      transportPayloadTypeForm.get('enableCompatibilityWithJsonPayloadFormat').enable({emitEvent: false});
      transportPayloadTypeForm.get('useJsonPayloadFormatForDefaultDownlinkTopics').enable({emitEvent: false});
    } else {
      transportPayloadTypeForm.get('deviceTelemetryProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceAttributesProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcRequestProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('deviceRpcResponseProtoSchema').disable({emitEvent: false});
      transportPayloadTypeForm.get('enableCompatibilityWithJsonPayloadFormat').disable({emitEvent: false});
      transportPayloadTypeForm.get('useJsonPayloadFormatForDefaultDownlinkTopics').disable({emitEvent: false});
    }
  }

  private validationMQTTTopic(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newTopic = c.value;
      const wildcardSymbols = /[#+]/g;
      let findSymbol = wildcardSymbols.exec(newTopic);
      while (findSymbol) {
        const index = findSymbol.index;
        const currentSymbol = findSymbol[0];
        const prevSymbol = index > 0 ? newTopic[index - 1] : null;
        const nextSymbol = index < (newTopic.length - 1) ? newTopic[index + 1] : null;
        if (currentSymbol === '#' && (index !== (newTopic.length - 1) || (prevSymbol !== null && prevSymbol !== '/'))) {
          return {
            invalidMultiTopicCharacter: {
              valid: false
            }
          };
        }
        if (currentSymbol === '+' && ((prevSymbol !== null && prevSymbol !== '/') || (nextSymbol !== null && nextSymbol !== '/'))) {
          return {
            invalidSingleTopicCharacter: {
              valid: false
            }
          };
        }
        findSymbol = wildcardSymbols.exec(newTopic);
      }
      return null;
    };
  }

  private uniqueDeviceTopicValidator(control: UntypedFormGroup): { [key: string]: boolean } | null {
    if (control.getRawValue()) {
      const formValue = control.getRawValue() as MqttDeviceProfileTransportConfiguration;
      if (formValue.deviceAttributesTopic === formValue.deviceTelemetryTopic) {
        return {unique: true};
      }
    }
    return null;
  }
}
