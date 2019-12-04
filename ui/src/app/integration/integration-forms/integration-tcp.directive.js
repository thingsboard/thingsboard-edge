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
import integrationTcpTemplate from './integration-tcp.tpl.html';

/*@ngInject*/
export default function IntegrationTcpDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationTcpTemplate);
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
            setupTcpConfiguration();
        };

        function setupTcpConfiguration() {
            setupDefaultHandlerConfigurations();
            if (!scope.configuration.clientConfiguration) {
                scope.configuration.clientConfiguration = {
                    port: 10560,
                    soBacklogOption: 128,
                    soRcvBuf: 64,
                    soSndBuf: 64,
                    soKeepaliveOption: false,
                    tcpNoDelay: true,
                    handlerConfiguration: angular.copy(defaultHandlerConfigurations[types.handlerConfigurationTypes.binary.value])
                }
            }
        }

        function setupDefaultHandlerConfigurations() {
            defaultHandlerConfigurations[types.handlerConfigurationTypes.binary.value] = {
                handlerType: types.handlerConfigurationTypes.binary.value,
                byteOrder: types.tcpBinaryByteOrder.littleEndian.value,
                maxFrameLength: 128,
                lengthFieldOffset: 0,
                lengthFieldLength: 2,
                lengthAdjustment: 0,
                initialBytesToStrip: 0,
                failFast: false
            };

            defaultHandlerConfigurations[types.handlerConfigurationTypes.text.value] = {
                handlerType: types.handlerConfigurationTypes.text.value,
                maxFrameLength: 128,
                stripDelimiter: true,
                messageSeparator: types.tcpTextMessageSeparator.systemLineSeparator.value
            };

            defaultHandlerConfigurations[types.handlerConfigurationTypes.json.value] = {
                handlerType: types.handlerConfigurationTypes.json.value
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
