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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { IntegrationId } from '@shared/models/id/integration-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { ActivatedRouteSnapshot } from '@angular/router';

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
  CHIRPSTACK = 'CHIRPSTACK',
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
  PUB_SUB = 'PUB_SUB',
  COAP = 'COAP',
  CUSTOM = 'CUSTOM'
}

export enum CoapSecurityMode {
  NO_SECURE = 'NO_SECURE',
  DTLS = 'DTLS',
  MIXED = 'MIXED',
}

export const coapSecurityModeTranslationsMap = new Map<CoapSecurityMode, string>(
  [
    [CoapSecurityMode.NO_SECURE, 'integration.coap-security-mode-no-secure'],
    [CoapSecurityMode.DTLS, 'integration.coap-security-mode-dtls'],
    [CoapSecurityMode.MIXED, 'integration.coap-security-mode-mixed']
  ]
);

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
      IntegrationType.CHIRPSTACK,
      { name: 'integration.type-chirpstack' }
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
      IntegrationType.PUB_SUB,
      { name: 'integration.type-pubsub' }
    ],
    [
      IntegrationType.COAP,
      { name: 'integration.type-coap' }
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
    [IntegrationType.CHIRPSTACK, 'integrationChirpStack'],
    [IntegrationType.AZURE_EVENT_HUB, 'integrationAzureEventHub'],
    [IntegrationType.AZURE_IOT_HUB, 'integrationAzureIoTHub'],
    [IntegrationType.OPC_UA, 'integrationOpcUa'],
    [IntegrationType.UDP, 'integrationUdp'],
    [IntegrationType.TCP, 'integrationTcp'],
    [IntegrationType.KAFKA, 'integrationKafka'],
    [IntegrationType.RABBITMQ, 'integrationRabbitmq'],
    [IntegrationType.APACHE_PULSAR, 'integrationApachePulsar'],
    [IntegrationType.PUB_SUB, 'integrationPubsub'],
    [IntegrationType.COAP, 'integrationCoAP'],
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

export interface Integration extends BaseData<IntegrationId>, ExportableEntity<IntegrationId> {
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
  edgeTemplate: boolean;
}

export interface IntegrationParams extends EntityGroupParams {
  integrationScope: string;
}

export enum IntegrationSubType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}

export function resolveIntegrationParams(route: ActivatedRouteSnapshot): IntegrationParams {
  let routeParams = {...route.params};
  let routeData = {...route.data};
  let edgeId: string;
  let integrationScope: string;
  if (routeParams?.hierarchyView) {
    edgeId = routeParams.edgeId;
    integrationScope = routeParams.integrationScope;
  } else {
    edgeId = routeParams?.edgeId;
    integrationScope = routeData.integrationsType ? routeData.integrationsType : 'tenant';
  }
  return {
    edgeId,
    integrationScope,
    hierarchyView: routeParams?.hierarchyView,
    entityGroupId: routeParams?.entityGroupId,
    childEntityGroupId: routeParams?.childEntityGroupId,
    customerId: routeParams?.customerId
  };
}

