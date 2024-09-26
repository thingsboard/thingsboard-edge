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

import {
  LegacySlaveConfig,
  ModbusDataType,
  ModbusLegacyRegisterValues,
  ModbusLegacySlave,
  ModbusMasterConfig,
  ModbusRegisterValues,
  ModbusSlave,
  ModbusValue,
  ModbusValues,
  ReportStrategyType,
  SlaveConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class ModbusVersionMappingUtil {

  static mapMasterToUpgradedVersion(master: ModbusMasterConfig<LegacySlaveConfig>): ModbusMasterConfig {
    return {
      slaves: master.slaves.map((slave: LegacySlaveConfig) => {
        const { sendDataOnlyOnChange, ...restSlave } = slave;
        return {
          ...restSlave,
          deviceType: slave.deviceType ?? 'default',
          reportStrategy: sendDataOnlyOnChange
            ? { type: ReportStrategyType.OnChange }
            : { type: ReportStrategyType.OnReportPeriod, reportPeriod: slave.pollPeriod }
        };
      })
    };
  }

  static mapMasterToDowngradedVersion(master: ModbusMasterConfig): ModbusMasterConfig<LegacySlaveConfig> {
    return {
      slaves: master.slaves.map((slave: SlaveConfig) => {
        const { reportStrategy, ...restSlave } = slave;
        return {
          ...restSlave,
          sendDataOnlyOnChange: reportStrategy?.type !== ReportStrategyType.OnReportPeriod
        };
      })
    };
  }

  static mapSlaveToDowngradedVersion(slave: ModbusSlave): ModbusLegacySlave {
    if (!slave?.values) {
      return slave as Omit<ModbusLegacySlave, 'values'>;
    }
    const values = Object.keys(slave.values).reduce((acc, valueKey) => {
      acc = {
        ...acc,
        [valueKey]: [
          slave.values[valueKey]
        ]
      };
      return acc;
    }, {} as ModbusLegacyRegisterValues);
    return {
      ...slave,
      values
    };
  }

  static mapSlaveToUpgradedVersion(slave: ModbusLegacySlave): ModbusSlave {
    if (!slave?.values) {
      return slave as Omit<ModbusSlave, 'values'>;
    }
    const values = Object.keys(slave.values).reduce((acc, valueKey) => {
      acc = {
        ...acc,
        [valueKey]: this.mapValuesToUpgradedVersion(slave.values[valueKey][0])
      };
      return acc;
    }, {} as ModbusRegisterValues);
    return {
      ...slave,
      values
    };
  }

  private static mapValuesToUpgradedVersion(registerValues: ModbusValues): ModbusValues {
    return Object.keys(registerValues).reduce((acc, valueKey) => {
      acc = {
       ...acc,
        [valueKey]: registerValues[valueKey].map((value: ModbusValue) =>
          ({ ...value, type: (value.type as string) === 'int' ? ModbusDataType.INT16 : value.type }))
      };
      return acc;
    }, {} as ModbusValues);
  }
}
