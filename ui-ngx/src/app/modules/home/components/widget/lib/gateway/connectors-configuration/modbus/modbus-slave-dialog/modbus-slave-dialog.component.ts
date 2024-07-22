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

import { ChangeDetectionStrategy, Component, forwardRef, Inject, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import {
  MappingInfo,
  ModbusMethodLabelsMap,
  ModbusMethodType,
  ModbusOrderType,
  ModbusProtocolLabelsMap,
  ModbusProtocolType,
  noLeadTrailSpacesRegex,
  PortLimits, SlaveConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { ModbusValuesComponent } from '../modbus-values/modbus-values.component';
import { ModbusSecurityConfigComponent } from '../modbus-security-config/modbus-security-config.component';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GatewayPortTooltipPipe } from '@home/pipes/gateway-port-tooltip/gateway-port-tooltip.pipe';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-modbus-slave-dialog',
  templateUrl: './modbus-slave-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusSlaveDialogComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusSlaveDialogComponent),
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
      .slaves-config-container {
        width: 900px;
      }
      .nested-expansion-header {
        .mat-content {
          height: 100%;
          overflow: hidden;
        }
      }
    }
  `],
})
export class ModbusSlaveDialogComponent extends DialogComponent<ModbusSlaveDialogComponent, SlaveConfig> implements OnDestroy {

  slaveConfigFormGroup: UntypedFormGroup;
  showSecurityControl: UntypedFormControl;
  portLimits = PortLimits;

  readonly modbusProtocolTypes = Object.values(ModbusProtocolType);
  readonly modbusMethodTypes = Object.values(ModbusMethodType);
  readonly modbusOrderType = Object.values(ModbusOrderType);
  readonly ModbusProtocolType = ModbusProtocolType;
  readonly ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;
  readonly ModbusMethodLabelsMap = ModbusMethodLabelsMap;
  readonly modbusHelpLink =
    'https://thingsboard.io/docs/iot-gateway/config/modbus/#section-master-description-and-configuration-parameters';
  readonly serialSpecificControlKeys = ['serialPort', 'baudrate', 'stopbits', 'bytesize', 'parity', 'strict'];
  readonly tcpUdpSpecificControlKeys = ['port', 'security', 'host'];

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: MappingInfo,
    public dialogRef: MatDialogRef<ModbusSlaveDialogComponent, SlaveConfig>,
  ) {
    super(store, router, dialogRef);

    this.showSecurityControl = this.fb.control(false);
    this.slaveConfigFormGroup = this.fb.group({
      name: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [ModbusProtocolType.TCP, [Validators.required]],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      serialPort: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      method: [ModbusMethodType.SOCKET, []],
      baudrate: [null, []],
      stopbits: [null, []],
      bytesize: [null, []],
      parity: [null, []],
      strict: [false, []],
      unitId: [null, [Validators.required]],
      deviceName: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      sendDataOnlyOnChange: [false, []],
      timeout: [],
      byteOrder: [ModbusOrderType.BIG, []],
      wordOrder: [ModbusOrderType.BIG, []],
      retries: [false, []],
      retryOnEmpty: [false, []],
      retryOnInvalid: [false, []],
      pollPeriod: [null, []],
      connectAttemptTimeMs: [null, []],
      connectAttemptCount: [null, []],
      waitAfterFailedAttemptsMs: [null, []],
      values: [{}, []],
      security: [{}],
    });

    this.slaveConfigFormGroup.patchValue({
      ...this.data.value,
      port: this.data.value.type === ModbusProtocolType.Serial
        ? null
        : this.data.value.port,
      serialPort: this.data.value.type === ModbusProtocolType.Serial
        ? this.data.value.port
        : '',
      values: {
        attributes: this.data.value.attributes ?? [],
        timeseries: this.data.value.timeseries ?? [],
        attributeUpdates: this.data.value.attributeUpdates ?? [],
        rpc: this.data.value.rpc ?? [],
      }
    });
    this.showSecurityControl.patchValue(!!this.data.value.security);
    this.updateControlsEnabling(this.data.value.type);
    this.observeTypeChange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.slaveConfigFormGroup.valid) {
      const slaveResult = {...this.slaveConfigFormGroup.value, ...this.slaveConfigFormGroup.value.values};
      delete slaveResult.values;
      if (slaveResult.type === ModbusProtocolType.Serial) {
        slaveResult.port = slaveResult.serialPort;
      }
      delete slaveResult.serialPort;
      this.dialogRef.close(slaveResult);
    }
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
}
