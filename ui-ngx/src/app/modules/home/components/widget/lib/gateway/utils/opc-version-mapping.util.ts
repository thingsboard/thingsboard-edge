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
  Attribute,
  AttributesUpdate,
  DeviceConnectorMapping,
  LegacyAttribute,
  LegacyDeviceAttributeUpdate,
  LegacyDeviceConnectorMapping,
  LegacyRpcMethod,
  LegacyServerConfig,
  LegacyTimeseries,
  OPCBasicConfig_v3_5_2,
  OPCUaSourceType,
  RpcArgument,
  RpcMethod,
  ServerConfig,
  Timeseries
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class OpcVersionMappingUtil {

  static mapServerToUpgradedVersion(server: LegacyServerConfig): ServerConfig {
    const { mapping, disableSubscriptions, pollPeriodInMillis, ...restServer } = server;
    return {
      ...restServer,
      pollPeriodInMillis: pollPeriodInMillis ?? 5000,
      enableSubscriptions: !disableSubscriptions,
    };
  }

  static mapServerToDowngradedVersion(config: OPCBasicConfig_v3_5_2): LegacyServerConfig {
    const { mapping, server } = config;
    const { enableSubscriptions, ...restServer } = server ?? {} as ServerConfig;
    return {
      ...restServer,
      mapping: mapping ? this.mapMappingToDowngradedVersion(mapping) : [],
      disableSubscriptions: !enableSubscriptions,
    };
  }

  static mapMappingToUpgradedVersion(mapping: LegacyDeviceConnectorMapping[]): DeviceConnectorMapping[] {
    return mapping.map((legacyMapping: LegacyDeviceConnectorMapping) => ({
      ...legacyMapping,
      deviceNodeSource: this.getTypeSourceByValue(legacyMapping.deviceNodePattern),
      deviceInfo: {
        deviceNameExpression: legacyMapping.deviceNamePattern,
        deviceNameExpressionSource: this.getTypeSourceByValue(legacyMapping.deviceNamePattern),
        deviceProfileExpression: legacyMapping.deviceTypePattern ?? 'default',
        deviceProfileExpressionSource: this.getTypeSourceByValue(legacyMapping.deviceTypePattern ?? 'default'),
      },
      attributes: legacyMapping.attributes.map((attribute: LegacyAttribute) => ({
        key: attribute.key,
        type: this.getTypeSourceByValue(attribute.path),
        value: attribute.path,
      })),
      attributes_updates: legacyMapping.attributes_updates.map((attributeUpdate: LegacyDeviceAttributeUpdate) => ({
        key: attributeUpdate.attributeOnThingsBoard,
        type: this.getTypeSourceByValue(attributeUpdate.attributeOnDevice),
        value: attributeUpdate.attributeOnDevice,
      })),
      timeseries: legacyMapping.timeseries.map((timeseries: LegacyTimeseries) => ({
        key: timeseries.key,
        type: this.getTypeSourceByValue(timeseries.path),
        value: timeseries.path,
      })),
      rpc_methods: legacyMapping.rpc_methods.map((rpcMethod: LegacyRpcMethod) => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map(arg => ({
          value: arg,
          type: this.getArgumentType(arg),
        } as RpcArgument))
      }))
    }));
  }

  static mapMappingToDowngradedVersion(mapping: DeviceConnectorMapping[]): LegacyDeviceConnectorMapping[] {
    return mapping.map((upgradedMapping: DeviceConnectorMapping) => ({
      ...upgradedMapping,
      deviceNamePattern: upgradedMapping.deviceInfo.deviceNameExpression,
      deviceTypePattern: upgradedMapping.deviceInfo.deviceProfileExpression,
      attributes: upgradedMapping.attributes.map((attribute: Attribute) => ({
        key: attribute.key,
        path: attribute.value,
      })),
      attributes_updates: upgradedMapping.attributes_updates.map((attributeUpdate: AttributesUpdate) => ({
        attributeOnThingsBoard: attributeUpdate.key,
        attributeOnDevice: attributeUpdate.value,
      })),
      timeseries: upgradedMapping.timeseries.map((timeseries: Timeseries) => ({
        key: timeseries.key,
        path: timeseries.value,
      })),
      rpc_methods: upgradedMapping.rpc_methods.map((rpcMethod: RpcMethod) => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map((arg: RpcArgument) => arg.value)
      }))
    }));
  }

  private static getTypeSourceByValue(value: string): OPCUaSourceType {
    if (value.includes('${')) {
      return OPCUaSourceType.IDENTIFIER;
    }
    if (value.includes(`/`) || value.includes('\\')) {
      return OPCUaSourceType.PATH;
    }
    return OPCUaSourceType.CONST;
  }

  private static getArgumentType(arg: unknown): string {
    switch (typeof arg) {
      case 'boolean':
        return 'boolean';
      case 'number':
        return Number.isInteger(arg) ? 'integer' : 'float';
      default:
        return 'string';
    }
  }
}
