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

import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import {
  FormBuilder,
} from '@angular/forms';
import {
  LegacySlaveConfig,
  ModbusProtocolType,
  ModbusSlaveInfo,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { ModbusValuesComponent } from '../modbus-values/modbus-values.component';
import { ModbusSecurityConfigComponent } from '../modbus-security-config/modbus-security-config.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GatewayPortTooltipPipe } from '@home/components/widget/lib/gateway/pipes/gateway-port-tooltip.pipe';
import {
  ReportStrategyComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/report-strategy/report-strategy.component';
import {
  ModbusSlaveDialogAbstract
} from '@home/components/widget/lib/gateway/connectors-configuration/modbus/modbus-slave-dialog/modbus-slave-dialog.abstract';

@Component({
  selector: 'tb-modbus-legacy-slave-dialog',
  templateUrl: './modbus-slave-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusValuesComponent,
    ModbusSecurityConfigComponent,
    GatewayPortTooltipPipe,
    ReportStrategyComponent,
  ],
  styleUrls: ['./modbus-slave-dialog.component.scss'],
})
export class ModbusLegacySlaveDialogComponent extends ModbusSlaveDialogAbstract<ModbusLegacySlaveDialogComponent, LegacySlaveConfig> {

  constructor(
    protected fb: FormBuilder,
    protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: ModbusSlaveInfo,
    public dialogRef: MatDialogRef<ModbusLegacySlaveDialogComponent, LegacySlaveConfig>,
  ) {
    super(fb, store, router, data, dialogRef);
  }

  protected override getSlaveResultData(): LegacySlaveConfig {
    const { values, type, serialPort, ...rest } = this.slaveConfigFormGroup.value;
    const slaveResult = { ...rest, type, ...values };

    if (type === ModbusProtocolType.Serial) {
      slaveResult.port = serialPort;
    }

    return slaveResult;
  }


  protected override addFieldsToFormGroup(): void {
    this.slaveConfigFormGroup.addControl('sendDataOnlyOnChange', this.fb.control(false));
  }
}
