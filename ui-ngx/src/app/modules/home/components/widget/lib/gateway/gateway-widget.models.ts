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

export enum StorageTypes {
  MEMORY = 'memory',
  FILE = 'file',
  SQLITE = 'sqlite'
}

export enum DeviceGatewayStatus {
  EXCEPTION = 'EXCEPTION'
}

export enum GatewayLogLevel {
  NONE = 'NONE',
  CRITICAL = 'CRITICAL',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFO = 'INFO',
  DEBUG = 'DEBUG'
}

export const GatewayStatus = {
  ...GatewayLogLevel,
  ...DeviceGatewayStatus
};

export type GatewayStatus = DeviceGatewayStatus | GatewayLogLevel;

export enum LogSavingPeriod {
  days = 'D',
  hours = 'H',
  minutes = 'M',
  seconds = 'S'
}

export enum LocalLogsConfigs {
  service = 'service',
  connector = 'connector',
  converter = 'converter',
  tb_connection = 'tb_connection',
  storage = 'storage',
  extension = 'extension'
}

export const LocalLogsConfigTranslateMap = new Map<LocalLogsConfigs, string>([
  [LocalLogsConfigs.service, 'Service'],
  [LocalLogsConfigs.connector, 'Connector'],
  [LocalLogsConfigs.converter, 'Converter'],
  [LocalLogsConfigs.tb_connection, 'TB Connection'],
  [LocalLogsConfigs.storage, 'Storage'],
  [LocalLogsConfigs.extension, 'Extension']
]);

export const LogSavingPeriodTranslations = new Map<LogSavingPeriod, string>(
  [
    [LogSavingPeriod.days, 'gateway.logs.days'],
    [LogSavingPeriod.hours, 'gateway.logs.hours'],
    [LogSavingPeriod.minutes, 'gateway.logs.minutes'],
    [LogSavingPeriod.seconds, 'gateway.logs.seconds']
  ]
);

export const StorageTypesTranslationMap = new Map<StorageTypes, string>(
  [
    [StorageTypes.MEMORY, 'gateway.storage-types.memory-storage'],
    [StorageTypes.FILE, 'gateway.storage-types.file-storage'],
    [StorageTypes.SQLITE, 'gateway.storage-types.sqlite']
  ]
);

export enum SecurityTypes {
  ACCESS_TOKEN = 'accessToken',
  USERNAME_PASSWORD = 'usernamePassword',
  TLS_ACCESS_TOKEN = 'tlsAccessToken',
  TLS_PRIVATE_KEY = 'tlsPrivateKey'
}

export const GecurityTypesTranslationsMap = new Map<SecurityTypes, string>(
  [
    [SecurityTypes.ACCESS_TOKEN, 'gateway.security-types.access-token'],
    [SecurityTypes.USERNAME_PASSWORD, 'gateway.security-types.username-password'],
    [SecurityTypes.TLS_ACCESS_TOKEN, 'gateway.security-types.tls-access-token'],
    // [SecurityTypes.TLS_PRIVATE_KEY, 'gateway.security-types.tls-private-key'],
  ]
);

export interface GatewayConnector {
  name: string;
  type: string;
  configuration?: string;
  configurationJson: string;
  logLevel: string;
  key?: string;
}

export enum ConnectorType {
  MQTT = 'mqtt',
  MODBUS = 'modbus',
  GRPC = 'grpc',
  OPCUA = 'opcua',
  OPCUA_ASYNCIO = 'opcua_asyncio',
  BLE = 'ble',
  REQUEST = 'request',
  CAN = 'can',
  BACNET = 'bacnet',
  ODBC = 'odbc',
  REST = 'rest',
  SNMP = 'snmp',
  FTP = 'ftp',
  SOCKET = 'socket',
  XMPP = 'xmpp',
  OCPP = 'ocpp',
  CUSTOM = 'custom'
}

export const GatewayConnectorDefaultTypesTranslates = new Map<ConnectorType, string>([
  [ConnectorType.MQTT, 'MQTT'],
  [ConnectorType.MODBUS, 'MODBUS'],
  [ConnectorType.GRPC, 'GRPC'],
  [ConnectorType.OPCUA, 'OPCUA'],
  [ConnectorType.OPCUA_ASYNCIO, 'OPCUA ASYNCIO'],
  [ConnectorType.BLE, 'BLE'],
  [ConnectorType.REQUEST, 'REQUEST'],
  [ConnectorType.CAN, 'CAN'],
  [ConnectorType.BACNET, 'BACNET'],
  [ConnectorType.ODBC, 'ODBC'],
  [ConnectorType.REST, 'REST'],
  [ConnectorType.SNMP, 'SNMP'],
  [ConnectorType.FTP, 'FTP'],
  [ConnectorType.SOCKET, 'SOCKET'],
  [ConnectorType.XMPP, 'XMPP'],
  [ConnectorType.OCPP, 'OCPP'],
  [ConnectorType.CUSTOM, 'CUSTOM']
]);

export interface RPCCommand {
  command: string,
  params: any,
  time: number
}


