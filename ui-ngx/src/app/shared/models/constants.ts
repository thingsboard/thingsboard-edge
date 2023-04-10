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

import { InjectionToken } from '@angular/core';
import { IModulesMap } from '@modules/common/modules-map.models';
import { EntityType } from '@shared/models/entity-type.models';

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

export const serverErrorCodesTranslations = new Map<number, string>([
  [Constants.serverErrorCode.general, 'server-error.general'],
  [Constants.serverErrorCode.authentication, 'server-error.authentication'],
  [Constants.serverErrorCode.jwtTokenExpired, 'server-error.jwt-token-expired'],
  [Constants.serverErrorCode.tenantTrialExpired, 'server-error.tenant-trial-expired'],
  [Constants.serverErrorCode.credentialsExpired, 'server-error.credentials-expired'],
  [Constants.serverErrorCode.permissionDenied, 'server-error.permission-denied'],
  [Constants.serverErrorCode.invalidArguments, 'server-error.invalid-arguments'],
  [Constants.serverErrorCode.badRequestParams, 'server-error.bad-request-params'],
  [Constants.serverErrorCode.itemNotFound, 'server-error.item-not-found'],
  [Constants.serverErrorCode.tooManyRequests, 'server-error.too-many-requests'],
  [Constants.serverErrorCode.tooManyUpdates, 'server-error.too-many-updates'],
]);

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

export const helpBaseUrl = 'https://thingsboard.io';

