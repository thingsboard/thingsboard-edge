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
  ConnectorType,
  GatewayConnector,
  LegacyGatewayConnector,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { MqttVersionMappingUtil } from './mqtt-version-mapping.util';

export class GatewayConnectorVersionMappingUtil {

  static getMappedByVersion(connector: GatewayConnector, gatewayVersion: string): GatewayConnector {
    switch (connector.type) {
      case ConnectorType.MQTT:
        return this.getMappedMQTTByVersion(connector, gatewayVersion);
      default:
        return connector;
    }
  }

  private static getMappedMQTTByVersion(
    connector: GatewayConnector | LegacyGatewayConnector,
    gatewayVersion: string
  ): GatewayConnector | LegacyGatewayConnector {
    if (this.isVersionUpdateNeeded(gatewayVersion, connector.configVersion)) {
      return this.isGatewayOutdated(gatewayVersion, connector.configVersion)
        ? MqttVersionMappingUtil.getLegacyVersion(connector)
        : MqttVersionMappingUtil.getNewestVersion(connector);
    }
    return connector;
  }

  private static isGatewayOutdated(gatewayVersion: string, configVersion: string): boolean {
    if (!gatewayVersion || !configVersion) {
      return false;
    }

    return this.parseVersion(configVersion) > this.parseVersion(gatewayVersion);
  }

  private static isVersionUpdateNeeded(configVersion: string, gatewayVersion: string): boolean {
    if (!gatewayVersion || !configVersion) {
      return false;
    }

    return this.parseVersion(configVersion) !== this.parseVersion(gatewayVersion);
  }

  private static parseVersion(version: string): number {
    return Number(version?.replace(/\./g, ''));
  }
}
