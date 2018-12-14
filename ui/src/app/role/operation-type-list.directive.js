/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import operationTypeListTemplate from './operation-type-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './operation-type-list.scss';

/*@ngInject*/
export default function OperationTypeListDirective($compile, $templateCache, $q, $mdUtil, $translate, $filter, securityTypes) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(operationTypeListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.placeholder = $translate.instant('permission.operation.enter-operation');
        scope.secondaryPlaceholder = '+' + $translate.instant('permission.operation.operation');

        scope.operations = null;

        scope.$watch('tbRequired', function () {
            scope.updateValidity();
        });

        scope.$watch('disabled', function () {
            updateInput();
        });

        scope.fetchOperations = function(searchText) {
            var deferred = $q.defer();
            loadOperations();
            var operations = $filter('filter')(scope.operations, {name: searchText});
            deferred.resolve(operations);
            return deferred.promise;
        };

        scope.updateValidity = function() {
            var value = ngModelCtrl.$viewValue;
            var valid = !scope.tbRequired || value && value.length > 0;
            ngModelCtrl.$setValidity('operationList', valid);
        };

        ngModelCtrl.$render = function () {
            loadOperations();
            if (scope.operationListWatch) {
                scope.operationListWatch();
                scope.operationListWatch = null;
            }
            var operationList = [];
            var value = ngModelCtrl.$viewValue;
            if (value && value.length) {
                value.forEach(function(type) {
                    var result = $filter('filter')(scope.operations, {'value': type}, true);
                    if (result && result.length) {
                        operationList.push(result[0]);
                    }
               });
            }
            scope.operationList = operationList;
            checkOperationTypeAll();
            scope.operationListWatch = scope.$watch('operationList', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    updateOperationList();
                }
            }, true);
        };

        function updateOperationList() {
            checkOperationTypeAll();
            var values = ngModelCtrl.$viewValue;
            if (!values) {
                values = [];
                ngModelCtrl.$setViewValue(values);
            } else {
                values.length = 0;
            }
            if (scope.operationList && scope.operationList.length) {
                scope.operationList.forEach(function (operationType) {
                    values.push(operationType.value);
                });
            }
            scope.updateValidity();
        }

        function checkOperationTypeAll() {
            var result = $filter('filter')(scope.operationList, {'value': securityTypes.operation.all}, true);
            if (result && result.length) {
                scope.secondaryPlaceholder = '';
                if (scope.operationList.length > 1) {
                    scope.operationList = result;
                }
            } else {
                scope.secondaryPlaceholder = '+' + $translate.instant('permission.operation.operation');
            }
        }

        function loadOperations() {
            if (!scope.operations) {
                scope.operations = [];
                for (var operationType in securityTypes.operation) {
                    var operation = {
                        value: securityTypes.operation[operationType],
                        name: $translate.instant('permission.operation.display-type.' + securityTypes.operation[operationType])
                    };
                    scope.operations.push(operation);
                }
            }
        }

        function updateInput() {
            if (!scope.disabled) {
                $mdUtil.nextTick(function () {
                    var inputElement = angular.element('input', element);
                    inputElement.on('blur', function () {
                        scope.inputTouched = true;
                    });
                });
            } else {
                scope.inputTouched = false;
            }
        }

        $compile(element.contents())(scope);


    };

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            disabled:'=ngDisabled',
            tbRequired: '=?'
        }
    };

}
