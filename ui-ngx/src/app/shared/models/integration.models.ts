///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { IntegrationId } from '@shared/models/id/integration-id';
import { ConverterId } from '@shared/models/id/converter-id';

export enum IntegrationType {
  HTTP = 'HTTP',
  OCEANCONNECT = 'OCEANCONNECT',
  SIGFOX = 'SIGFOX',
  THINGPARK = 'THINGPARK',
  LORIOT = 'LORIOT',
  TPE = 'TPE',
  TMOBILE_IOT_CDP = 'TMOBILE_IOT_CDP',
  MQTT = 'MQTT',
  AWS_IOT = 'AWS_IOT',
  AWS_SQS = 'AWS_SQS',
  AWS_KINESIS = 'AWS_KINESIS',
  IBM_WATSON_IOT = 'IBM_WATSON_IOT',
  TTN = 'TTN',
  TTI = 'TTI',
  AZURE_EVENT_HUB = 'AZURE_EVENT_HUB',
  AZURE_IOT_HUB = 'AZURE_IOT_HUB',
  OPC_UA = 'OPC_UA',
  UDP = 'UDP',
  TCP = 'TCP',
  KAFKA = 'KAFKA',
  RABBITMQ = 'RABBITMQ',
  APACHE_PULSAR = 'APACHE_PULSAR',
  CUSTOM = 'CUSTOM'
}

export interface IntegrationTypeInfo {
  name: string;
  http?: boolean;
  mqtt?: boolean;
  remote?: boolean;
}

export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>(
  [
    [
      IntegrationType.HTTP,
      { name: 'integration.type-http', http: true }
    ],
    [
      IntegrationType.OCEANCONNECT,
      { name: 'integration.type-ocean-connect', http: true }
    ],
    [
      IntegrationType.SIGFOX,
      { name: 'integration.type-sigfox', http: true }
    ],
    [
      IntegrationType.THINGPARK,
      { name: 'integration.type-thingpark', http: true }
    ],
    [
      IntegrationType.TPE,
      { name: 'integration.type-thingpark-enterprise', http: true }
    ],
    [
      IntegrationType.TMOBILE_IOT_CDP,
      { name: 'integration.type-tmobile-iot-cdp', http: true }
    ],
    [
      IntegrationType.LORIOT,
      { name: 'integration.type-loriot', http: true }
    ],
    [
      IntegrationType.MQTT,
      { name: 'integration.type-mqtt', mqtt: true }
    ],
    [
      IntegrationType.AWS_IOT,
      { name: 'integration.type-aws-iot', mqtt: true }
    ],
    [
      IntegrationType.AWS_SQS,
      { name: 'integration.type-aws-sqs', mqtt: true }
    ],
    [
      IntegrationType.AWS_KINESIS,
      { name: 'integration.type-aws-kinesis' }
    ],
    [
      IntegrationType.IBM_WATSON_IOT,
      { name: 'integration.type-ibm-watson-iot', mqtt: true }
    ],
    [
      IntegrationType.TTN,
      { name: 'integration.type-ttn', mqtt: true }
    ],
    [
      IntegrationType.TTI,
      { name: 'integration.type-tti', mqtt: true }
    ],
    [
      IntegrationType.AZURE_EVENT_HUB,
      { name: 'integration.type-azure-event-hub' }
    ],
    [
      IntegrationType.AZURE_IOT_HUB,
      { name: 'integration.type-azure-iot-hub' }
    ],
    [
      IntegrationType.OPC_UA,
      { name: 'integration.type-opc-ua' }
    ],
    [
      IntegrationType.UDP,
      { name: 'integration.type-udp', remote: true }
    ],
    [
      IntegrationType.TCP,
      { name: 'integration.type-tcp', remote: true }
    ],
    [
      IntegrationType.KAFKA,
      { name: 'integration.type-kafka' }
    ],
    [
      IntegrationType.RABBITMQ,
      { name: 'integration.type-rabbitmq' }
    ],
    [
      IntegrationType.APACHE_PULSAR,
      { name: 'integration.type-apache-pulsar' }
    ],
    [
      IntegrationType.CUSTOM,
      { name: 'integration.type-custom', remote: true }
    ]
  ]
);

const integrationHelpLinkMap = new Map<IntegrationType, string>(
  [
    [IntegrationType.HTTP, 'integrationHttp'],
    [IntegrationType.OCEANCONNECT, 'integrationOceanConnect'],
    [IntegrationType.SIGFOX, 'integrationSigFox'],
    [IntegrationType.THINGPARK, 'integrationThingPark'],
    [IntegrationType.TPE, 'integrationThingParkEnterprise'],
    [IntegrationType.TMOBILE_IOT_CDP, 'integrationTMobileIotCdp'],
    [IntegrationType.LORIOT, 'integrationLoriot'],
    [IntegrationType.MQTT, 'integrationMqtt'],
    [IntegrationType.AWS_IOT, 'integrationAwsIoT'],
    [IntegrationType.AWS_SQS, 'integrationAwsSQS'],
    [IntegrationType.AWS_KINESIS, 'integrationAwsKinesis'],
    [IntegrationType.IBM_WATSON_IOT, 'integrationIbmWatsonIoT'],
    [IntegrationType.TTN, 'integrationTheThingsNetwork'],
    [IntegrationType.TTI, 'integrationTheThingsIndustries'],
    [IntegrationType.AZURE_EVENT_HUB, 'integrationAzureEventHub'],
    [IntegrationType.AZURE_IOT_HUB, 'integrationAzureIoTHub'],
    [IntegrationType.OPC_UA, 'integrationOpcUa'],
    [IntegrationType.UDP, 'integrationUdp'],
    [IntegrationType.TCP, 'integrationTcp'],
    [IntegrationType.KAFKA, 'integrationKafka'],
    [IntegrationType.RABBITMQ, 'integrationRabbitmq'],
    [IntegrationType.APACHE_PULSAR, 'integrationApachePulsar'],
    [IntegrationType.CUSTOM, 'integrationCustom']
  ]
);

export function getIntegrationHelpLink(integration: Integration): string {
  if (integration && integration.type) {
    if (integrationHelpLinkMap.has(integration.type)) {
      return integrationHelpLinkMap.get(integration.type);
    }
  }
  return 'integrations';
}

export interface Integration extends BaseData<IntegrationId> {
  tenantId?: TenantId;
  defaultConverterId: ConverterId;
  downlinkConverterId?: ConverterId;
  name: string;
  routingKey: string;
  type: IntegrationType;
  debugMode: boolean;
  enabled: boolean;
  remote: boolean;
  allowCreateDevicesOrAssets: boolean;
  secret: string;
  configuration: any;
  additionalInfo?: any;
}
