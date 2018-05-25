/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
    'org.thingsboard.rule.engine.filter.TbJsFilterNode': 'ruleNodeJsFilter',
    'org.thingsboard.rule.engine.filter.TbJsSwitchNode': 'ruleNodeJsSwitch',
    'org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode': 'ruleNodeMessageTypeFilter',
    'org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode': 'ruleNodeMessageTypeSwitch',
    'org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode': 'ruleNodeOriginatorTypeSwitch',
    'org.thingsboard.rule.engine.metadata.TbGetAttributesNode': 'ruleNodeOriginatorAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode': 'ruleNodeCustomerAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetDeviceAttrNode': 'ruleNodeDeviceAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode': 'ruleNodeRelatedAttributes',
    'org.thingsboard.rule.engine.metadata.TbGetTenantAttributeNode': 'ruleNodeTenantAttributes',
    'org.thingsboard.rule.engine.transform.TbChangeOriginatorNode': 'ruleNodeChangeOriginator',
    'org.thingsboard.rule.engine.transform.TbTransformMsgNode': 'ruleNodeTransformMsg',
    'org.thingsboard.rule.engine.mail.TbMsgToEmailNode': 'ruleNodeMsgToEmail',
    'org.thingsboard.rule.engine.action.TbClearAlarmNode': 'ruleNodeClearAlarm',
    'org.thingsboard.rule.engine.action.TbCreateAlarmNode': 'ruleNodeCrateAlarm',
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
    'org.thingsboard.rule.engine.mail.TbSendEmailNode': 'ruleNodeSendEmail'
};

var integrationTypeHelpLinkMap = {
    'HTTP': 'integrationHttp',
    'OCEANCONNECT': 'integrationOceanConnect',
    'SIGFOX': 'integrationSigFox',
    'THINGPARK': 'integrationThingPark',
    'MQTT': 'integrationMqtt',
    'AWS_IOT': 'integrationAwsIoT',
    'IBM_WATSON_IOT': 'integrationIbmWatsonIoT',
    'TTN': 'integrationTheThingsNetwork',
    'AZURE_EVENT_HUB': 'integrationAzureEventHub'
};

var helpBaseUrl = "https://thingsboard.io";

export default angular.module('thingsboard.help', [])
    .constant('helpLinks',
        {
            linksMap: {
                docs: helpBaseUrl + "/docs",
                outgoingMailSettings: helpBaseUrl + "/docs/user-guide/ui/mail-settings",
                ruleEngine: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/overview/",
                ruleNodeJsFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node",
                ruleNodeJsSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#switch-node",
                ruleNodeMessageTypeFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node",
                ruleNodeMessageTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node",
                ruleNodeOriginatorTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node",
                ruleNodeOriginatorAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes",
                ruleNodeCustomerAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes",
                ruleNodeDeviceAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes",
                ruleNodeRelatedAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes",
                ruleNodeTenantAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes",
                ruleNodeChangeOriginator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#change-originator",
                ruleNodeTransformMsg: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node",
                ruleNodeMsgToEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node",
                ruleNodeClearAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node",
                ruleNodeCrateAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node",
                ruleNodeMsgGenerator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#generator-node",
                ruleNodeLog: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#log-node",
                ruleNodeRpcCallReply: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node",
                ruleNodeRpcCallRequest: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node",
                ruleNodeSaveAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node",
                ruleNodeSaveTimeseries: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node",
                ruleNodeRuleChain: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/rule-chains/",
                ruleNodeAwsSns: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node",
                ruleNodeAwsSqs: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node",
                ruleNodeKafka: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#kafka-node",
                ruleNodeMqtt: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#mqtt-node",
                ruleNodeRabbitMq: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node",
                ruleNodeRestApiCall: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node",
                ruleNodeSendEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#send-email-node",
                rulechains: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/rule-chains/",
                tenants: helpBaseUrl + "/docs/user-guide/ui/tenants",
                customers: helpBaseUrl + "/docs/user-guide/ui/customers",
                assets: helpBaseUrl + "/docs/user-guide/ui/assets",
                devices: helpBaseUrl + "/docs/user-guide/ui/devices",
                dashboards: helpBaseUrl + "/docs/user-guide/ui/dashboards",
                users: helpBaseUrl + "/docs/user-guide/ui/users",
                widgetsBundles: helpBaseUrl + "/docs/user-guide/ui/widget-library#bundles",
                widgetsConfig:  helpBaseUrl + "/docs/user-guide/ui/dashboards#widget-configuration",
                widgetsConfigTimeseries:  helpBaseUrl + "/docs/user-guide/ui/dashboards#timeseries",
                widgetsConfigLatest: helpBaseUrl +  "/docs/user-guide/ui/dashboards#latest",
                widgetsConfigRpc: helpBaseUrl +  "/docs/user-guide/ui/dashboards#rpc",
                widgetsConfigAlarm: helpBaseUrl +  "/docs/user-guide/ui/dashboards#alarm",
                widgetsConfigStatic: helpBaseUrl +  "/docs/user-guide/ui/dashboards#static",
                converters: helpBaseUrl +  "/docs/user-guide/data-converters",
                integrations: helpBaseUrl +  "/docs/user-guide/integrations",
                integrationHttp: helpBaseUrl +  "/docs/user-guide/integrations/http",
                integrationOceanConnect: helpBaseUrl +  "/docs/user-guide/integrations/ocean-connect",
                integrationSigFox: helpBaseUrl +  "/docs/user-guide/integrations/sigfox",
                integrationThingPark: helpBaseUrl +  "/docs/user-guide/integrations/thingpark",
                integrationMqtt: helpBaseUrl +  "/docs/user-guide/integrations/mqtt",
                integrationAwsIoT: helpBaseUrl +  "/docs/user-guide/integrations/aws-iot",
                integrationIbmWatsonIoT: helpBaseUrl +  "/docs/user-guide/integrations/ibm-watson-iot",
                integrationTheThingsNetwork: helpBaseUrl +  "/docs/user-guide/integrations/ttn",
                integrationAzureEventHub: helpBaseUrl +  "/docs/user-guide/integrations/azure-event-hub",
                whiteLabeling: helpBaseUrl +  "/docs/user-guide/white-labeling",
                entityGroups: helpBaseUrl +  "/docs/user-guide/groups",
            },
            getRuleNodeLink: function(ruleNode) {
                var link = 'ruleEngine';
                if (ruleNode && ruleNode.component) {
                    if (ruleNode.component.configurationDescriptor &&
                        ruleNode.component.configurationDescriptor.nodeDefinition &&
                        ruleNode.component.configurationDescriptor.nodeDefinition.docUrl) {
                        link = ruleNode.component.configurationDescriptor.nodeDefinition.docUrl;
                    } else if (ruleNode && ruleNode.component && ruleNode.component.clazz) {
                        if (ruleNodeClazzHelpLinkMap[ruleNode.component.clazz]) {
                            link = ruleNodeClazzHelpLinkMap[ruleNode.component.clazz];
                        }
                    }
                }
                return link;
            },
            getIntegrationLink: function(integration) {
                var link = 'integrations';
                if (integration && integration.type) {
                    if (integrationTypeHelpLinkMap[integration.type]) {
                        link = integrationTypeHelpLinkMap[integration.type];
                    }
                }
                return link;
            }
        }
    ).name;
