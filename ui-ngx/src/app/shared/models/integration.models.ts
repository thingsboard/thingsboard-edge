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
import {
  HandlerConfigurationType,
  IdentityType,
  integrationBaseUrlChanged,
  IntegrationCredentialType,
  mqttClientIdMaxLengthValidator,
  mqttClientIdPatternValidator,
  MqttQos,
  OpcKeystoreType,
  OpcMappingType,
  OpcSecurityType,
  TcpBinaryByteOrder,
  TcpTextMessageSeparator,
  ttnVersion,
  ttnVersionMap
} from '@home/components/integration/integration.models';
import { Validators } from '@angular/forms';
import { baseUrl, coapBaseUrl } from '@core/utils';
import barsOptions = jquery.flot.barsOptions;

export enum IntegrationType {
  MQTT = 'MQTT',
  HTTP = 'HTTP',
  TTN = 'TTN',
  AWS_IOT = 'AWS_IOT',
  TCP = 'TCP',
  SIGFOX = 'SIGFOX',
  TTI = 'TTI',
  CHIRPSTACK = 'CHIRPSTACK',
  AZURE_EVENT_HUB = 'AZURE_EVENT_HUB',
  COAP = 'COAP',
  OPC_UA = 'OPC_UA',
  APACHE_PULSAR = 'APACHE_PULSAR',
  AWS_KINESIS = 'AWS_KINESIS',
  AWS_SQS = 'AWS_SQS',
  AZURE_IOT_HUB = 'AZURE_IOT_HUB',
  CUSTOM = 'CUSTOM',
  IBM_WATSON_IOT = 'IBM_WATSON_IOT',
  KAFKA = 'KAFKA',
  LORIOT = 'LORIOT',
  OCEANCONNECT = 'OCEANCONNECT',
  PUB_SUB = 'PUB_SUB',
  RABBITMQ = 'RABBITMQ',
  THINGPARK = 'THINGPARK',
  TMOBILE_IOT_CDP = 'TMOBILE_IOT_CDP',
  TPE = 'TPE',
  UDP = 'UDP'
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
  description: string;
  icon: string;
  tags?: string[];
  http?: boolean;
  mqtt?: boolean;
  remote?: boolean;
}

