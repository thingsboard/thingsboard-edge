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

export const Constants = {
  serverErrorCode: {
    general: 2,
    authentication: 10,
    jwtTokenExpired: 11,
    tenantTrialExpired: 12,
    credentialsExpired: 15,
    permissionDenied: 20,
    invalidArguments: 30,
    badRequestParams: 31,
    itemNotFound: 32,
    tooManyRequests: 33,
    tooManyUpdates: 34
  },
  entryPoints: {
    login: '/api/auth/login',
    tokenRefresh: '/api/auth/token',
    nonTokenBased: '/api/noauth'
  }
};

export const MediaBreakpoints = {
  xs: 'screen and (max-width: 599px)',
  sm: 'screen and (min-width: 600px) and (max-width: 959px)',
  md: 'screen and (min-width: 960px) and (max-width: 1279px)',
  lg: 'screen and (min-width: 1280px) and (max-width: 1919px)',
  xl: 'screen and (min-width: 1920px) and (max-width: 5000px)',
  'lt-sm': 'screen and (max-width: 599px)',
  'lt-md': 'screen and (max-width: 959px)',
  'lt-lg': 'screen and (max-width: 1279px)',
  'lt-xl': 'screen and (max-width: 1919px)',
  'gt-xs': 'screen and (min-width: 600px)',
  'gt-sm': 'screen and (min-width: 960px)',
  'gt-md': 'screen and (min-width: 1280px)',
  'gt-lg': 'screen and (min-width: 1920px)',
  'gt-xl': 'screen and (min-width: 5001px)'
};

const helpBaseUrl = 'https://thingsboard.io';

export const HelpLinks = {
  linksMap: {
    docs: helpBaseUrl + '/docs',
    outgoingMailSettings: helpBaseUrl + '/docs/user-guide/ui/mail-settings',
    securitySettings: helpBaseUrl + '/docs/user-guide/ui/security-settings',
    ruleEngine: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/overview/',
    ruleNodeCheckRelation: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node',
    ruleNodeCheckExistenceFields: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#check-existence-fields-node',
    ruleNodeJsFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node',
    ruleNodeJsSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#switch-node',
    ruleNodeMessageTypeFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node',
    ruleNodeMessageTypeSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node',
    ruleNodeOriginatorTypeFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node',
    ruleNodeOriginatorTypeSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node',
    ruleNodeOriginatorAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes',
    ruleNodeOriginatorFields: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields',
    ruleNodeCustomerAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes',
    ruleNodeDeviceAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes',
    ruleNodeRelatedAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes',
    ruleNodeTenantAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes',
    ruleNodeChangeOriginator: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#change-originator',
    ruleNodeTransformMsg: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node',
    ruleNodeMsgToEmail: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node',
    ruleNodeClearAlarm: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node',
    ruleNodeCreateAlarm: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node',
    ruleNodeMsgDelay: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#delay-node',
    ruleNodeMsgGenerator: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#generator-node',
    ruleNodeLog: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#log-node',
    ruleNodeRpcCallReply: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node',
    ruleNodeRpcCallRequest: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node',
    ruleNodeSaveAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node',
    ruleNodeSaveTimeseries: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node',
    ruleNodeRuleChain: helpBaseUrl + '/docs/user-guide/ui/rule-chains/',
    ruleNodeAwsSns: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node',
    ruleNodeAwsSqs: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node',
    ruleNodeKafka: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#kafka-node',
    ruleNodeMqtt: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#mqtt-node',
    ruleNodeRabbitMq: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node',
    ruleNodeRestApiCall: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node',
    ruleNodeSendEmail: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#send-email-node',
    ruleNodeIntegrationDownlink: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/action-nodes/#integration-downlink-node',
    ruleNodeAddToGroup: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/action-nodes/#add-to-group-node',
    ruleNodeRemoveFromGroup: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/action-nodes/#remove-from-group-node',
    ruleNodeDuplicateToGroup: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/transformation-nodes/#duplicate-to-group-node',
    ruleNodeDuplicateToRelated: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/transformation-nodes/#duplicate-to-related-node',
    ruleNodeGenerateReport: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/action-nodes/#generate-report-node',
    ruleNodeRestCallReply: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/action-nodes/#rest-call-reply-node',
    ruleNodeAggregateLatest: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#aggregate-latest-node',
    ruleNodeAggregateStream: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#aggregate-stream-node',
    ruleNodeAlarmsCount: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#alarms-count-node',
    tenants: helpBaseUrl + '/docs/user-guide/ui/tenants',
    customers: helpBaseUrl + '/docs/user-guide/customers',
    users: helpBaseUrl + '/docs/user-guide/ui/users',
    devices: helpBaseUrl + '/docs/user-guide/ui/devices',
    assets: helpBaseUrl + '/docs/user-guide/ui/assets',
    entityViews: helpBaseUrl + '/docs/user-guide/ui/entity-views',
    entitiesImport: helpBaseUrl + '/docs/user-guide/bulk-provisioning',
    rulechains: helpBaseUrl + '/docs/user-guide/ui/rule-chains',
    dashboards: helpBaseUrl + '/docs/user-guide/ui/dashboards',
    widgetsBundles: helpBaseUrl + '/docs/user-guide/ui/widget-library#bundles',
    widgetsConfig:  helpBaseUrl + '/docs/user-guide/ui/dashboards#widget-configuration',
    widgetsConfigTimeseries:  helpBaseUrl + '/docs/user-guide/ui/dashboards#timeseries',
    widgetsConfigLatest: helpBaseUrl +  '/docs/user-guide/ui/dashboards#latest',
    widgetsConfigRpc: helpBaseUrl +  '/docs/user-guide/ui/dashboards#rpc',
    widgetsConfigAlarm: helpBaseUrl +  '/docs/user-guide/ui/dashboards#alarm',
    widgetsConfigStatic: helpBaseUrl +  '/docs/user-guide/ui/dashboards#static',
    converters: helpBaseUrl +  '/docs/user-guide/integrations/#data-converters',
    uplinkConverters: helpBaseUrl +  '/docs/user-guide/integrations/#uplink-data-converter',
    downlinkConverters: helpBaseUrl +  '/docs/user-guide/integrations/#downlink-data-converter',
    integrations: helpBaseUrl +  '/docs/user-guide/integrations',
    integrationHttp: helpBaseUrl +  '/docs/user-guide/integrations/http',
    integrationOceanConnect: helpBaseUrl +  '/docs/user-guide/integrations/ocean-connect',
    integrationSigFox: helpBaseUrl +  '/docs/user-guide/integrations/sigfox',
    integrationThingPark: helpBaseUrl +  '/docs/user-guide/integrations/thingpark',
    integrationThingParkEnterprise: helpBaseUrl +  '/docs/samples/abeeway/tracker',
    integrationMqtt: helpBaseUrl +  '/docs/user-guide/integrations/mqtt',
    integrationAwsIoT: helpBaseUrl +  '/docs/user-guide/integrations/aws-iot',
    integrationAwsKinesis:  helpBaseUrl +  '/docs/user-guide/integrations/aws-kinesis',
    integrationIbmWatsonIoT: helpBaseUrl +  '/docs/user-guide/integrations/ibm-watson-iot',
    integrationTheThingsNetwork: helpBaseUrl +  '/docs/user-guide/integrations/ttn',
    integrationAzureEventHub: helpBaseUrl +  '/docs/user-guide/integrations/azure-event-hub',
    integrationOpcUa:  helpBaseUrl +  '/docs/user-guide/integrations/opc-ua',
    whiteLabeling: helpBaseUrl +  '/docs/user-guide/white-labeling',
    entityGroups: helpBaseUrl +  '/docs/user-guide/groups',
    customTranslation: helpBaseUrl +  '/docs/user-guide/custom-translation',
    customMenu: helpBaseUrl +  '/docs/user-guide/custom-menu',
    roles: helpBaseUrl + '/docs/user-guide/ui/roles',
    selfRegistration: helpBaseUrl + '/docs/user-guide/self-registration'
  }
};

