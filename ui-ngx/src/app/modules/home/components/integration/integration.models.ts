///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Validators } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';

export enum IntegrationCredentialType {
  Anonymous = 'anonymous',
  Basic = 'basic',
  CertPEM = 'cert.PEM',
  Token = 'token',
  SAS = 'sas'
}

export const IntegrationCredentialTypeTranslation = new Map<IntegrationCredentialType, string>([
  [IntegrationCredentialType.Anonymous, 'extension.anonymous'],
  [IntegrationCredentialType.Basic, 'extension.basic'],
  [IntegrationCredentialType.CertPEM, 'extension.pem'],
  [IntegrationCredentialType.Token, 'extension.token'],
  [IntegrationCredentialType.SAS, 'extension.sas']
]);

export enum MqttQos {
  AT_MOST_ONE = 0,
  AT_LEAST_ONCE = 1,
  EXACTLY_ONCE = 2
}

export const MqttQosTranslation = new Map<MqttQos, string>([
  [MqttQos.AT_MOST_ONE, 'integration.mqtt-qos-at-most-once'],
  [MqttQos.AT_LEAST_ONCE, 'integration.mqtt-qos-at-least-once'],
  [MqttQos.EXACTLY_ONCE, 'integration.mqtt-qos-exactly-once']
]);

export interface MqttTopicFilter {
  filter: string;
  qos: MqttQos;
}

export enum ThingsStartHostType {
  Region = 0,
  Custom = 1
}

export const ThingsStartHostTypeTranslation = new Map<ThingsStartHostType, string> ([
  [ThingsStartHostType.Region, 'Region'],
  [ThingsStartHostType.Custom, 'Custom'],
]);

export const mqttClientIdPatternValidator = Validators.pattern('[a-zA-Z0-9]*');
export const mqttClientIdMaxLengthValidator = Validators.maxLength(23);

export enum ttnVersion {
  v2,
  v3
}

export interface TtnVersionParameter {
  downlinkPattern: string;
  uplinkTopic: MqttTopicFilter[];
}

export const ttnVersionMap = new Map<ttnVersion, TtnVersionParameter>([
  [
    ttnVersion.v2, {
      downlinkPattern: '${applicationId}/devices/${devId}/down',
      uplinkTopic: [{
        filter: '+/devices/+/up',
        qos: MqttQos.AT_MOST_ONE
      }]
    }
  ],
  [
    ttnVersion.v3, {
    downlinkPattern: 'v3/${applicationId}/devices/${devId}/down/push',
    uplinkTopic: [{
      filter: 'v3/+/devices/+/up',
      qos: MqttQos.AT_MOST_ONE
    }]
  }
  ]
]);

export function integrationBaseUrlChanged(type: IntegrationType, baseUrl: string, key = ''): string {
  return `${baseUrl}/api/v1/integrations/${type.toLowerCase()}/${key}`;
}

export enum IdentityType {
  Anonymous = 'anonymous',
  Username = 'username'
}

export const IdentityTypeTranslation = new Map<IdentityType, string>([
  [IdentityType.Anonymous, 'extension.anonymous'],
  [IdentityType.Username, 'extension.username']
]);

export enum OpcSecurityType {
  Basic128Rsa15 = 'Basic128Rsa15',
  Basic256 = 'Basic256',
  Basic256Sha256 = 'Basic256Sha256',
  None = 'None'
}

export enum OpcKeystoreType {
  PKCS12 = 'PKCS12',
  JKS = 'JKS'
}

export enum OpcMappingType {
  ID = 'ID',
  FQN = 'FQN'
}

export const OpcMappingTypeTranslation = new Map<OpcMappingType, string>([
  [OpcMappingType.ID, 'ID'],
  [OpcMappingType.FQN, 'Fully Qualified Name']
]);

export enum InitialPositionInStream {
  LATEST = 'LATEST',
  TRIM_HORIZON = 'TRIM_HORIZON',
  AT_TIMESTAMP = 'AT_TIMESTAMP'
}

export const InitialPositionInStreamTranslation = new Map<InitialPositionInStream, string>([
  [InitialPositionInStream.LATEST, 'Latest'],
  [InitialPositionInStream.TRIM_HORIZON, 'Trim horizon'],
  [InitialPositionInStream.AT_TIMESTAMP, 'At timestamp']
]);

export enum TcpHandlerConfigurationType {
  TEXT = 'TEXT',
  BINARY = 'BINARY',
  JSON = 'JSON'
}

export enum UpdHandlerConfigurationType {
  HEX = 'HEX'
}

export type HandlerConfigurationType = TcpHandlerConfigurationType | UpdHandlerConfigurationType;
export const HandlerConfigurationType = {...TcpHandlerConfigurationType, ...UpdHandlerConfigurationType};

export const HandlerConfigurationTypeTranslation = new Map<HandlerConfigurationType, string>([
  [HandlerConfigurationType.TEXT, 'extension.text'],
  [HandlerConfigurationType.BINARY, 'extension.binary'],
  [HandlerConfigurationType.JSON, 'extension.json'],
  [HandlerConfigurationType.HEX, 'extension.hex']
]);

export enum TcpBinaryByteOrder {
  LITTLE_ENDIAN = 'LITTLE_ENDIAN',
  BIG_ENDIAN = 'BIG_ENDIAN'
}

export enum TcpTextMessageSeparator {
  SYSTEM_LINE_SEPARATOR = 'SYSTEM_LINE_SEPARATOR',
  NUL_DELIMITER = 'NUL_DELIMITER'
}
