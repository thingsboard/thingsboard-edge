///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { BaseData } from '@shared/models/base-data';
import { DeviceId } from './id/device-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { DeviceCredentialsId } from '@shared/models/id/device-credentials-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { KeyFilter } from '@shared/models/query/query.models';
import { TimeUnit } from '@shared/models/time/time.models';

export enum DeviceProfileType {
  DEFAULT = 'DEFAULT'
}

export enum DeviceTransportType {
  DEFAULT = 'DEFAULT',
  MQTT = 'MQTT',
  LWM2M = 'LWM2M'
}

export interface DeviceConfigurationFormInfo {
  hasProfileConfiguration: boolean;
  hasDeviceConfiguration: boolean;
}

export const deviceProfileTypeTranslationMap = new Map<DeviceProfileType, string>(
  [
    [DeviceProfileType.DEFAULT, 'device-profile.type-default']
  ]
);

export const deviceProfileTypeConfigurationInfoMap = new Map<DeviceProfileType, DeviceConfigurationFormInfo>(
  [
    [
      DeviceProfileType.DEFAULT,
      {
        hasProfileConfiguration: false,
        hasDeviceConfiguration: false,
      }
    ]
  ]
);

export const deviceTransportTypeTranslationMap = new Map<DeviceTransportType, string>(
  [
    [DeviceTransportType.DEFAULT, 'device-profile.transport-type-default'],
    [DeviceTransportType.MQTT, 'device-profile.transport-type-mqtt'],
    [DeviceTransportType.LWM2M, 'device-profile.transport-type-lwm2m']
  ]
);

export const deviceTransportTypeConfigurationInfoMap = new Map<DeviceTransportType, DeviceConfigurationFormInfo>(
  [
    [
      DeviceTransportType.DEFAULT,
      {
        hasProfileConfiguration: false,
        hasDeviceConfiguration: false,
      }
    ],
    [
      DeviceTransportType.MQTT,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true,
      }
    ],
    [
      DeviceTransportType.LWM2M,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true,
      }
    ]
  ]
);

export interface DefaultDeviceProfileConfiguration {
  [key: string]: any;
}

export type DeviceProfileConfigurations = DefaultDeviceProfileConfiguration;

export interface DeviceProfileConfiguration extends DeviceProfileConfigurations {
  type: DeviceProfileType;
}

export interface DefaultDeviceProfileTransportConfiguration {
  [key: string]: any;
}

export interface MqttDeviceProfileTransportConfiguration {
  deviceTelemetryTopic?: string;
  deviceAttributesTopic?: string;
  [key: string]: any;
}

export interface Lwm2mDeviceProfileTransportConfiguration {
  [key: string]: any;
}

export type DeviceProfileTransportConfigurations = DefaultDeviceProfileTransportConfiguration &
                                                   MqttDeviceProfileTransportConfiguration &
                                                   Lwm2mDeviceProfileTransportConfiguration;

export interface DeviceProfileTransportConfiguration extends DeviceProfileTransportConfigurations {
  type: DeviceTransportType;
}

export function createDeviceProfileConfiguration(type: DeviceProfileType): DeviceProfileConfiguration {
  let configuration: DeviceProfileConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceProfileType.DEFAULT:
        const defaultConfiguration: DefaultDeviceProfileConfiguration = {};
        configuration = {...defaultConfiguration, type: DeviceProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
}

export function createDeviceConfiguration(type: DeviceProfileType): DeviceConfiguration {
  let configuration: DeviceConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceProfileType.DEFAULT:
        const defaultConfiguration: DefaultDeviceConfiguration = {};
        configuration = {...defaultConfiguration, type: DeviceProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
}

export function createDeviceProfileTransportConfiguration(type: DeviceTransportType): DeviceProfileTransportConfiguration {
  let transportConfiguration: DeviceProfileTransportConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceTransportType.DEFAULT:
        const defaultTransportConfiguration: DefaultDeviceProfileTransportConfiguration = {};
        transportConfiguration = {...defaultTransportConfiguration, type: DeviceTransportType.DEFAULT};
        break;
      case DeviceTransportType.MQTT:
        const mqttTransportConfiguration: MqttDeviceProfileTransportConfiguration = {
          deviceTelemetryTopic: 'v1/devices/me/telemetry',
          deviceAttributesTopic: 'v1/devices/me/attributes'
        };
        transportConfiguration = {...mqttTransportConfiguration, type: DeviceTransportType.MQTT};
        break;
      case DeviceTransportType.LWM2M:
        const lwm2mTransportConfiguration: Lwm2mDeviceProfileTransportConfiguration = {};
        transportConfiguration = {...lwm2mTransportConfiguration, type: DeviceTransportType.LWM2M};
        break;
    }
  }
  return transportConfiguration;
}

