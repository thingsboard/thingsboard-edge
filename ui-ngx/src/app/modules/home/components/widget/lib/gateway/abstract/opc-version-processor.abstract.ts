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
  GatewayConnector, LegacyServerConfig,
  OPCBasicConfig,
  OPCBasicConfig_v3_5_2,
  OPCLegacyBasicConfig,
} from '../gateway-widget.models';
import { GatewayConnectorVersionProcessor } from './gateway-connector-version-processor.abstract';
import { OpcVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/opc-version-mapping.util';

export class OpcVersionProcessor extends GatewayConnectorVersionProcessor<OPCBasicConfig> {

  constructor(
    protected gatewayVersionStr: string,
    protected connector: GatewayConnector<OPCBasicConfig>
  ) {
    super(gatewayVersionStr, connector);
  }

  getUpgradedVersion(): GatewayConnector<OPCBasicConfig_v3_5_2> {
    const server = this.connector.configurationJson.server as LegacyServerConfig;
    return {
      ...this.connector,
      configurationJson: {
        server: server ? OpcVersionMappingUtil.mapServerToUpgradedVersion(server) : {},
        mapping: server.mapping ? OpcVersionMappingUtil.mapMappingToUpgradedVersion(server.mapping) : [],
      },
      configVersion: this.gatewayVersionStr
    } as GatewayConnector<OPCBasicConfig_v3_5_2>;
  }

  getDowngradedVersion(): GatewayConnector<OPCLegacyBasicConfig> {
    return {
      ...this.connector,
      configurationJson: {
        server: OpcVersionMappingUtil.mapServerToDowngradedVersion(this.connector.configurationJson as OPCBasicConfig_v3_5_2)
      },
      configVersion: this.gatewayVersionStr
    } as GatewayConnector<OPCLegacyBasicConfig>;
  }
}
