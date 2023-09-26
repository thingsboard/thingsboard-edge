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
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  SnmpDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface OidMappingConfiguration {
  isAttribute: boolean;
  key: string;
  type: string;
  method: string;
  oid: string;
}

@Component({
  selector: 'tb-snmp-device-profile-transport-configuration',
  templateUrl: './snmp-device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
      multi: true
    }]
})
export class SnmpDeviceProfileTransportConfigurationComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  snmpDeviceProfileTransportConfigurationFormGroup: UntypedFormGroup;

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

  private propagateChange = (v: any) => {
  }

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.snmpDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      timeoutMs: [500, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      retries: [0, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      communicationConfigs: [null, Validators.required],
    });
    this.snmpDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.snmpDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SnmpDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: !value.communicationConfigs});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.snmpDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.snmpDeviceProfileTransportConfigurationFormGroup.getRawValue();
      configuration.type = DeviceTransportType.SNMP;
    }
    this.propagateChange(configuration);
  }

  validate(): ValidationErrors | null {
    return this.snmpDeviceProfileTransportConfigurationFormGroup.valid ? null : {
      snmpDeviceProfileTransportConfiguration: false
    };
  }
}
