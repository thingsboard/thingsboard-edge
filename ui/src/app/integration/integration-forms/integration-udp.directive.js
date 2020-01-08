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
import integrationUdpTemplate from './integration-udp.tpl.html';

/*@ngInject*/
export default function IntegrationUdpDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationUdpTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;

        var defaultHandlerConfigurations = {};

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupUdpConfiguration();
        };

        function setupUdpConfiguration() {
            setupDefaultHandlerConfigurations();
            if (!scope.configuration.clientConfiguration) {
                scope.configuration.clientConfiguration = {
                    port: 11560,
                    soBroadcast: true,
                    soRcvBuf: 64,
                    handlerConfiguration: angular.copy(defaultHandlerConfigurations[types.handlerConfigurationTypes.binary.value])
                }
            }
        }

        function setupDefaultHandlerConfigurations() {
            defaultHandlerConfigurations[types.handlerConfigurationTypes.binary.value] = {
                handlerType: types.handlerConfigurationTypes.binary.value
            };

            defaultHandlerConfigurations[types.handlerConfigurationTypes.text.value] = {
                handlerType: types.handlerConfigurationTypes.text.value,
                charsetName: 'UTF-8'
            };

            defaultHandlerConfigurations[types.handlerConfigurationTypes.json.value] = {
                handlerType: types.handlerConfigurationTypes.json.value
            };

            defaultHandlerConfigurations[types.handlerConfigurationTypes.hex.value] = {
                handlerType: types.handlerConfigurationTypes.hex.value,
                maxFrameLength: 128
            };

        }

        scope.handlerConfigurationTypeChanged = () => {
            let handlerType = scope.configuration.clientConfiguration.handlerConfiguration.handlerType;
            scope.configuration.clientConfiguration.handlerConfiguration = {};
            scope.configuration.clientConfiguration.handlerConfiguration = angular.copy(defaultHandlerConfigurations[handlerType]);
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '=',
            integrationType: '='
        },
        link: linker
    };
}
