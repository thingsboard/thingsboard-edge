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
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import {
  ModbusBaudrates,
  ModbusMethodLabelsMap,
  ModbusMethodType,
  ModbusOrderType,
  ModbusProtocolLabelsMap,
  ModbusProtocolType,
  ModbusRegisterValues,
  ModbusSerialMethodType,
  ModbusSlave,
  noLeadTrailSpacesRegex,
  PortLimits,
  SlaveConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GatewayPortTooltipPipe } from '@home/components/widget/lib/gateway/pipes/gateway-port-tooltip.pipe';
import { ModbusSecurityConfigComponent } from '../modbus-security-config/modbus-security-config.component';
import { ModbusValuesComponent, } from '../modbus-values/modbus-values.component';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-modbus-slave-config',
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
})
export class ModbusSlaveConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  slaveConfigFormGroup: UntypedFormGroup;
  showSecurityControl: FormControl<boolean>;
  ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;
  ModbusMethodLabelsMap = ModbusMethodLabelsMap;
  portLimits = PortLimits;

  readonly modbusProtocolTypes = Object.values(ModbusProtocolType);
  readonly modbusMethodTypes = Object.values(ModbusMethodType);
  readonly modbusSerialMethodTypes = Object.values(ModbusSerialMethodType);
  readonly modbusOrderType = Object.values(ModbusOrderType);
  readonly ModbusProtocolType = ModbusProtocolType;
  readonly modbusBaudrates = ModbusBaudrates;

  private isSlaveEnabled = false;
  private readonly serialSpecificControlKeys = ['serialPort', 'baudrate'];
  private readonly tcpUdpSpecificControlKeys = ['port', 'security', 'host'];

  private onChange: (value: SlaveConfig) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.showSecurityControl = this.fb.control(false);
    this.slaveConfigFormGroup = this.fb.group({
      type: [ModbusProtocolType.TCP],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      serialPort: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      method: [ModbusMethodType.SOCKET],
      unitId: [null, [Validators.required]],
      baudrate: [this.modbusBaudrates[0]],
      deviceName: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      pollPeriod: [5000, [Validators.required]],
      sendDataToThingsBoard: [false],
      byteOrder:[ModbusOrderType.BIG],
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

    this.observeValueChanges();
    this.observeTypeChange();
    this.observeShowSecurity();
  }

  get protocolType(): ModbusProtocolType {
    return this.slaveConfigFormGroup.get('type').value;
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
      slaveConfigFormGroup: { valid: false }
    };
  }

  writeValue(slaveConfig: ModbusSlave): void {
    this.showSecurityControl.patchValue(!!slaveConfig.security && !isEqual(slaveConfig.security, {}));
    this.updateSlaveConfig(slaveConfig);
  }

  setDisabledState(isDisabled: boolean): void {
    this.isSlaveEnabled = !isDisabled;
    this.updateFormEnableState();
  }

  private observeValueChanges(): void {
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
  }

  private observeTypeChange(): void {
    this.slaveConfigFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => {
        this.updateFormEnableState();
        this.updateMethodType(type);
      });
  }

  private updateMethodType(type: ModbusProtocolType): void {
    if (this.slaveConfigFormGroup.get('method').value !== ModbusMethodType.RTU) {
      this.slaveConfigFormGroup.get('method').patchValue(
        type === ModbusProtocolType.Serial
          ? ModbusSerialMethodType.ASCII
          : ModbusMethodType.SOCKET,
        {emitEvent: false}
      );
    }
  }

  private updateFormEnableState(): void {
    if (this.isSlaveEnabled) {
      this.slaveConfigFormGroup.enable({emitEvent: false});
      this.showSecurityControl.enable({emitEvent: false});
    } else {
      this.slaveConfigFormGroup.disable({emitEvent: false});
      this.showSecurityControl.disable({emitEvent: false});
    }
    this.updateEnablingByProtocol();
    this.updateSecurityEnable(this.showSecurityControl.value);
  }

  private observeShowSecurity(): void {
    this.showSecurityControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => this.updateSecurityEnable(value));
  }

  private updateSecurityEnable(securityEnabled: boolean): void {
    if (securityEnabled && this.isSlaveEnabled && this.protocolType !== ModbusProtocolType.Serial) {
      this.slaveConfigFormGroup.get('security').enable({emitEvent: false});
    } else {
      this.slaveConfigFormGroup.get('security').disable({emitEvent: false});
    }
  }

  private updateEnablingByProtocol(): void {
    const isSerial = this.protocolType === ModbusProtocolType.Serial;
    const enableKeys = isSerial ? this.serialSpecificControlKeys : this.tcpUdpSpecificControlKeys;
    const disableKeys = isSerial ? this.tcpUdpSpecificControlKeys : this.serialSpecificControlKeys;

    if (this.isSlaveEnabled) {
      enableKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({ emitEvent: false }));
    }

    disableKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({ emitEvent: false }));
  }

  private updateSlaveConfig(slaveConfig: ModbusSlave): void {
    const {
      type = ModbusProtocolType.TCP,
      method = ModbusMethodType.RTU,
      unitId = 0,
      deviceName = '',
      deviceType = '',
      pollPeriod = 5000,
      sendDataToThingsBoard = false,
      byteOrder = ModbusOrderType.BIG,
      security = {},
      identity = {
        vendorName: '',
        productCode: '',
        vendorUrl: '',
        productName: '',
        modelName: '',
      },
      values = {} as ModbusRegisterValues,
      baudrate = this.modbusBaudrates[0],
      host = '',
      port = null,
    } = slaveConfig;

    const slaveState: ModbusSlave = {
      type,
      method,
      unitId,
      deviceName,
      deviceType,
      pollPeriod,
      sendDataToThingsBoard: !!sendDataToThingsBoard,
      byteOrder,
      security,
      identity,
      values,
      baudrate,
      host: type === ModbusProtocolType.Serial ? '' : host,
      port: type === ModbusProtocolType.Serial ? null : port,
      serialPort: (type === ModbusProtocolType.Serial ? port : '') as string,
    };

    this.slaveConfigFormGroup.setValue(slaveState, { emitEvent: false });
  }
}
