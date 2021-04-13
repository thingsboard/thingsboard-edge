///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

export const PAGE_SIZE_LIMIT = 50;
export const INSTANCES = 'instances';
export const INSTANCE = 'instance';
export const RESOURCES = 'resources';
export const ATTRIBUTE_LWM2M = 'attributeLwm2m';
export const CLIENT_LWM2M = 'clientLwM2M';
export const CLIENT_LWM2M_SETTINGS = 'clientLwM2mSettings';
export const OBSERVE_ATTR_TELEMETRY = 'observeAttrTelemetry';
export const OBSERVE = 'observe';
export const ATTRIBUTE = 'attribute';
export const TELEMETRY = 'telemetry';
export const KEY_NAME = 'keyName';
export const DEFAULT_ID_SERVER = 123;
export const DEFAULT_ID_BOOTSTRAP = 111;
export const DEFAULT_HOST_NAME = 'localhost';
export const DEFAULT_PORT_SERVER_NO_SEC = 5685;
export const DEFAULT_PORT_BOOTSTRAP_NO_SEC = 5687;
export const DEFAULT_CLIENT_HOLD_OFF_TIME = 1;
export const DEFAULT_LIFE_TIME = 300;
export const DEFAULT_MIN_PERIOD = 1;
export const DEFAULT_NOTIF_IF_DESIBLED = true;
export const DEFAULT_BINDING = 'UQ';
export const DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT = 0;
export const LEN_MAX_PUBLIC_KEY_RPK = 182;
export const LEN_MAX_PUBLIC_KEY_X509 = 3000;
export const KEY_REGEXP_HEX_DEC = /^[-+]?[0-9A-Fa-f]+\.?[0-9A-Fa-f]*?$/;
export const KEY_REGEXP_NUMBER = /^(\-?|\+?)\d*$/;
export const INSTANCES_ID_VALUE_MIN = 0;
export const INSTANCES_ID_VALUE_MAX = 65535;

export enum ATTRIBUTE_LWM2M_ENUM {
  dim = 'dim',
  ver = 'ver',
  pmin = 'pmin',
  pmax = 'pmax',
  gt = 'gt',
  lt = 'lt',
  st = 'st'
}

export const ATTRIBUTE_LWM2M_LABEL = new Map<ATTRIBUTE_LWM2M_ENUM, string>(
  [
    [ATTRIBUTE_LWM2M_ENUM.dim, 'dim='],
    [ATTRIBUTE_LWM2M_ENUM.ver, 'ver='],
    [ATTRIBUTE_LWM2M_ENUM.pmin, 'pmin='],
    [ATTRIBUTE_LWM2M_ENUM.pmax, 'pmax='],
    [ATTRIBUTE_LWM2M_ENUM.gt, '>'],
    [ATTRIBUTE_LWM2M_ENUM.lt, '<'],
    [ATTRIBUTE_LWM2M_ENUM.st, 'st='],

  ]
);

export const ATTRIBUTE_LWM2M_MAP = new Map<ATTRIBUTE_LWM2M_ENUM, string>(
  [
    [ATTRIBUTE_LWM2M_ENUM.dim, 'Dimension'],
    [ATTRIBUTE_LWM2M_ENUM.ver, 'Object version'],
    [ATTRIBUTE_LWM2M_ENUM.pmin, 'Minimum period'],
    [ATTRIBUTE_LWM2M_ENUM.pmax, 'Maximum period'],
    [ATTRIBUTE_LWM2M_ENUM.gt, 'Greater than'],
    [ATTRIBUTE_LWM2M_ENUM.lt, 'Lesser than'],
    [ATTRIBUTE_LWM2M_ENUM.st, 'Step'],

  ]
);

export const ATTRIBUTE_KEYS = Object.keys(ATTRIBUTE_LWM2M_ENUM) as string[];

export enum SECURITY_CONFIG_MODE {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NO_SEC = 'NO_SEC'
}

export const SECURITY_CONFIG_MODE_NAMES = new Map<SECURITY_CONFIG_MODE, string>(
  [
    [SECURITY_CONFIG_MODE.PSK, 'Pre-Shared Key'],
    [SECURITY_CONFIG_MODE.RPK, 'Raw Public Key'],
    [SECURITY_CONFIG_MODE.X509, 'X.509 Certificate'],
    [SECURITY_CONFIG_MODE.NO_SEC, 'No Security']
  ]
);

export interface ModelValue {
  objectIds: string[],
  objectsList: ObjectLwM2M[]
}

