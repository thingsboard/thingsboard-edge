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

import { isEqual } from '@core/utils';
import {
  GatewayConnector,
  MQTTBasicConfig,
  MQTTBasicConfig_v3_5_2,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestType,
} from '../gateway-widget.models';
import { MqttVersionMappingUtil } from '../utils/mqtt-version-mapping.util';
import { GatewayConnectorVersionProcessor } from './gateway-connector-version-processor.abstract';

export class MqttVersionProcessor extends GatewayConnectorVersionProcessor<MQTTBasicConfig> {

  private readonly mqttRequestTypeKeys = Object.values(RequestType);

  constructor(
    protected gatewayVersionIn: string,
    protected connector: GatewayConnector<MQTTBasicConfig>
  ) {
    super(gatewayVersionIn, connector);
  }

  getUpgradedVersion(): GatewayConnector<MQTTBasicConfig_v3_5_2> {
    const {
      connectRequests,
      disconnectRequests,
      attributeRequests,
      attributeUpdates,
      serverSideRpc
    } = this.connector.configurationJson as MQTTLegacyBasicConfig;
    let configurationJson = {
      ...this.connector.configurationJson,
      requestsMapping: MqttVersionMappingUtil.mapRequestsToUpgradedVersion({
        connectRequests,
        disconnectRequests,
        attributeRequests,
        attributeUpdates,
        serverSideRpc
      }),
      mapping: MqttVersionMappingUtil.mapMappingToUpgradedVersion((this.connector.configurationJson as MQTTLegacyBasicConfig).mapping),
    };

    this.mqttRequestTypeKeys.forEach((key: RequestType) => {
      const { [key]: removedValue, ...rest } = configurationJson as MQTTLegacyBasicConfig;
      configurationJson = { ...rest } as any;
    });

    this.cleanUpConfigJson(configurationJson as MQTTBasicConfig_v3_5_2);

    return {
      ...this.connector,
      configurationJson,
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<MQTTBasicConfig_v3_5_2>;
  }

  getDowngradedVersion(): GatewayConnector<MQTTLegacyBasicConfig> {
    const { requestsMapping, mapping, ...restConfig } = this.connector.configurationJson as MQTTBasicConfig_v3_5_2;

    const updatedRequestsMapping = requestsMapping
      ? MqttVersionMappingUtil.mapRequestsToDowngradedVersion(requestsMapping as Record<RequestType, RequestMappingData[]>) : {};
    const updatedMapping = MqttVersionMappingUtil.mapMappingToDowngradedVersion(mapping);

    return {
      ...this.connector,
      configurationJson: {
        ...restConfig,
        ...updatedRequestsMapping,
        mapping: updatedMapping,
      },
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<MQTTLegacyBasicConfig>;
  }

  private cleanUpConfigJson(configurationJson: MQTTBasicConfig_v3_5_2): void {
    if (isEqual(configurationJson.requestsMapping, {})) {
      delete configurationJson.requestsMapping;
    }

    if (isEqual(configurationJson.mapping, [])) {
      delete configurationJson.mapping;
    }
  }
}
