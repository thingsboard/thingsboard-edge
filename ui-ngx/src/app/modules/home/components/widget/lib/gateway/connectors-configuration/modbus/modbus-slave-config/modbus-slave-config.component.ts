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
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import {
  ModbusMethodLabelsMap,
  ModbusMethodType,
  ModbusOrderType,
  ModbusProtocolLabelsMap,
  ModbusProtocolType,
  ModbusRegisterValues,
  ModbusSlave,
  noLeadTrailSpacesRegex,
  PortLimits,
  SlaveConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GatewayPortTooltipPipe } from '@home/pipes/public-api';
import { ModbusSecurityConfigComponent } from '../modbus-security-config/modbus-security-config.component';
import { ModbusValuesComponent, } from '../modbus-values/modbus-values.component';

@Component({
  selector: 'tb-modbus-server-config',
  templateUrl: './modbus-slave-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusSlaveConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusSlaveConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusValuesComponent,
    ModbusSecurityConfigComponent,
    GatewayPortTooltipPipe,
  ],
  styles: [`
    :host {
      .nested-expansion-header {
        .mat-content {
          height: 100%;
        }
      }
    }
  `],
})
export class ModbusSlaveConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  slaveConfigFormGroup: UntypedFormGroup;
  showSecurityControl: UntypedFormControl;
  ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;
  ModbusMethodLabelsMap = ModbusMethodLabelsMap;
  portLimits = PortLimits;

  readonly modbusProtocolTypes = Object.values(ModbusProtocolType);
  readonly modbusMethodTypes = Object.values(ModbusMethodType);
  readonly modbusOrderType = Object.values(ModbusOrderType);
  readonly ModbusProtocolType = ModbusProtocolType;
  readonly serialSpecificControlKeys = ['serialPort', 'baudrate'];
  readonly tcpUdpSpecificControlKeys = ['port', 'security', 'host'];

  private onChange: (value: SlaveConfig) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.showSecurityControl = this.fb.control(false);
    this.slaveConfigFormGroup = this.fb.group({
      type: [ModbusProtocolType.TCP, []],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      serialPort: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      method: [ModbusMethodType.SOCKET, []],
      unitId: [null, [Validators.required]],
      baudrate: [null, []],
      deviceName: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      pollPeriod: [null, []],
      sendDataToThingsBoard: [false, []],
      byteOrder:[ModbusOrderType.BIG, []],
      security: [],
      identity: this.fb.group({
        vendorName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        productCode: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        vendorUrl: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        productName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        modelName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      }),
      values: [],
    });

    this.slaveConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: SlaveConfig) => {
      if (value.type === ModbusProtocolType.Serial) {
        value.port = value.serialPort;
        delete value.serialPort;
      }
      this.onChange(value);
      this.onTouched();
    });

    this.observeTypeChange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: SlaveConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.slaveConfigFormGroup.valid ? null : {
      serverConfigFormGroup: { valid: false }
    };
  }

  writeValue(slaveConfig: ModbusSlave): void {
    this.showSecurityControl.patchValue(!!slaveConfig.security);
    this.updateSlaveConfig(slaveConfig);
    this.updateControlsEnabling(slaveConfig.type);
  }

  private observeTypeChange(): void {
    this.slaveConfigFormGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(type => {
      this.updateControlsEnabling(type);
    });
  }

  private updateControlsEnabling(type: ModbusProtocolType): void {
    if (type === ModbusProtocolType.Serial) {
      this.serialSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({emitEvent: false}));
      this.tcpUdpSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({emitEvent: false}));
    } else {
      this.serialSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({emitEvent: false}));
      this.tcpUdpSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({emitEvent: false}));
    }
  };

  private updateSlaveConfig(slaveConfig: ModbusSlave): void {
    const {
      type,
      method,
      unitId,
      deviceName,
      deviceType,
      pollPeriod,
      sendDataToThingsBoard,
      byteOrder,
      security,
      identity,
      values,
      baudrate,
      host,
      port,
    } = slaveConfig;
    let slaveState: ModbusSlave = {
      host: host ?? '',
      type: type ?? ModbusProtocolType.TCP,
      method: method ?? ModbusMethodType.SOCKET,
      unitId: unitId ?? null,
      deviceName: deviceName ?? '',
      deviceType: deviceType ?? '',
      pollPeriod: pollPeriod ?? null,
      sendDataToThingsBoard: !!sendDataToThingsBoard,
      byteOrder: byteOrder ?? ModbusOrderType.BIG,
      security: security ?? {},
      identity: identity ?? {
        vendorName: '',
        productCode: '',
        vendorUrl: '',
        productName: '',
        modelName: '',
      },
      values: values ?? {} as ModbusRegisterValues,
      port: port ?? null,
    };
    if (slaveConfig.type === ModbusProtocolType.Serial) {
      slaveState = { ...slaveState, baudrate, serialPort: port, host: '', port: null } as ModbusSlave;
    } else {
      slaveState = { ...slaveState, serialPort: '', baudrate: null } as ModbusSlave;
    }
    this.slaveConfigFormGroup.setValue(slaveState, {emitEvent: false});
  }
}
