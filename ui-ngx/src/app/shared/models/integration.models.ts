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
import { TenantId } from '@shared/models/id/tenant-id';
import { IntegrationId } from '@shared/models/id/integration-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { RuleNodeComponentDescriptor } from '@shared/models/rule-node.models';

export enum IntegrationType {
  HTTP = 'HTTP',
  OCEANCONNECT = 'OCEANCONNECT',
  SIGFOX = 'SIGFOX',
  THINGPARK = 'THINGPARK',
  TPE = 'TPE',
  TMOBILE_IOT_CDP = 'TMOBILE_IOT_CDP',
  MQTT = 'MQTT',
  AWS_IOT = 'AWS_IOT',
  AWS_SQS = 'AWS_SQS',
  AWS_KINESIS = 'AWS_KINESIS',
  IBM_WATSON_IOT = 'IBM_WATSON_IOT',
  TTN = 'TTN',
  AZURE_EVENT_HUB = 'AZURE_EVENT_HUB',
  OPC_UA = 'OPC_UA',
  UDP = 'UDP',
  TCP = 'TCP',
  KAFKA = 'KAFKA',
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
      IntegrationType.AZURE_EVENT_HUB,
      { name: 'integration.type-azure-event-hub' }
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
    [IntegrationType.MQTT, 'integrationMqtt'],
    [IntegrationType.AWS_IOT, 'integrationAwsIoT'],
    [IntegrationType.AWS_KINESIS, 'integrationAwsKinesis'],
    [IntegrationType.IBM_WATSON_IOT, 'integrationIbmWatsonIoT'],
    [IntegrationType.TTN, 'integrationTheThingsNetwork'],
    [IntegrationType.AZURE_EVENT_HUB, 'integrationAzureEventHub'],
    [IntegrationType.OPC_UA, 'integrationOpcUa']
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
  secret: string;
  configuration: any;
  additionalInfo?: any;
}