export function createDeviceTransportConfiguration(type: DeviceTransportType): DeviceTransportConfiguration {
  let transportConfiguration: DeviceTransportConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceTransportType.DEFAULT:
        const defaultTransportConfiguration: DefaultDeviceTransportConfiguration = {};
        transportConfiguration = {...defaultTransportConfiguration, type: DeviceTransportType.DEFAULT};
        break;
      case DeviceTransportType.MQTT:
        const mqttTransportConfiguration: MqttDeviceTransportConfiguration = {};
        transportConfiguration = {...mqttTransportConfiguration, type: DeviceTransportType.MQTT};
        break;
      case DeviceTransportType.LWM2M:
        const lwm2mTransportConfiguration: Lwm2mDeviceTransportConfiguration = {};
        transportConfiguration = {...lwm2mTransportConfiguration, type: DeviceTransportType.LWM2M};
        break;
    }
  }
  return transportConfiguration;
}

export interface AlarmCondition {
  condition: Array<KeyFilter>;
  durationUnit?: TimeUnit;
  durationValue?: number;
}

export interface AlarmRule {
  condition: AlarmCondition;
  alarmDetails?: string;
}

export interface DeviceProfileAlarm {
  id: string;
  alarmType: string;
  createRules: {[severity: string]: AlarmRule};
  clearRule?: AlarmRule;
  propagate?: boolean;
  propagateRelationTypes?: Array<string>;
}

export interface DeviceProfileData {
  configuration: DeviceProfileConfiguration;
  transportConfiguration: DeviceProfileTransportConfiguration;
  alarms?: Array<DeviceProfileAlarm>;
}

export interface DeviceProfile extends BaseData<DeviceProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  default?: boolean;
  type: DeviceProfileType;
  transportType: DeviceTransportType;
  defaultRuleChainId?: RuleChainId;
  profileData: DeviceProfileData;
}

export interface DeviceProfileInfo extends EntityInfoData {
  type: DeviceProfileType;
  transportType: DeviceTransportType;
}

export interface DefaultDeviceConfiguration {
  [key: string]: any;
}

export type DeviceConfigurations = DefaultDeviceConfiguration;

export interface DeviceConfiguration extends DeviceConfigurations {
  type: DeviceProfileType;
}

export interface DefaultDeviceTransportConfiguration {
  [key: string]: any;
}

export interface MqttDeviceTransportConfiguration {
  [key: string]: any;
}

export interface Lwm2mDeviceTransportConfiguration {
  [key: string]: any;
}

export type DeviceTransportConfigurations = DefaultDeviceTransportConfiguration &
  MqttDeviceTransportConfiguration &
  Lwm2mDeviceTransportConfiguration;

export interface DeviceTransportConfiguration extends DeviceTransportConfigurations {
  type: DeviceTransportType;
}

export interface DeviceData {
  configuration: DeviceConfiguration;
  transportConfiguration: DeviceTransportConfiguration;
}

export interface Device extends BaseData<DeviceId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  label: string;
  deviceProfileId?: DeviceProfileId;
  deviceData?: DeviceData;
  additionalInfo?: any;
}

/*export interface DeviceInfo extends Device {
  customerTitle: string;
  customerIsPublic: boolean;
  deviceProfileName: string;
}*/

export enum DeviceCredentialsType {
  ACCESS_TOKEN = 'ACCESS_TOKEN',
  X509_CERTIFICATE = 'X509_CERTIFICATE',
  MQTT_BASIC = 'MQTT_BASIC'
}

export const credentialTypeNames = new Map<DeviceCredentialsType, string>(
  [
    [DeviceCredentialsType.ACCESS_TOKEN, 'Access token'],
    [DeviceCredentialsType.X509_CERTIFICATE, 'MQTT X.509'],
    [DeviceCredentialsType.MQTT_BASIC, 'MQTT Basic']
  ]
);

export interface DeviceCredentials extends BaseData<DeviceCredentialsId> {
  deviceId: DeviceId;
  credentialsType: DeviceCredentialsType;
  credentialsId: string;
  credentialsValue: string;
}

export interface DeviceCredentialMQTTBasic {
  clientId: string;
  userName: string;
  password: string;
}

export interface DeviceSearchQuery extends EntitySearchQuery {
  deviceTypes: Array<string>;
}

export interface ClaimRequest {
  secretKey: string;
}

export enum ClaimResponse {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  CLAIMED = 'CLAIMED'
}

export interface ClaimResult {
  device: Device;
  response: ClaimResponse;
}
