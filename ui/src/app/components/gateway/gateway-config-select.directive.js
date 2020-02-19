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
import './gateway-config-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import gatewaySelectTemplate from './gateway-config-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.gatewayConfigSelect', [])
    .directive('tbGatewayConfigSelect', GatewayConfigSelect)
    .name;

/*@ngInject*/
function GatewayConfigSelect($compile, $templateCache, $mdConstant, $translate, $mdDialog) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        const template = $templateCache.get(gatewaySelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.gateway = null;
        scope.gatewaySearchText = '';

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('gateway', valid);
        };

        function startWatchers() {
            scope.$watch('gateway', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal) && newVal !== null) {
                    scope.updateView();
                }
            });
        }

        scope.gatewayNameSearch = function (gatewaySearchText) {
            return gatewaySearchText ? scope.gatewayList.filter(
                scope.createFilterForGatewayName(gatewaySearchText)) : scope.gatewayList;
        };

        scope.createFilterForGatewayName = function (query) {
            var lowercaseQuery = query.toLowerCase();
            return function filterFn(device) {
                return (device.name.toLowerCase().indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.gateway);
            scope.updateValidity();
            scope.getAccessToken(scope.gateway.id);
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.gateway = ngModelCtrl.$viewValue;
                startWatchers();
            }
        };

        scope.textIsEmpty = function (str) {
            return (!str || 0 === str.length);
        };

        scope.gatewayNameEnter = function ($event) {
            if ($event.keyCode === $mdConstant.KEY_CODE.ENTER) {
                $event.preventDefault();
                let indexRes = scope.gatewayList.findIndex((element) => element.key === scope.gatewaySearchText);
                if (indexRes === -1) {
                    scope.createNewGatewayDialog($event, scope.gatewaySearchText);
                }
            }
        };

        scope.createNewGatewayDialog = function ($event, deviceName) {
            if ($event) {
                $event.stopPropagation();
            }
            var title = $translate.instant('gateway.create-new-gateway');
            var content = $translate.instant('gateway.create-new-gateway-text', {gatewayName: deviceName});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(
                () => {
                    let deviceObj = {
                        name: deviceName,
                        type: "Gateway",
                        additionalInfo: {
                            gateway: true
                        }
                    };
                    scope.createDevice(deviceObj);
                },
                () => {
                    scope.gatewaySearchText = "";
                }
            );
        };
        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            gatewayList: '=?',
            getAccessToken: '=',
            createDevice: '=',
            theForm: '='
        }
    };
}

/* eslint-enable angular/angularelement */
