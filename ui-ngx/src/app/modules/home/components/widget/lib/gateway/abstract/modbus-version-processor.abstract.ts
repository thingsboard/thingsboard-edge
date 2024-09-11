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
  GatewayConnector,
  ModbusBasicConfig,
  ModbusBasicConfig_v3_5_2,
  ModbusLegacyBasicConfig,
  ModbusLegacySlave,
  ModbusMasterConfig,
  ModbusSlave,
} from '../gateway-widget.models';
import { GatewayConnectorVersionProcessor } from './gateway-connector-version-processor.abstract';
import { ModbusVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/modbus-version-mapping.util';

export class ModbusVersionProcessor extends GatewayConnectorVersionProcessor<any> {

  constructor(
    protected gatewayVersionStr: string,
    protected connector: GatewayConnector<ModbusBasicConfig>
  ) {
    super(gatewayVersionStr, connector);
  }

  getUpgradedVersion(): GatewayConnector<ModbusBasicConfig_v3_5_2> {
    const configurationJson = this.connector.configurationJson;
    return {
      ...this.connector,
      configurationJson: {
        master: configurationJson.master
          ? ModbusVersionMappingUtil.mapMasterToUpgradedVersion(configurationJson.master)
          : {} as ModbusMasterConfig,
        slave: configurationJson.slave
          ? ModbusVersionMappingUtil.mapSlaveToUpgradedVersion(configurationJson.slave as ModbusLegacySlave)
          : {} as ModbusSlave,
      },
      configVersion: this.gatewayVersionStr
    } as GatewayConnector<ModbusBasicConfig_v3_5_2>;
  }

  getDowngradedVersion(): GatewayConnector<ModbusLegacyBasicConfig> {
    const configurationJson = this.connector.configurationJson;
    return {
      ...this.connector,
      configurationJson: {
        ...configurationJson,
        slave: configurationJson.slave
          ? ModbusVersionMappingUtil.mapSlaveToDowngradedVersion(configurationJson.slave as ModbusSlave)
          : {} as ModbusLegacySlave,
        master: configurationJson.master,
      },
      configVersion: this.gatewayVersionStr
    } as GatewayConnector<ModbusLegacyBasicConfig>;
  }
}
