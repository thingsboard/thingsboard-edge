/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import integrationHttpTemplate from './integration-http.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './integration-http.scss';

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
                if (!scope.configuration.downlinkUrl) {
                    scope.configuration.downlinkUrl = 'https://api.thingpark.com/thingpark/lrc/rest/downlink';
                }
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
            toast.showSuccess($translate.instant('integration.http-endpoint-url-copied-message'), 750, angular.element(element).parent().parent().parent(), 'bottom left');
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
