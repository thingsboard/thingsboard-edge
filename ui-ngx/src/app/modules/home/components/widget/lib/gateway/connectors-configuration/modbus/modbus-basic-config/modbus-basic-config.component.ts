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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  ModbusBasicConfig_v3_5_2,
  ModbusMasterConfig,
  ModbusSlave
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ModbusSlaveConfigComponent } from '../modbus-slave-config/modbus-slave-config.component';
import { ModbusMasterTableComponent } from '../modbus-master-table/modbus-master-table.component';
import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import {
  ModbusBasicConfigDirective
} from '@home/components/widget/lib/gateway/connectors-configuration/modbus/modbus-basic-config/modbus-basic-config.abstract';

@Component({
  selector: 'tb-modbus-basic-config',
  templateUrl: './modbus-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusSlaveConfigComponent,
    ModbusMasterTableComponent,
    EllipsisChipListDirective,
  ],
  styleUrls: ['./modbus-basic-config.component.scss'],
})
export class ModbusBasicConfigComponent extends ModbusBasicConfigDirective<ModbusBasicConfig_v3_5_2, ModbusBasicConfig_v3_5_2> {

  isLegacy = false;

  protected override mapConfigToFormValue({ master, slave }: ModbusBasicConfig_v3_5_2): ModbusBasicConfig_v3_5_2 {
    return {
      master: master?.slaves ? master : { slaves: [] } as ModbusMasterConfig,
      slave: slave ?? {} as ModbusSlave,
    };
  }

  protected override getMappedValue(value: ModbusBasicConfig_v3_5_2): ModbusBasicConfig_v3_5_2 {
    return {
      master: value.master,
      slave: value.slave,
    };
  }
}
