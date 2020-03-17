/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
var ruleNodeClazzHelpLinkMap = {
    'org.thingsboard.rule.engine.filter.TbCheckRelationNode': 'ruleNodeCheckRelation',
    'org.thingsboard.rule.engine.filter.TbJsFilterNode': 'ruleNodeJsFilter',
    'org.thingsboard.rule.engine.filter.TbJsSwitchNode': 'ruleNodeJsSwitch',
    'org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode': 'ruleNodeMessageTypeFilter',
    'org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode': 'ruleNodeMessageTypeSwitch',
    'org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode': 'ruleNodeOriginatorTypeFilter',
    'org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode': 'ruleNodeOriginatorTypeSwitch',
    'org.thingsboard.rule.engine.metadata.TbGetAttributesNode': 'ruleNodeOriginatorAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetOriginatorFieldsNode': 'ruleNodeOriginatorFields',
    'org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode': 'ruleNodeCustomerAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetDeviceAttrNode': 'ruleNodeDeviceAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode': 'ruleNodeRelatedAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetTenantAttributeNode': 'ruleNodeTenantAttributes',
    'org.thingsboard.rule.engine.transform.TbChangeOriginatorNode': 'ruleNodeChangeOriginator',
    'org.thingsboard.rule.engine.transform.TbTransformMsgNode': 'ruleNodeTransformMsg',
    'org.thingsboard.rule.engine.mail.TbMsgToEmailNode': 'ruleNodeMsgToEmail',
    'org.thingsboard.rule.engine.action.TbClearAlarmNode': 'ruleNodeClearAlarm',
    'org.thingsboard.rule.engine.action.TbCreateAlarmNode': 'ruleNodeCreateAlarm',
    'org.thingsboard.rule.engine.delay.TbMsgDelayNode': 'ruleNodeMsgDelay',
    'org.thingsboard.rule.engine.debug.TbMsgGeneratorNode': 'ruleNodeMsgGenerator',
    'org.thingsboard.rule.engine.action.TbLogNode': 'ruleNodeLog',
    'org.thingsboard.rule.engine.rpc.TbSendRPCReplyNode': 'ruleNodeRpcCallReply',
    'org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode': 'ruleNodeRpcCallRequest',
    'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode': 'ruleNodeSaveAttributes',
    'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode': 'ruleNodeSaveTimeseries',
    'tb.internal.RuleChain': 'ruleNodeRuleChain',
    'org.thingsboard.rule.engine.aws.sns.TbSnsNode': 'ruleNodeAwsSns',
    'org.thingsboard.rule.engine.aws.sqs.TbSqsNode': 'ruleNodeAwsSqs',
    'org.thingsboard.rule.engine.kafka.TbKafkaNode': 'ruleNodeKafka',
    'org.thingsboard.rule.engine.mqtt.TbMqttNode': 'ruleNodeMqtt',
    'org.thingsboard.rule.engine.rabbitmq.TbRabbitMqNode': 'ruleNodeRabbitMq',
    'org.thingsboard.rule.engine.rest.TbRestApiCallNode': 'ruleNodeRestApiCall',
    'org.thingsboard.rule.engine.mail.TbSendEmailNode': 'ruleNodeSendEmail',
    'org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode': 'ruleNodeIntegrationDownlink',
    'org.thingsboard.rule.engine.action.TbAddToGroupNode': 'ruleNodeAddToGroup',
    'org.thingsboard.rule.engine.action.TbRemoveFromGroupNode': 'ruleNodeRemoveFromGroup',
    'org.thingsboard.rule.engine.transform.TbDuplicateMsgToGroupNode': 'ruleNodeDuplicateToGroup',
    'org.thingsboard.rule.engine.transform.TbDuplicateMsgToRelatedNode': 'ruleNodeDuplicateToRelated',
    'org.thingsboard.rule.engine.report.TbGenerateReportNode': 'ruleNodeGenerateReport',
    'org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode': 'ruleNodeRestCallReply',
    'org.thingsboard.rule.engine.analytics.latest.telemetry.TbAggLatestTelemetryNode': 'ruleNodeAggregateLatest',
    'org.thingsboard.rule.engine.analytics.incoming.TbSimpleAggMsgNode': 'ruleNodeAggregateStream',
    'org.thingsboard.rule.engine.analytics.latest.alarm.TbAlarmsCountNode': 'ruleNodeAlarmsCount'
};

