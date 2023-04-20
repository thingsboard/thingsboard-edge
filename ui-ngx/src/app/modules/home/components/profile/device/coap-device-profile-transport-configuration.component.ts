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
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  CoapDeviceProfileTransportConfiguration,
  coapDeviceTypeTranslationMap,
  CoapTransportDeviceType,
  defaultAttributesSchema,
  defaultRpcRequestSchema,
  defaultRpcResponseSchema,
  defaultTelemetrySchema,
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  TransportPayloadType,
  transportPayloadTypeTranslationMap,
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PowerMode } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';

@Component({
  selector: 'tb-coap-device-profile-transport-configuration',
  templateUrl: './coap-device-profile-transport-configuration.component.html',
  styleUrls: ['./coap-device-profile-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CoapDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class CoapDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, OnDestroy {

  coapTransportDeviceTypes = Object.values(CoapTransportDeviceType);
  coapTransportDeviceTypeTranslations = coapDeviceTypeTranslationMap;

  transportPayloadTypes = Object.values(TransportPayloadType);
  transportPayloadTypeTranslations = transportPayloadTypeTranslationMap;

  coapTransportConfigurationFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private requiredValue: boolean;

  private transportPayloadTypeConfiguration = this.fb.group({
    transportPayloadType: [TransportPayloadType.JSON, Validators.required],
    deviceTelemetryProtoSchema: [defaultTelemetrySchema, Validators.required],
    deviceAttributesProtoSchema: [defaultAttributesSchema, Validators.required],
    deviceRpcRequestProtoSchema: [defaultRpcRequestSchema, Validators.required],
    deviceRpcResponseProtoSchema: [defaultRpcResponseSchema, Validators.required]
  });

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
    this.coapTransportConfigurationFormGroup = this.fb.group({
      coapDeviceTypeConfiguration: this.fb.group({
        coapDeviceType: [CoapTransportDeviceType.DEFAULT, Validators.required],
        transportPayloadTypeConfiguration: this.transportPayloadTypeConfiguration,
      }),
      clientSettings: this.fb.group({
        powerMode: [PowerMode.DRX, Validators.required],
        edrxCycle: [{disabled: true, value: 0}, Validators.required],
        psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
        pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required]
      })}
    );
    this.coapTransportConfigurationFormGroup.get('coapDeviceTypeConfiguration.coapDeviceType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(coapDeviceType => {
      this.updateCoapDeviceTypeBasedControls(coapDeviceType, true);
    });
    this.coapTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get coapDeviceTypeDefault(): boolean {
    const coapDeviceType = this.coapTransportConfigurationFormGroup.get('coapDeviceTypeConfiguration.coapDeviceType').value;
    return coapDeviceType === CoapTransportDeviceType.DEFAULT;
  }

  get protoPayloadType(): boolean {
    const transportPayloadTypePath = 'coapDeviceTypeConfiguration.transportPayloadTypeConfiguration.transportPayloadType';
    const transportPayloadType = this.coapTransportConfigurationFormGroup.get(transportPayloadTypePath).value;
    return transportPayloadType === TransportPayloadType.PROTOBUF;
  }

  get clientSettingsFormGroup(): UntypedFormGroup {
    return this.coapTransportConfigurationFormGroup.get('clientSettings') as UntypedFormGroup;
  }

  private updateCoapDeviceTypeBasedControls(type: CoapTransportDeviceType, forceUpdated = false) {
    const coapDeviceTypeConfigurationFormGroup = this.coapTransportConfigurationFormGroup
      .get('coapDeviceTypeConfiguration') as UntypedFormGroup;
    if (forceUpdated) {
      coapDeviceTypeConfigurationFormGroup.patchValue({
        transportPayloadTypeConfiguration: this.transportPayloadTypeConfiguration
      }, {emitEvent: false});
    }
    if (type === CoapTransportDeviceType.DEFAULT && !this.disabled) {
      coapDeviceTypeConfigurationFormGroup.get('transportPayloadTypeConfiguration').enable({emitEvent: false});
    } else {
      coapDeviceTypeConfigurationFormGroup.get('transportPayloadTypeConfiguration').disable({emitEvent: false});
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.coapTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.coapTransportConfigurationFormGroup.enable({emitEvent: false});
      this.coapTransportConfigurationFormGroup.get('clientSettings.powerMode').updateValueAndValidity({onlySelf: true});
    }
  }

  writeValue(value: CoapDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      if (!value.clientSettings) {
        value.clientSettings = {
          powerMode: PowerMode.DRX
        };
      }
      this.coapTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
      if (!this.disabled) {
        this.coapTransportConfigurationFormGroup.get('clientSettings.powerMode').updateValueAndValidity({onlySelf: true});
      }
      this.updateCoapDeviceTypeBasedControls(value.coapDeviceTypeConfiguration?.coapDeviceType);
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.coapTransportConfigurationFormGroup.valid) {
      configuration = this.coapTransportConfigurationFormGroup.value;
      configuration.type = DeviceTransportType.COAP;
    }
    this.propagateChange(configuration);
  }
}