export interface BootstrapServersSecurityConfig {
  shortId: number;
  lifetime: number;
  defaultMinPeriod: number;
  notifIfDisabled: boolean;
  binding: string;
}

export interface ServerSecurityConfig {
  host?: string;
  port?: number;
  bootstrapServerIs?: boolean;
  securityMode: string;
  clientPublicKeyOrId?: string;
  clientSecretKey?: string;
  serverPublicKey?: string;
  clientHoldOffTime?: number;
  serverId?: number;
  bootstrapServerAccountTimeout: number;
}

interface BootstrapSecurityConfig {
  servers: BootstrapServersSecurityConfig;
  bootstrapServer: ServerSecurityConfig;
  lwm2mServer: ServerSecurityConfig;
}

export interface Lwm2mProfileConfigModels {
  clientLwM2mSettings: ClientLwM2mSettings;
  observeAttr: ObservableAttributes;
  bootstrap: BootstrapSecurityConfig;

}

export interface ClientLwM2mSettings {
  clientOnlyObserveAfterConnect: number;
}

export interface ObservableAttributes {
  observe: string[];
  attribute: string[];
  telemetry: string[];
  keyName: {};
  attributeLwm2m: {};
}

export function getDefaultBootstrapServersSecurityConfig(): BootstrapServersSecurityConfig {
  return {
    shortId: DEFAULT_ID_SERVER,
    lifetime: DEFAULT_LIFE_TIME,
    defaultMinPeriod: DEFAULT_MIN_PERIOD,
    notifIfDisabled: DEFAULT_NOTIF_IF_DESIBLED,
    binding: DEFAULT_BINDING
  };
}

export function getDefaultBootstrapServerSecurityConfig(hostname: any): ServerSecurityConfig {
  return {
    host: hostname,
    port: DEFAULT_PORT_BOOTSTRAP_NO_SEC,
    bootstrapServerIs: true,
    securityMode: SECURITY_CONFIG_MODE.NO_SEC.toString(),
    serverPublicKey: '',
    clientHoldOffTime: DEFAULT_CLIENT_HOLD_OFF_TIME,
    serverId: DEFAULT_ID_BOOTSTRAP,
    bootstrapServerAccountTimeout: DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT
  };
}

export function getDefaultLwM2MServerSecurityConfig(hostname): ServerSecurityConfig {
  const DefaultLwM2MServerSecurityConfig = getDefaultBootstrapServerSecurityConfig(hostname);
  DefaultLwM2MServerSecurityConfig.bootstrapServerIs = false;
  DefaultLwM2MServerSecurityConfig.port = DEFAULT_PORT_SERVER_NO_SEC;
  DefaultLwM2MServerSecurityConfig.serverId = DEFAULT_ID_SERVER;
  return DefaultLwM2MServerSecurityConfig;
}

function getDefaultProfileBootstrapSecurityConfig(hostname: any): BootstrapSecurityConfig {
  return {
    servers: getDefaultBootstrapServersSecurityConfig(),
    bootstrapServer: getDefaultBootstrapServerSecurityConfig(hostname),
    lwm2mServer: getDefaultLwM2MServerSecurityConfig(hostname)
  };
}

function getDefaultProfileObserveAttrConfig(): ObservableAttributes {
  return {
    observe: [],
    attribute: [],
    telemetry: [],
    keyName: {},
    attributeLwm2m: {}
  };
}

export function getDefaultProfileConfig(hostname?: any): Lwm2mProfileConfigModels {
  return {
    clientLwM2mSettings: getDefaultProfileClientLwM2mSettingsConfig(),
    observeAttr: getDefaultProfileObserveAttrConfig(),
    bootstrap: getDefaultProfileBootstrapSecurityConfig((hostname) ? hostname : DEFAULT_HOST_NAME)
  };
}

function getDefaultProfileClientLwM2mSettingsConfig(): ClientLwM2mSettings {
  return {
    clientOnlyObserveAfterConnect: 1
  };
}

export interface ResourceLwM2M {
  id: number;
  name: string;
  observe: boolean;
  attribute: boolean;
  telemetry: boolean;
  keyName: {};
  attributeLwm2m?: {};
}

export interface Instance {
  id: number;
  attributeLwm2m?: {};
  resources: ResourceLwM2M[];
}

/**
 * multiple  == true  => Multiple
 * multiple  == false => Single
 * mandatory == true  => Mandatory
 * mandatory == false => Optional
 */
export interface ObjectLwM2M {

  id: number;
  keyId: string;
  name: string;
  multiple?: boolean;
  mandatory?: boolean;
  attributeLwm2m?: {};
  instances?: Instance [];
}
