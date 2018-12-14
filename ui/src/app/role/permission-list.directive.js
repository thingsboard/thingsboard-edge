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
import './permission-list.scss';

/* eslint-disable import/no-unresolved, import/default */

import permissionListTemplate from './permission-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function PermissionListDirective($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(permissionListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.removePermission = removePermission;
        scope.addPermission = addPermission;

        scope.permissionList = [];

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.permissionList.length = 0;
                for (var key in value) {
                    scope.permissionList.push({
                        resource: key,
                        operations: value[key]
                    });
                }
            }
            scope.$watch('permissionList', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    updateValue();
                }
            }, true);
            updateValidity();
        };

        function removePermission(index) {
            if (index > -1) {
                scope.permissionList.splice(index, 1);
            }
        }

        function addPermission() {
            if (!scope.permissionList) {
                scope.permissionList = [];
            }
            scope.permissionList.push(
                {
                    resource: null,
                    operations: []
                }
            );
        }

        function updateValue() {
            var value = {};
            scope.permissionList.forEach(function (permissionEntry) {
                if (permissionEntry.resource) {
                    value[permissionEntry.resource] = permissionEntry.operations;
                }
            });
            ngModelCtrl.$setViewValue(value);
            updateValidity();
        }

        function updateValidity() {
            var permissionMapValid = true;
            if (scope.required && !scope.permissionList.length) {
                permissionMapValid = false;
            }
            ngModelCtrl.$setValidity('permissionMap', permissionMapValid);
        }

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            required:'=ngRequired',
            disabled:'=ngDisabled'
        },
        link: linker
    };
}