export enum ModbusCommandTypes {
  Bits = 'bits',
  Bit = 'bit',
  String = 'string',
  Bytes = 'bytes',
  Int8 = '8int',
  Uint8 = '8uint',
  Int16 = '16int',
  Uint16 = '16uint',
  Float16 = '16float',
  Int32 = '32int',
  Uint32 = '32uint',
  Float32 = '32float',
  Int64 = '64int',
  Uint64 = '64uint',
  Float64 = '64float'
}

export const ModbusCodesTranslate = new Map<number, string>([
  [1, 'gateway.rpc.read-coils'],
  [2, 'gateway.rpc.read-discrete-inputs'],
  [3, 'gateway.rpc.read-multiple-holding-registers'],
  [4, 'gateway.rpc.read-input-registers'],
  [5, 'gateway.rpc.write-single-coil'],
  [6, 'gateway.rpc.write-single-holding-register'],
  [15, 'gateway.rpc.write-multiple-coils'],
  [16, 'gateway.rpc.write-multiple-holding-registers']
])

export enum BACnetRequestTypes {
  WriteProperty = 'writeProperty',
  ReadProperty = 'readProperty'
}

export const BACnetRequestTypesTranslates = new Map<BACnetRequestTypes, string>([
  [BACnetRequestTypes.WriteProperty, 'gateway.rpc.write-property'],
  [BACnetRequestTypes.ReadProperty, "gateway.rpc.read-property"]
])

export enum BACnetObjectTypes {
  BinaryInput = 'binaryInput',
  BinaryOutput = 'binaryOutput',
  AnalogInput = 'analogInput',
  AnalogOutput = 'analogOutput',
  BinaryValue = 'binaryValue',
  AnalogValue = 'analogValue'
}

export const BACnetObjectTypesTranslates = new Map<BACnetObjectTypes, string>([
  [BACnetObjectTypes.AnalogOutput, 'gateway.rpc.analog-output'],
  [BACnetObjectTypes.AnalogInput, 'gateway.rpc.analog-input'],
  [BACnetObjectTypes.BinaryOutput, 'gateway.rpc.binary-output'],
  [BACnetObjectTypes.BinaryInput, 'gateway.rpc.binary-input'],
  [BACnetObjectTypes.BinaryValue, 'gateway.rpc.binary-value'],
  [BACnetObjectTypes.AnalogValue, 'gateway.rpc.analog-value']
])

export enum BLEMethods {
  WRITE = 'write',
  READ = 'read',
  SCAN = 'scan'
}

export const BLEMethodsTranslates = new Map<BLEMethods, string>([
  [BLEMethods.WRITE, 'gateway.rpc.write'],
  [BLEMethods.READ, 'gateway.rpc.read'],
  [BLEMethods.SCAN, 'gateway.rpc.scan'],
])

export enum CANByteOrders {
  LITTLE = 'LITTLE',
  BIG = 'BIG'
}

export enum SocketMethodProcessings {
  WRITE = 'write'
}

export const SocketMethodProcessingsTranslates = new Map<SocketMethodProcessings, string>([
  [SocketMethodProcessings.WRITE, 'gateway.rpc.write']
])

export enum SNMPMethods {
  SET = 'set',
  MULTISET = "multiset",
  GET = "get",
  BULKWALK = "bulkwalk",
  TABLE = "table",
  MULTIGET = "multiget",
  GETNEXT = "getnext",
  BULKGET = "bulkget",
  WALKS = "walk"
}

export const SNMPMethodsTranslations = new Map<SNMPMethods, string>([
  [SNMPMethods.SET, 'gateway.rpc.set'],
  [SNMPMethods.MULTISET, 'gateway.rpc.multiset'],
  [SNMPMethods.GET, 'gateway.rpc.get'],
  [SNMPMethods.BULKWALK, 'gateway.rpc.bulk-walk'],
  [SNMPMethods.TABLE, 'gateway.rpc.table'],
  [SNMPMethods.MULTIGET, 'gateway.rpc.multi-get'],
  [SNMPMethods.GETNEXT, 'gateway.rpc.get-next'],
  [SNMPMethods.BULKGET, 'gateway.rpc.bul-kget'],
  [SNMPMethods.WALKS, 'gateway.rpc.walk']
])

export enum HTTPMethods {
  CONNECT = 'CONNECT',
  DELETE = 'DELETE',
  GET = 'GET',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
  PATCH = 'PATCH',
  POST = 'POST',
  PUT = 'PUT',
  TRACE = 'TRACE'

}

export enum SocketEncodings {
  UTF_8 = 'utf-8'
}

export interface RPCTemplate {
  name?: string;
  config: RPCTemplateConfig;
}

export interface RPCTemplateConfig {
  [key: string]: any;
}

export interface SaveRPCTemplateData {
  config: RPCTemplateConfig,
  templates: Array<RPCTemplate>
}

export interface LogLink {
  name: string;
  key: string;
  filterFn?: (arg: any) => boolean;
}

export interface GatewayLogData {
  ts: number;
  key: string;
  message: string;
  status: GatewayStatus;
}
