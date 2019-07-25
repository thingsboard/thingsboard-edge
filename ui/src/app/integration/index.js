/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import IntegrationRoutes from './integration.routes';
import {IntegrationController, IntegrationCardController} from './integration.controller';
import IntegrationDirective from './integration.directive';
import IntegrationHttpDirective from './integration-forms/integration-http.directive';
import IntegrationMqttDirective from './integration-forms/integration-mqtt.directive';
import IntegrationOpcUaDirective from './integration-forms/integration-opc-ua.directive';
import IntegrationAwsIotDirective from './integration-forms/integration-aws-iot.directive';
import IntegrationIbmWatsonIotDirective from './integration-forms/integration-ibm-watson-iot.directive';
import IntegrationTtnDirective from './integration-forms/integration-ttn.directive';
import MqttTopicFiltersDirective from './integration-forms/mqtt-topic-filters.directive';
import OpcUaSubscriptionTagsDirective from './integration-forms/opc-ua-subscription-tags.directive';
import IntegrationAzureEventHubDirective from './integration-forms/integration-azure-event-hub.directive';
import IntegrationCustomDirective from './integration-forms/integration-custom.directive';

export default angular.module('thingsboard.integration', [])
    .config(IntegrationRoutes)
    .controller('IntegrationController', IntegrationController)
    .controller('IntegrationCardController', IntegrationCardController)
    .directive('tbIntegration', IntegrationDirective)
    .directive('tbIntegrationHttp', IntegrationHttpDirective)
    .directive('tbIntegrationMqtt', IntegrationMqttDirective)
    .directive('tbIntegrationOpcUa', IntegrationOpcUaDirective)
    .directive('tbIntegrationAwsIot', IntegrationAwsIotDirective)
    .directive('tbIntegrationIbmWatsonIot', IntegrationIbmWatsonIotDirective)
    .directive('tbIntegrationTtn', IntegrationTtnDirective)
    .directive('tbMqttTopicFilters', MqttTopicFiltersDirective)
    .directive('tbOpcUaSubscriptionTags', OpcUaSubscriptionTagsDirective)
    .directive('tbIntegrationAzureEventHub', IntegrationAzureEventHubDirective)
    .directive('tbIntegrationCustom', IntegrationCustomDirective)
    .name;