var integrationTypeHelpLinkMap = {
    'HTTP': 'integrationHttp',
    'OCEANCONNECT': 'integrationOceanConnect',
    'SIGFOX': 'integrationSigFox',
    'THINGPARK': 'integrationThingPark',
    'MQTT': 'integrationMqtt',
    'AWS_IOT': 'integrationAwsIoT',
    'AWS_KINESIS': 'integrationAwsKinesis',
    'IBM_WATSON_IOT': 'integrationIbmWatsonIoT',
    'TTN': 'integrationTheThingsNetwork',
    'AZURE_EVENT_HUB': 'integrationAzureEventHub',
    'OPC_UA': 'integrationOpcUa'
};

var helpBaseUrl = "https://thingsboard.io";

export default angular.module('thingsboard.help', [])
    .constant('helpLinks',
        {
            linksMap: {
                docs: helpBaseUrl + "/docs",
                outgoingMailSettings: helpBaseUrl + "/docs/user-guide/ui/mail-settings",
                securitySettings: helpBaseUrl + "/docs/user-guide/ui/security-settings",
                ruleEngine: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/overview/",
                ruleNodeCheckRelation: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node",
                ruleNodeJsFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node",
                ruleNodeJsSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#switch-node",
                ruleNodeMessageTypeFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node",
                ruleNodeMessageTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node",
                ruleNodeOriginatorTypeFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node",
                ruleNodeOriginatorTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node",
                ruleNodeOriginatorAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes",
                ruleNodeOriginatorFields: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields",
                ruleNodeCustomerAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes",
                ruleNodeDeviceAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes",
                ruleNodeRelatedAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes",
                ruleNodeTenantAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes",
                ruleNodeChangeOriginator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#change-originator",
                ruleNodeTransformMsg: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node",
                ruleNodeMsgToEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node",
                ruleNodeClearAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node",
                ruleNodeCreateAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node",
                ruleNodeMsgDelay: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#delay-node",
                ruleNodeMsgGenerator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#generator-node",
                ruleNodeLog: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#log-node",
                ruleNodeRpcCallReply: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node",
                ruleNodeRpcCallRequest: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node",
                ruleNodeSaveAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node",
                ruleNodeSaveTimeseries: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node",
                ruleNodeRuleChain: helpBaseUrl + "/docs/user-guide/ui/rule-chains/",
                ruleNodeAwsSns: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node",
                ruleNodeAwsSqs: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node",
                ruleNodeKafka: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#kafka-node",
                ruleNodeMqtt: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#mqtt-node",
                ruleNodeRabbitMq: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node",
                ruleNodeRestApiCall: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node",
                ruleNodeSendEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#send-email-node",
                ruleNodeIntegrationDownlink: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/action-nodes/#integration-downlink-node",
                ruleNodeAddToGroup: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/action-nodes/#add-to-group-node",
                ruleNodeRemoveFromGroup: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/action-nodes/#remove-from-group-node",
                ruleNodeDuplicateToGroup: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/transformation-nodes/#duplicate-to-group-node",
                ruleNodeDuplicateToRelated: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/transformation-nodes/#duplicate-to-related-node",
                ruleNodeGenerateReport: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/action-nodes/#generate-report-node",
                ruleNodeRestCallReply: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/action-nodes/#rest-call-reply-node",
                ruleNodeAggregateLatest: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#aggregate-latest-node",
                ruleNodeAggregateStream: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#aggregate-stream-node",
                ruleNodeAlarmsCount: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/pe/analytics-nodes/#alarms-count-node",
                rulechains: helpBaseUrl + "/docs/user-guide/ui/rule-chains/",
                tenants: helpBaseUrl + "/docs/user-guide/ui/tenants",
                customers: helpBaseUrl + "/docs/user-guide/ui/customers",
                assets: helpBaseUrl + "/docs/user-guide/ui/assets",
                devices: helpBaseUrl + "/docs/user-guide/ui/devices",
                entityViews: helpBaseUrl + "/docs/user-guide/ui/entity-views",
                entitiesImport: helpBaseUrl + "/docs/user-guide/bulk-provisioning",
                dashboards: helpBaseUrl + "/docs/user-guide/ui/dashboards",
                users: helpBaseUrl + "/docs/user-guide/ui/users",
                widgetsBundles: helpBaseUrl + "/docs/user-guide/ui/widget-library#bundles",
                widgetsConfig:  helpBaseUrl + "/docs/user-guide/ui/dashboards#widget-configuration",
                widgetsConfigTimeseries:  helpBaseUrl + "/docs/user-guide/ui/dashboards#timeseries",
                widgetsConfigLatest: helpBaseUrl +  "/docs/user-guide/ui/dashboards#telemetry",
                widgetsConfigRpc: helpBaseUrl +  "/docs/user-guide/ui/dashboards#rpc",
                widgetsConfigAlarm: helpBaseUrl +  "/docs/user-guide/ui/dashboards#alarm",
                widgetsConfigStatic: helpBaseUrl +  "/docs/user-guide/ui/dashboards#static",
                converters: helpBaseUrl +  "/docs/user-guide/integrations/#data-converters",
                uplinkConverters: helpBaseUrl +  "/docs/user-guide/integrations/#uplink-data-converter",
                downlinkConverters: helpBaseUrl +  "/docs/user-guide/integrations/#downlink-data-converter",
                integrations: helpBaseUrl +  "/docs/user-guide/integrations",
                integrationHttp: helpBaseUrl +  "/docs/user-guide/integrations/http",
                integrationOceanConnect: helpBaseUrl +  "/docs/user-guide/integrations/ocean-connect",
                integrationSigFox: helpBaseUrl +  "/docs/user-guide/integrations/sigfox",
                integrationThingPark: helpBaseUrl +  "/docs/user-guide/integrations/thingpark",
                integrationMqtt: helpBaseUrl +  "/docs/user-guide/integrations/mqtt",
                integrationAwsIoT: helpBaseUrl +  "/docs/user-guide/integrations/aws-iot",
                integrationAwsKinesis:  helpBaseUrl +  "/docs/user-guide/integrations/aws-kinesis",
                integrationIbmWatsonIoT: helpBaseUrl +  "/docs/user-guide/integrations/ibm-watson-iot",
                integrationTheThingsNetwork: helpBaseUrl +  "/docs/user-guide/integrations/ttn",
                integrationAzureEventHub: helpBaseUrl +  "/docs/user-guide/integrations/azure-event-hub",
                integrationOpcUa:  helpBaseUrl +  "/docs/user-guide/integrations/opc-ua",
                whiteLabeling: helpBaseUrl +  "/docs/user-guide/white-labeling",
                entityGroups: helpBaseUrl +  "/docs/user-guide/groups",
                customTranslation: helpBaseUrl +  "/docs/user-guide/custom-translation",
                customMenu: helpBaseUrl +  "/docs/user-guide/custom-menu",
                roles: helpBaseUrl + "/docs/user-guide/ui/roles",
                selfRegistration: helpBaseUrl + "/docs/user-guide/self-registration",
            },
            getRuleNodeLink: function(ruleNode) {
                if (ruleNode && ruleNode.component) {
                    if (ruleNode.component.configurationDescriptor &&
                        ruleNode.component.configurationDescriptor.nodeDefinition &&
                        ruleNode.component.configurationDescriptor.nodeDefinition.docUrl) {
                        return ruleNode.component.configurationDescriptor.nodeDefinition.docUrl;
                    } else if (ruleNode.component.clazz) {
                        if (ruleNodeClazzHelpLinkMap[ruleNode.component.clazz]) {
                            return ruleNodeClazzHelpLinkMap[ruleNode.component.clazz];
                        }
                    }
                }
                return 'ruleEngine';
            },
            getIntegrationLink: function(integration) {
                var link = 'integrations';
                if (integration && integration.type) {
                    if (integrationTypeHelpLinkMap[integration.type]) {
                        link = integrationTypeHelpLinkMap[integration.type];
                    }
                }
                return link;
            },
            getConverterLink: function(converter) {
                var link = 'converters';
                if (converter && converter.type) {
                    if (converter.type === 'UPLINK') {
                        link = 'uplinkConverters';
                    } else {
                        link = 'downlinkConverters';
                    }
                }
                return link;
            }
        }
    ).name;