export interface ValueTypeData {
  name: string;
  icon: string;
}

export enum ValueType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  DOUBLE = 'DOUBLE',
  BOOLEAN = 'BOOLEAN',
  JSON = 'JSON'
}

export const valueTypesMap = new Map<ValueType, ValueTypeData>(
  [
    [
      ValueType.STRING,
      {
        name: 'value.string',
        icon: 'mdi:format-text'
      }
    ],
    [
      ValueType.INTEGER,
      {
        name: 'value.integer',
        icon: 'mdi:numeric'
      }
    ],
    [
      ValueType.DOUBLE,
      {
        name: 'value.double',
        icon: 'mdi:numeric'
      }
    ],
    [
      ValueType.BOOLEAN,
      {
        name: 'value.boolean',
        icon: 'mdi:checkbox-marked-outline'
      }
    ],
    [
      ValueType.JSON,
      {
        name: 'value.json',
        icon: 'mdi:json'
      }
    ]
  ]
);

export interface ContentTypeData {
  name: string;
  code: string;
}

export enum ContentType {
  JSON = 'JSON',
  TEXT = 'TEXT',
  BINARY = 'BINARY'
}

export const contentTypesMap = new Map<ContentType, ContentTypeData>(
  [
    [
      ContentType.JSON,
      {
        name: 'content-type.json',
        code: 'json'
      }
    ],
    [
      ContentType.TEXT,
      {
        name: 'content-type.text',
        code: 'text'
      }
    ],
    [
      ContentType.BINARY,
      {
        name: 'content-type.binary',
        code: 'text'
      }
    ]
  ]
);

export const customTranslationsPrefix = 'custom.';
export const i18nPrefix = 'i18n';
