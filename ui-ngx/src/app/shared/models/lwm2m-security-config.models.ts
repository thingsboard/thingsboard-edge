///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

export enum Lwm2mSecurityType {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NO_SEC = 'NO_SEC'
}

export const Lwm2mSecurityTypeTranslationMap = new Map<Lwm2mSecurityType, string>(
  [
    [Lwm2mSecurityType.PSK, 'Pre-Shared Key'],
    [Lwm2mSecurityType.RPK, 'Raw Public Key'],
    [Lwm2mSecurityType.X509, 'X.509 Certificate'],
    [Lwm2mSecurityType.NO_SEC, 'No Security'],
  ]
);

export const Lwm2mPublicKeyOrIdTooltipTranslationsMap = new Map<Lwm2mSecurityType, string>(
  [
    [Lwm2mSecurityType.PSK, 'device.lwm2m-security-config.client-publicKey-or-id-tooltip-psk'],
    [Lwm2mSecurityType.RPK, 'device.lwm2m-security-config.client-publicKey-or-id-tooltip-rpk'],
    [Lwm2mSecurityType.X509, 'device.lwm2m-security-config.client-publicKey-or-id-tooltip-x509']
  ]
);

export const Lwm2mClientSecretKeyTooltipTranslationsMap = new Map<Lwm2mSecurityType, string>(
  [
    [Lwm2mSecurityType.PSK, 'device.lwm2m-security-config.client-secret-key-tooltip-psk'],
    [Lwm2mSecurityType.RPK, 'device.lwm2m-security-config.client-secret-key-tooltip-prk'],
    [Lwm2mSecurityType.X509, 'device.lwm2m-security-config.client-secret-key-tooltip-x509']
  ]
);

export const Lwm2mClientKeyTooltipTranslationsMap = new Map<Lwm2mSecurityType, string>(
  [
    [Lwm2mSecurityType.PSK, 'device.lwm2m-security-config.client-secret-key-tooltip-psk'],
    [Lwm2mSecurityType.RPK, 'device.lwm2m-security-config.client-secret-key-tooltip-prk']
  ]
);

export interface ClientSecurityConfig {
  securityConfigClientMode: Lwm2mSecurityType;
  endpoint: string;
  identity?: string;
  key?: string;
  cert?: string;
}

export interface ServerSecurityConfig {
  securityMode: Lwm2mSecurityType;
  clientPublicKeyOrId?: string;
  clientSecretKey?: string;
}

export interface Lwm2mSecurityConfigModels {
  client: ClientSecurityConfig;
  bootstrap: Array<ServerSecurityConfig>;
}


export function getLwm2mSecurityConfigModelsDefault(): Lwm2mSecurityConfigModels {
  return {
    client: {
      securityConfigClientMode: Lwm2mSecurityType.NO_SEC,
      endpoint: ''
    },
    bootstrap: [
      getDefaultServerSecurityConfig()
    ]
  };
}

export function getDefaultClientSecurityConfig(securityConfigMode: Lwm2mSecurityType, endPoint = ''): ClientSecurityConfig {
  let security =  {
    securityConfigClientMode: securityConfigMode,
    endpoint: endPoint,
    identity: '',
    key: '',
  };
  switch (securityConfigMode) {
    case Lwm2mSecurityType.X509:
      security = { ...security, ...{cert: ''}};
      break;
    case Lwm2mSecurityType.PSK:
      security = { ...security, ...{identity: endPoint, key: ''}};
      break;
    case Lwm2mSecurityType.RPK:
      security = { ...security, ...{key: ''}};
      break;
  }
  return security;
}

export function getDefaultServerSecurityConfig(): ServerSecurityConfig {
  return {
    securityMode: Lwm2mSecurityType.NO_SEC
  };
}
