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

import { deleteNullProperties } from '@core/utils';
import {
  AttributeRequest,
  ConnectorDeviceInfo,
  Converter,
  ConverterConnectorMapping,
  ConvertorType,
  LegacyConverter,
  LegacyConverterConnectorMapping,
  LegacyRequestMappingData,
  RequestMappingData,
  RequestType,
  ServerSideRpc,
  ServerSideRpcType,
  SourceType
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class MqttVersionMappingUtil {

  static readonly mqttRequestTypeKeys = Object.values(RequestType);
  static readonly mqttRequestMappingOldFields =
    ['attributeNameJsonExpression', 'deviceNameJsonExpression', 'deviceNameTopicExpression', 'extension-config'];
  static readonly mqttRequestMappingNewFields =
    ['attributeNameExpressionSource', 'responseTopicQoS', 'extensionConfig'];

  static mapMappingToUpgradedVersion(
    mapping: LegacyConverterConnectorMapping[]
  ): ConverterConnectorMapping[] {
    return mapping?.map(({ converter, topicFilter, subscriptionQos = 1 }) => {
      const deviceInfo = converter.deviceInfo ?? this.extractConverterDeviceInfo(converter);

      const newConverter = {
        ...converter,
        deviceInfo,
        extensionConfig: converter.extensionConfig || converter['extension-config'] || null
      };

      this.cleanUpOldFields(newConverter);

      return { converter: newConverter, topicFilter, subscriptionQos };
    }) as ConverterConnectorMapping[];
  }

  static mapRequestsToUpgradedVersion(
    requestMapping: Record<RequestType,
      LegacyRequestMappingData[]>
  ): Record<RequestType, RequestMappingData[]> {
    return this.mqttRequestTypeKeys.reduce((acc, key: RequestType) => {
      if (!requestMapping[key]) {
        return acc;
      }

      acc[key] = requestMapping[key].map(value => {
        const newValue = this.mapRequestToUpgradedVersion(value as LegacyRequestMappingData, key);

        this.cleanUpOldFields(newValue as {});

        return newValue;
      });

      return acc;
    }, {}) as Record<RequestType, RequestMappingData[]>;
  }

  static mapRequestsToDowngradedVersion(
    requestsMapping: Record<RequestType, RequestMappingData[]>
  ): Record<RequestType, LegacyRequestMappingData[]> {
    return this.mqttRequestTypeKeys.reduce((acc, key) => {
      if (!requestsMapping[key]) {
        return acc;
      }

      acc[key] = requestsMapping[key].map((value: RequestMappingData) => {
        if (key === RequestType.SERVER_SIDE_RPC) {
          delete (value as ServerSideRpc).type;
        }

        const { attributeNameExpression, deviceInfo, ...rest } = value as AttributeRequest;

        const newValue = {
          ...rest,
          attributeNameJsonExpression: attributeNameExpression || null,
          deviceNameJsonExpression: deviceInfo?.deviceNameExpressionSource !== SourceType.TOPIC ? deviceInfo?.deviceNameExpression : null,
          deviceNameTopicExpression: deviceInfo?.deviceNameExpressionSource === SourceType.TOPIC ? deviceInfo?.deviceNameExpression : null,
        };

        this.cleanUpNewFields(newValue);

        return newValue;
      });

      return acc;
    }, {}) as Record<RequestType, LegacyRequestMappingData[]>;
  }

  static mapMappingToDowngradedVersion(
    mapping: ConverterConnectorMapping[]
  ): LegacyConverterConnectorMapping[] {
    return mapping?.map((converterMapping: ConverterConnectorMapping) => {
      const converter = this.mapConverterToDowngradedVersion(converterMapping.converter);

      this.cleanUpNewFields(converter as {});

      return { converter, topicFilter: converterMapping.topicFilter };
    });
  }

  private static mapConverterToDowngradedVersion(converter: Converter): LegacyConverter {
    const { deviceInfo, ...restConverter } = converter;

    return converter.type !== ConvertorType.BYTES ? {
      ...restConverter,
      deviceNameJsonExpression: deviceInfo?.deviceNameExpressionSource === SourceType.MSG ? deviceInfo.deviceNameExpression : null,
      deviceTypeJsonExpression:
        deviceInfo?.deviceProfileExpressionSource === SourceType.MSG ? deviceInfo.deviceProfileExpression : null,
      deviceNameTopicExpression:
        deviceInfo?.deviceNameExpressionSource !== SourceType.MSG
          ? deviceInfo?.deviceNameExpression
          : null,
      deviceTypeTopicExpression: deviceInfo?.deviceProfileExpressionSource !== SourceType.MSG
        ? deviceInfo?.deviceProfileExpression
        : null,
    } : {
      ...restConverter,
      deviceNameExpression: deviceInfo.deviceNameExpression,
      deviceTypeExpression: deviceInfo.deviceProfileExpression,
      ['extension-config']: converter.extensionConfig,
    };
  }

  private static cleanUpOldFields(obj: Record<string, unknown>): void {
    this.mqttRequestMappingOldFields.forEach(field => delete obj[field]);
    deleteNullProperties(obj);
  }

  private static cleanUpNewFields(obj: Record<string, unknown>): void {
    this.mqttRequestMappingNewFields.forEach(field => delete obj[field]);
    deleteNullProperties(obj);
  }

  private static getTypeSourceByValue(value: string): SourceType {
    if (value.includes('${')) {
      return SourceType.MSG;
    }
    if (value.includes(`/`)) {
      return SourceType.TOPIC;
    }
    return SourceType.CONST;
  }

  private static extractConverterDeviceInfo(converter: LegacyConverter): ConnectorDeviceInfo {
    const deviceNameExpression = converter.deviceNameExpression
      || converter.deviceNameJsonExpression
      || converter.deviceNameTopicExpression
      || null;
    const deviceNameExpressionSource = converter.deviceNameExpressionSource
      ? converter.deviceNameExpressionSource as SourceType
      : deviceNameExpression ? this.getTypeSourceByValue(deviceNameExpression) : null;
    const deviceProfileExpression = converter.deviceProfileExpression
      || converter.deviceTypeTopicExpression
      || converter.deviceTypeJsonExpression
      || 'default';
    const deviceProfileExpressionSource = converter.deviceProfileExpressionSource
      ? converter.deviceProfileExpressionSource as SourceType
      : deviceProfileExpression ? this.getTypeSourceByValue(deviceProfileExpression) : null;

    return deviceNameExpression || deviceProfileExpression ? {
      deviceNameExpression,
      deviceNameExpressionSource,
      deviceProfileExpression,
      deviceProfileExpressionSource
    } : null;
  }

  private static mapRequestToUpgradedVersion(value, key: RequestType): RequestMappingData {
    const deviceNameExpression = value.deviceNameJsonExpression || value.deviceNameTopicExpression || null;
    const deviceProfileExpression = value.deviceTypeTopicExpression || value.deviceTypeJsonExpression || 'default';
    const deviceProfileExpressionSource = deviceProfileExpression ? this.getTypeSourceByValue(deviceProfileExpression) : null;
    const attributeNameExpression = value.attributeNameExpressionSource || value.attributeNameJsonExpression || null;
    const responseTopicQoS = key === RequestType.SERVER_SIDE_RPC ? 1 : null;
    const type = key === RequestType.SERVER_SIDE_RPC
      ? (value as ServerSideRpc).responseTopicExpression
        ? ServerSideRpcType.WithResponse
        : ServerSideRpcType.WithoutResponse
      : null;

    return {
      ...value,
      attributeNameExpression,
      attributeNameExpressionSource: attributeNameExpression ? this.getTypeSourceByValue(attributeNameExpression) : null,
      deviceInfo: value.deviceInfo ? value.deviceInfo : deviceNameExpression ? {
        deviceNameExpression,
        deviceNameExpressionSource: this.getTypeSourceByValue(deviceNameExpression),
        deviceProfileExpression,
        deviceProfileExpressionSource
      } : null,
      responseTopicQoS,
      type
    };
  }
}