export const HelpLinks = {
  linksMap: {
    docs: helpBaseUrl + '/docs/pe',
    outgoingMailSettings: helpBaseUrl + '/docs/pe/user-guide/ui/mail-settings',
    smsProviderSettings: helpBaseUrl + '/docs/pe/user-guide/ui/sms-provider-settings',
    slackSettings: helpBaseUrl + '/docs/pe/user-guide/ui/slack-settings',
    securitySettings: helpBaseUrl + '/docs/pe/user-guide/ui/security-settings',
    oauth2Settings: helpBaseUrl + '/docs/pe/user-guide/oauth-2-support/',
    twoFactorAuthSettings: helpBaseUrl + '/docs/',
    ruleEngine: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/overview/',
    ruleNodeCheckRelation: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node',
    ruleNodeCheckExistenceFields: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#check-existence-fields-node',
    ruleNodeGpsGeofencingFilter: helpBaseUrl + '/docs//pe/user-guide/rule-engine-2-0/filter-nodes/#gps-geofencing-filter-node',
    ruleNodeJsFilter: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node',
    ruleNodeJsSwitch: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#switch-node',
    ruleNodeAssetProfileSwitch: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#asset-profile-switch',
    ruleNodeDeviceProfileSwitch: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#device-profile-switch',
    ruleNodeCheckAlarmStatus: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#check-alarm-status',
    ruleNodeMessageTypeFilter: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node',
    ruleNodeMessageTypeSwitch: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node',
    ruleNodeOriginatorTypeFilter: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node',
    ruleNodeOriginatorTypeSwitch: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node',
    ruleNodeOriginatorAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes',
    ruleNodeOriginatorFields: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields',
    ruleNodeOriginatorTelemetry: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#originator-telemetry',
    ruleNodeCustomerAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes',
    ruleNodeCustomerDetails: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#customer-details',
    ruleNodeDeviceAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes',
    ruleNodeRelatedAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes',
    ruleNodeTenantAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes',
    ruleNodeTenantDetails: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-details',
    ruleNodeChangeOriginator: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#change-originator',
    ruleNodeTransformMsg: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node',
    ruleNodeMsgToEmail: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node',
    ruleNodeAssignToCustomer: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#assign-to-customer-node',
    ruleNodeUnassignFromCustomer: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#unassign-from-customer-node',
    ruleNodeClearAlarm: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node',
    ruleNodeCreateAlarm: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node',
    ruleNodeCreateRelation: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#create-relation-node',
    ruleNodeDeleteRelation: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#delete-relation-node',
    ruleNodeMsgDelay: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#delay-node',
    ruleNodeMsgGenerator: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#generator-node',
    ruleNodeGpsGeofencingEvents: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#gps-geofencing-events-node',
    ruleNodeLog: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#log-node',
    ruleNodeRpcCallReply: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node',
    ruleNodeRpcCallRequest: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node',
    ruleNodeSaveAttributes: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node',
    ruleNodeSaveTimeseries: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node',
    ruleNodeSaveToCustomTable: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#save-to-custom-table',
    ruleNodeRuleChain: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/flow-nodes/#rule-chain-node',
    ruleNodeOutputNode: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/flow-nodes/#output-node',
    ruleNodeAwsSns: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node',
    ruleNodeAwsSqs: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node',
    ruleNodeKafka: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#kafka-node',
    ruleNodeMqtt: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#mqtt-node',
    ruleNodeAzureIotHub: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#azure-iot-hub-node',
    ruleNodeRabbitMq: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node',
    ruleNodeRestApiCall: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node',
    ruleNodeSendEmail: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#send-email-node',
    ruleNodeSendSms: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/external-nodes/#send-sms-node',
    ruleNodeMath: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#math-function-node',
    ruleNodeIntegrationDownlink: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#integration-downlink-node',
    ruleNodeAddToGroup: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#add-to-group-node',
    ruleNodeRemoveFromGroup: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#remove-from-group-node',
    ruleNodeDuplicateToGroup: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#duplicate-to-group-node',
    ruleNodeDuplicateToGroupByName: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#duplicate-to-group-node-by-name',
    ruleNodeDuplicateToRelated: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/transformation-nodes/#duplicate-to-related-node',
    ruleNodeChangeOwner: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#change-owner-node',
    ruleNodeGenerateReport: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#generate-report-node',
    ruleNodeRestCallReply: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#rest-call-reply-node',
    ruleNodeAggregateLatest: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/analytics-nodes/#aggregate-latest-node',
    ruleNodeAggregateStream: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/analytics-nodes/#aggregate-stream-node',
    ruleNodeAlarmsCount: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/analytics-nodes/#alarms-count-node',
    ruleNodeAlarmsCountDeprecated: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/analytics-nodes/#alarms-count-node-deprecated',
    tenants: helpBaseUrl + '/docs/pe/user-guide/ui/tenants',
    tenantProfiles: helpBaseUrl + '/docs/pe/user-guide/ui/tenant-profiles',
    customers: helpBaseUrl + '/docs/pe/user-guide/ui/customers',
    users: helpBaseUrl + '/docs/pe/user-guide/ui/users',
    devices: helpBaseUrl + '/docs/pe/user-guide/ui/devices',
    deviceProfiles: helpBaseUrl + '/docs/pe/user-guide/ui/device-profiles',
    assetProfiles: helpBaseUrl + '/docs/pe/user-guide/ui/asset-profiles',
    edges: helpBaseUrl + '/docs/user-guide/ui/edges',
    assets: helpBaseUrl + '/docs/pe/user-guide/ui/assets',
    entityViews: helpBaseUrl + '/docs/pe/user-guide/ui/entity-views',
    entitiesImport: helpBaseUrl + '/docs/pe/user-guide/bulk-provisioning',
    rulechains: helpBaseUrl + '/docs/pe/user-guide/ui/rule-chains',
    dashboards: helpBaseUrl + '/docs/pe/user-guide/ui/dashboards',
    resources: helpBaseUrl + '/docs/pe/user-guide/ui/resources',
    otaUpdates: helpBaseUrl + '/docs/pe/user-guide/ota-updates',
    widgetsBundles: helpBaseUrl + '/docs/pe/user-guide/ui/widget-library#bundles',
    widgetsConfig:  helpBaseUrl + '/docs/pe/user-guide/ui/dashboards#widget-configuration',
    widgetsConfigTimeseries:  helpBaseUrl + '/docs/pe/user-guide/ui/dashboards#timeseries',
    widgetsConfigLatest: helpBaseUrl +  '/docs/pe/user-guide/ui/dashboards#latest',
    widgetsConfigRpc: helpBaseUrl +  '/docs/pe/user-guide/ui/dashboards#rpc',
    widgetsConfigAlarm: helpBaseUrl +  '/docs/pe/user-guide/ui/dashboards#alarm',
    widgetsConfigStatic: helpBaseUrl +  '/docs/pe/user-guide/ui/dashboards#static',
    ruleNodePushToCloud: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#push-to-cloud',
    ruleNodePushToEdge: helpBaseUrl + '/docs/pe/user-guide/rule-engine-2-0/action-nodes/#push-to-edge',
    converters: helpBaseUrl +  '/docs/user-guide/integrations/#data-converters',
    uplinkConverters: helpBaseUrl +  '/docs/user-guide/integrations/#uplink-data-converter',
    downlinkConverters: helpBaseUrl +  '/docs/user-guide/integrations/#downlink-data-converter',
    integrations: helpBaseUrl +  '/docs/user-guide/integrations',
    integrationHttp: helpBaseUrl +  '/docs/user-guide/integrations/http',
    integrationOceanConnect: helpBaseUrl +  '/docs/user-guide/integrations/ocean-connect',
    integrationSigFox: helpBaseUrl +  '/docs/user-guide/integrations/sigfox',
    integrationThingPark: helpBaseUrl +  '/docs/user-guide/integrations/thingpark',
    integrationThingParkEnterprise: helpBaseUrl +  '/docs/samples/abeeway/tracker',
    integrationTMobileIotCdp: helpBaseUrl +  '/docs/user-guide/integrations/t-mobile-iot-cdp',
    integrationLoriot: helpBaseUrl +  '/docs/user-guide/integrations/loriot',
    integrationMqtt: helpBaseUrl +  '/docs/user-guide/integrations/mqtt',
    integrationAwsIoT: helpBaseUrl +  '/docs/user-guide/integrations/aws-iot',
    integrationAwsSQS: helpBaseUrl +  '/docs/user-guide/integrations/aws-sqs',
    integrationAwsKinesis:  helpBaseUrl +  '/docs/user-guide/integrations/aws-kinesis',
    integrationIbmWatsonIoT: helpBaseUrl +  '/docs/user-guide/integrations/ibm-watson-iot',
    integrationTheThingsNetwork: helpBaseUrl +  '/docs/user-guide/integrations/ttn',
    integrationTheThingsIndustries: helpBaseUrl +  '/docs/user-guide/integrations/tti',
    integrationChirpStack: helpBaseUrl +  '/docs/user-guide/integrations/chirpstack',
    integrationAzureEventHub: helpBaseUrl +  '/docs/user-guide/integrations/azure-event-hub',
    integrationAzureIoTHub: helpBaseUrl +  '/docs/user-guide/integrations/azure-iot-hub',
    integrationOpcUa:  helpBaseUrl +  '/docs/user-guide/integrations/opc-ua',
    integrationUdp:  helpBaseUrl +  '/docs/user-guide/integrations/udp',
    integrationTcp:  helpBaseUrl +  '/docs/user-guide/integrations/tcp',
    integrationKafka:  helpBaseUrl +  '/docs/user-guide/integrations/kafka',
    integrationRabbitmq:  helpBaseUrl +  '/docs/user-guide/integrations/rabbitmq',
    integrationApachePulsar:  helpBaseUrl +  '/docs/user-guide/integrations/apache-pulsar',
    integrationPubsub:  helpBaseUrl +  '/docs/user-guide/integrations/pubsub',
    integrationCoAP:  helpBaseUrl +  '/docs/user-guide/integrations/coap',
    integrationCustom:  helpBaseUrl +  '/docs/user-guide/integrations/custom',
    whiteLabeling: helpBaseUrl +  '/docs/pe/user-guide/white-labeling',
    entityGroups: helpBaseUrl +  '/docs/pe/user-guide/groups',
    customTranslation: helpBaseUrl +  '/docs/pe/user-guide/custom-translation',
    customMenu: helpBaseUrl +  '/docs/pe/user-guide/custom-menu',
    roles: helpBaseUrl + '/docs/pe/user-guide/ui/roles',
    selfRegistration: helpBaseUrl + '/docs/pe/user-guide/self-registration',
    queue: helpBaseUrl + '/docs/pe/user-guide/queue',
    repositorySettings: helpBaseUrl + '/docs/pe/user-guide/version-control/#git-settings-configuration',
    autoCommitSettings: helpBaseUrl + '/docs/pe/user-guide/version-control/#auto-commit',
    twoFactorAuthentication: helpBaseUrl + '/docs/pe/user-guide/two-factor-authentication'
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

export enum DataType {
  STRING = 'STRING',
  LONG = 'LONG',
  BOOLEAN = 'BOOLEAN',
  DOUBLE = 'DOUBLE',
  JSON = 'JSON'
}

export const DataTypeTranslationMap = new Map([
  [DataType.STRING, 'value.string'],
  [DataType.LONG, 'value.integer'],
  [DataType.BOOLEAN, 'value.boolean'],
  [DataType.DOUBLE, 'value.double'],
  [DataType.JSON, 'value.json']
]);

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
        icon: 'mdi:code-json'
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

export const hidePageSizePixelValue = 550;
export const customTranslationsPrefix = 'custom.';
export const i18nPrefix = 'i18n';

export const MODULES_MAP = new InjectionToken<IModulesMap>('ModulesMap');
