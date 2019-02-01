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
/* eslint-disable import/no-unresolved, import/default */

import integrationTtnTemplate from './integration-ttn.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function IntegrationIbmWatsonIotDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationTtnTemplate);
        element.html(template);

        const hostRegionSuffix = ".thethings.network";
        scope.types = types;
        scope.hostTypes = {
            region: "Region",
            custom: "Custom"
        };

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        scope.$watch('currentHostType', function (newHostType) {
            if (newHostType == scope.hostTypes.region) {
                scope.hostCustom = "";
            } else if (newHostType == scope.hostTypes.custom) {
                scope.hostRegion = "";
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupTtnConfiguration();
        };

        scope.buildHostName = function () {
            scope.configuration.clientConfiguration.host = (scope.currentHostType === scope.hostTypes.region) ? (scope.hostRegion + hostRegionSuffix) : scope.hostCustom;
            scope.configuration.clientConfiguration.customHost = (scope.currentHostType === scope.hostTypes.custom);
        };

        function setupTtnConfiguration() {
            if (!scope.ttnAppIdWatcher) {
                scope.ttnAppIdWatcher = scope.$watch('configuration.clientConfiguration.credentials.username',
                    (newVal) => {
                        if (newVal) {
                            scope.configuration.downlinkTopicPattern = newVal + "/devices/${devId}/down";
                        }
                    }
                );
            }
            if (!scope.configuration.clientConfiguration) {
                scope.configuration.clientConfiguration = {
                    connectTimeoutSec: 10,
                    credentials: {
                    },
                    cleanSession: true
                };
                scope.configuration.clientConfiguration.host = '';
                scope.configuration.clientConfiguration.port = 8883;
                scope.configuration.clientConfiguration.ssl = true;
                scope.configuration.clientConfiguration.credentials.type = types.mqttCredentialTypes.basic.value;
            }
            if (!scope.configuration.topicFilters) {
                scope.configuration.topicFilters = [];
                scope.configuration.topicFilters.push({
                    filter: '+/devices/+/up',
                    qos: 0
                });
            }
            if (!scope.configuration.downlinkTopicPattern) {
                var ttnAppId = scope.configuration.clientConfiguration.credentials.username;
                if (!ttnAppId) {
                    ttnAppId = "";
                }
                scope.configuration.downlinkTopicPattern = ttnAppId + "/devices/${devId}/down";
            }
            scope.currentHostType = (scope.configuration.clientConfiguration.customHost) ? scope.hostTypes.custom : scope.hostTypes.region;
            if (scope.currentHostType === scope.hostTypes.custom) {
                scope.hostCustom = scope.configuration.clientConfiguration.host;
            } else if (scope.currentHostType === scope.hostTypes.region) {
                if (scope.configuration.clientConfiguration.host && scope.configuration.clientConfiguration.host.endsWith(hostRegionSuffix)) {
                    scope.hostRegion = scope.configuration.clientConfiguration.host.slice(0, -hostRegionSuffix.length);
                } else {
                    scope.hostRegion = scope.configuration.clientConfiguration.host;
                }
            }
        }
        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '='
        },
        link: linker
    };
}