export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>(
  [
    [
      IntegrationType.HTTP,
      {
        name: 'integration.type-http',
        description: 'integration.type-http-description',
        icon: 'assets/integration-icon/http.svg',
        http: true
      }
    ],
    [
      IntegrationType.OCEANCONNECT,
      {
        name: 'integration.type-ocean-connect',
        description: 'integration.type-ocean-connect-description',
        icon: 'assets/integration-icon/ocean-connect.svg',
        http: true
      }
    ],
    [
      IntegrationType.SIGFOX,
      {
        name: 'integration.type-sigfox',
        description: 'integration.type-sigfox-description',
        icon: 'assets/integration-icon/sigfox.svg',
        http: true
      }
    ],
    [
      IntegrationType.THINGPARK,
      {
        name: 'integration.type-thingpark',
        description: 'integration.type-thingpark-description',
        icon: 'assets/integration-icon/thingpark.svg',
        http: true
      }
    ],
    [
      IntegrationType.TPE,
      {
        name: 'integration.type-thingpark-enterprise',
        description: 'integration.type-thingpark-enterprise-description',
        icon: 'assets/integration-icon/thingpark-enterprise.svg',
        http: true
      }
    ],
    [
      IntegrationType.TMOBILE_IOT_CDP,
      {
        name: 'integration.type-tmobile-iot-cdp',
        description: 'integration.type-tmobile-iot-cdp-description',
        icon: 'assets/integration-icon/iotcreators.com.svg',
        http: true
      }
    ],
    [
      IntegrationType.LORIOT,
      {
        name: 'integration.type-loriot',
        description: 'integration.type-loriot-description',
        icon: 'assets/integration-icon/loriot.svg',
        http: true
      }
    ],
    [
      IntegrationType.MQTT,
      {
        name: 'integration.type-mqtt',
        description: 'integration.type-mqtt-description',
        icon: 'assets/integration-icon/mqtt.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.AWS_IOT,
      {
        name: 'integration.type-aws-iot',
        description: 'integration.type-aws-iot-description',
        icon: 'assets/integration-icon/aws-iot.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.AWS_SQS,
      {
        name: 'integration.type-aws-sqs',
        description: 'integration.type-aws-sqs-description',
        icon: 'assets/integration-icon/aws-sqs.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.AWS_KINESIS,
      {
        name: 'integration.type-aws-kinesis',
        description: 'integration.type-aws-kinesis-description',
        icon: 'assets/integration-icon/aws-kinesis.svg'
      }
    ],
    [
      IntegrationType.IBM_WATSON_IOT,
      {
        name: 'integration.type-ibm-watson-iot',
        description: 'integration.type-ibm-watson-iot-description',
        icon: 'assets/integration-icon/ibm-watson-iot.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.TTN,
      {
        name: 'integration.type-ttn',
        description: 'integration.type-ttn-description',
        icon: 'assets/integration-icon/ttn.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.TTI,
      {
        name: 'integration.type-tti',
        description: 'integration.type-tti-description',
        icon: 'assets/integration-icon/things-stack-industries.svg',
        mqtt: true
      }
    ],
    [
      IntegrationType.CHIRPSTACK,
      {
        name: 'integration.type-chirpstack',
        description: 'integration.type-chirpstack-description',
        icon: 'assets/integration-icon/chirpstack.svg'
      }
    ],
    [
      IntegrationType.AZURE_EVENT_HUB,
      {
        name: 'integration.type-azure-event-hub',
        description: 'integration.type-azure-event-hub-description',
        icon: 'assets/integration-icon/azure-event-hub.svg'
      }
    ],
    [
      IntegrationType.AZURE_IOT_HUB,
      {
        name: 'integration.type-azure-iot-hub',
        description: 'integration.type-azure-iot-hub-description',
        icon: 'assets/integration-icon/azure-iot-hub.svg'
      }
    ],
    [
      IntegrationType.OPC_UA,
      {
        name: 'integration.type-opc-ua',
        description: 'integration.type-opc-ua-description',
        icon: 'assets/integration-icon/opc-ua.svg'
      }
    ],
    [
      IntegrationType.UDP,
      {
        name: 'integration.type-udp',
        description: 'integration.type-udp-description',
        icon: 'assets/integration-icon/udp.svg',
        remote: true
      }
    ],
    [
      IntegrationType.TCP,
      {
        name: 'integration.type-tcp',
        description: 'integration.type-tcp-description',
        icon: 'assets/integration-icon/tcp.svg',
        remote: true
      }
    ],
    [
      IntegrationType.KAFKA,
      {
        name: 'integration.type-kafka',
        description: 'integration.type-kafka-description',
        icon: 'assets/integration-icon/kafka.svg'
      }
    ],
    [
      IntegrationType.RABBITMQ,
      {
        name: 'integration.type-rabbitmq',
        description: 'integration.type-rabbitmq-description',
        icon: 'assets/integration-icon/rabbitmq.svg'
      }
    ],
    [
      IntegrationType.APACHE_PULSAR,
      {
        name: 'integration.type-apache-pulsar',
        description: 'integration.type-apache-pulsar-description',
        icon: 'assets/integration-icon/apache-pulsar.svg'
      }
    ],
    [
      IntegrationType.PUB_SUB,
      {
        name: 'integration.type-pubsub',
        description: 'integration.type-pubsub-description',
        icon: 'assets/integration-icon/pub-sub.svg'
      }
    ],
    [
      IntegrationType.COAP,
      {
        name: 'integration.type-coap',
        description: 'integration.type-coap-description',
        icon: 'assets/integration-icon/coap.svg'
      }
    ],
    [
      IntegrationType.CUSTOM,
      {
        name: 'integration.type-custom',
        description: '',
        icon: 'assets/integration-icon/custom.svg',
        remote: true
      }
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

export interface IntegrationBasic extends BaseData<IntegrationId>, ExportableEntity<IntegrationId> {
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
  additionalInfo?: any;
  edgeTemplate: boolean;
}

export interface Integration extends IntegrationBasic {
  configuration: any;
}

export interface IntegrationInfo extends IntegrationBasic {
  status: IntegrationStatus;
  stats: Array<number>;
}

export interface IntegrationStatus {
  success: boolean;
  serviceId?: string;
  error?: any;
}

export interface IntegrationParams extends EntityGroupParams {
  integrationScope: string;
}

export enum IntegrationSubType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}

export function resolveIntegrationParams(route: ActivatedRouteSnapshot): IntegrationParams {
  const routeParams = {...route.params};
  const routeData = {...route.data};
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

export interface Credentials {
  type: IntegrationCredentialType
}

export interface Topics {
  topicFilters: Array<{filter: string; qos: number}>;
  downlinkTopicPattern: string;
}

export interface ApachePulsarIntegration {
  clientConfiguration: {
    serviceUrl: string;
    topics: string;
    subscriptionName: string;
    maxNumMessages: number;
    maxNumBytes: number;
    timeoutInMs: number;
    credentials: Credentials;
  }
}

export interface AwsIotIntegration {
  clientConfiguration: {
    host: string;
    connectTimeoutSec: number;
    clientId: string;
    maxBytesInMessage: number;
    credentials: Credentials;
  };
  topicFilters: Array<{filter: string; qos: MqttQos}>;
  downlinkTopicPattern: string;
}

export interface AwsKinesisIntegration {
  clientConfiguration: {
    streamName: string;
    region: string;
    accessKeyId: string;
    secretAccessKey: string;
    useCredentialsFromInstanceMetadata: boolean;
    applicationName?: string;
    initialPositionInStream: string;
    useConsumersWithEnhancedFanOut: boolean;
    maxRecords: number;
    requestTimeout: number;
  }
}

export interface AwsSqsIntegration {
  sqsConfiguration: {
    queueUrl: string;
    pollingPeriodSeconds: number;
    region: string;
    accessKeyId: string;
    secretAccessKey: string;
  }
}

export interface AzureEventHubIntegration {
  clientConfiguration: {
    connectTimeoutSec: number;
    connectionString: string;
    consumerGroup?: string;
    iotHubName?: string;
  }
}

export interface AzureIotHubIntegration {
  clientConfiguration: {
    host: string;
    clientId: string;
    maxBytesInMessage: number;
    credentials: Credentials;
  };
  topicFilters: Array<{filter: string; qos: number}>;
}

export interface ChipStackIntegration {
  clientConfiguration: {
    baseUrl: string;
    httpEndpoint: string,
    applicationServerUrl: string;
    applicationServerAPIToken: string;
  }
}

export interface CoapIntegration {
  clientConfiguration: {
    baseUrl: string;
    dtlsBaseUrl: string;
    securityMode: CoapSecurityMode;
    coapEndpoint: string;
    dtlsCoapEndpoint: string;
  }
}

export interface CustomIntegration {
  clazz: string;
  configuration: string;
}

export interface HttpIntegration {
  baseUrl: string;
  httpEndpoint: string;
  enableSecurity?: boolean;
  headersFilter?: {[key: string]: string} | null,
  replaceNoContentToOk: boolean;
}

export interface IbmWatsonIotIntegration extends Topics{
  clientConfiguration: {
    connectTimeoutSec: number;
    maxBytesInMessage: number;
    credentials: {
      type: string;
      username: string;
      password: string;
    }
  }
}

export interface KafkaIntegration {
  clientConfiguration: {
    groupId: string;
    clientId: string;
    topics: string;
    bootstrapServers: string;
    pollInterval: number;
    autoCreateTopics: boolean
    otherProperties?: {[key: string]: string} | null;
  }
}

export interface LoriotIntegration {
  baseUrl: string,
  httpEndpoint: {
    value: string;
    disabled: boolean;
  };
  enableSecurity: boolean;
  headersFilter?: {[key: string]: string} | null;
  replaceNoContentToOk: boolean;
  createLoriotOutput: boolean;
  sendDownlink: boolean;
  server: string;
  domain: string;
  appId: string;
  token: string;
  credentials: Credentials;
  loriotDownlinkUrl: string;
}

export interface MqttIntegration extends Topics{
  clientConfiguration: {
    host: string;
    port: number;
    cleanSession: boolean
    ssl: boolean;
    connectTimeoutSec: number
    clientId: string;
    maxBytesInMessage: number;
    credentials: Credentials
  }
}

export interface OpcUaIntegration {
  clientConfiguration: {
    applicationName?: string;
    applicationUri?: string;
    host: string;
    port: number;
    scanPeriodInSeconds: number;
    timeoutInMillis: number;
    security: OpcSecurityType;
    identity: {
      password: string;
      username: string;
      type: IdentityType;
    }
    mapping: Array<OpcUaMapping>;
    keystore: {
      location: string;
      type: OpcKeystoreType;
      fileContent: string;
      password: string;
      alias: string;
      keyPassword: string;
    }
  }
}

export interface OpcUaMapping {
  deviceNodePattern: string;
  mappingType: OpcMappingType;
  subscriptionTags: Array<OpcUaSubscription>
  namespace: number | null;
}

export interface OpcUaSubscription {
  key: string;
  path: string;
  required: boolean;
}

export interface PubSubIntegration {
  clientConfiguration: {
    projectId: string;
    subscriptionId: string;
    serviceAccountKey: string;
    serviceAccountKeyFileName: string;
  }
}

export interface RabbitMqIntegration {
  clientConfiguration: {
    exchangeName: string;
    host: string;
    port: number;
    virtualHost: string;
    username: string;
    password: string;
    downlinkTopic: string;
    queues: string;
    routingKeys: string;
    connectionTimeout: number;
    handshakeTimeout: number;
    pollPeriod: number;
    durable: boolean;
    exclusive: boolean;
    autoDelete: boolean;
  }
}

export interface TcpIntegration {
  clientConfiguration: {
    port: number;
    soBacklogOption: number;
    soRcvBuf: number;
    soSndBuf: number;
    soKeepaliveOption: boolean;
    tcpNoDelay: boolean;
    cacheSize: number;
    timeToLiveInMinutes: number;
    handlerConfiguration: {
      handlerType: HandlerConfigurationType;
      byteOrder: TcpBinaryByteOrder;
      maxFrameLength: number;
      lengthFieldOffset: number;
      lengthFieldLength: number;
      lengthAdjustment: number;
      initialBytesToStrip: number;
      failFast: boolean;
      stripDelimiter: {
        value: boolean
        disabled: boolean;
      }
      messageSeparator: {
        value: TcpTextMessageSeparator;
        disabled: boolean;
      }
    }
  }
}

export interface ThingParkIntegration {
  baseUrl: string
  httpEndpoint: string;
  enableSecurity: boolean;
  replaceNoContentToOk: boolean;
  downlinkUrl?: string;
  enableSecurityNew: boolean;
  asId?: string;
  asIdNew?: string;
  asKey?: string;
  clientIdNew?: string;
  clientSecret?: string;
  maxTimeDiffInSeconds: number;
}

export interface TtnIntegration extends Topics{
  clientConfiguration: {
   host: string;
   hostEdit?: string;
   customHost: boolean;
   port: number;
   ssl: boolean;
   maxBytesInMessage: number;
   connectTimeoutSec: number;
    apiVersion?: boolean;
   credentials: {
     type: string;
     username: string;
     password: string;
   }
  }
}

export interface UpdIntegration {
  clientConfiguration: {
    port: number;
    soBroadcast: boolean;
    soRcvBuf: number;
    cacheSize: number;
    timeToLiveInMinutes: number;
    handlerConfiguration: {
      handlerType: HandlerConfigurationType;
      charsetName: string;
      maxFrameLength: number;
    }
  }
}
