/*
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* eslint-disable import/no-unresolved, import/default */

import integrationHttpTemplate from './integration-http.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function IntegrationHttpDirective($compile, $templateCache, $translate, $mdExpansionPanel, toast, utils, types, integrationService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationHttpTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;
        scope.headersFilterPanelId = (Math.random()*1000).toFixed(0);

        scope.httpEndpoint = null;

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupHttpConfiguration();
        };

        function setupHttpConfiguration() {
            if (!scope.configuration.baseUrl) {
                scope.configuration.baseUrl = utils.baseUrl();
            }
            scope.httpEndpoint = integrationService.getIntegrationHttpEndpointLink(scope.configuration, scope.integrationType, scope.routingKey);
            if (scope.integrationType == types.integrationType.THINGPARK.value) {
                scope.configuration.downlinkUrl = 'https://api.thingpark.com/thingpark/lrc/rest/downlink';
            }
        }

        scope.integrationBaseUrlChanged = () => {
            if (types.integrationType[scope.integrationType].http) {
                scope.httpEndpoint = integrationService.getIntegrationHttpEndpointLink(scope.configuration, scope.integrationType, scope.routingKey);
            }
        };

        scope.httpEnableSecurityChanged = () => {
            if (scope.configuration.enableSecurity &&
                !scope.configuration.headersFilter) {
                scope.configuration.headersFilter = {};
            } else if (!scope.configuration.enableSecurity) {
                delete scope.configuration.headersFilter;
            }
        };

        scope.thingparkEnableSecurityChanged = () => {
            if (scope.configuration.enableSecurity &&
                !scope.configuration.maxTimeDiffInSeconds) {
                scope.configuration.maxTimeDiffInSeconds = 60;
            }
        };

        scope.onHttpEndpointCopied = function() {
            toast.showSuccess($translate.instant('integration.http-endpoint-url-copied-message'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '=',
            integrationType: '=',
            routingKey: '='
        },
        link: linker
    };
}
