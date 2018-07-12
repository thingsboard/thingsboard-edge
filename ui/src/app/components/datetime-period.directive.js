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
import './datetime-period.scss';

/* eslint-disable import/no-unresolved, import/default */

import datetimePeriodTemplate from './datetime-period.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.datetimePeriod', [])
    .directive('tbDatetimePeriod', DatetimePeriod)
    .name;

/*@ngInject*/
function DatetimePeriod($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(datetimePeriodTemplate);
        element.html(template);

        ngModelCtrl.$render = function () {
            var date = new Date();
            scope.startDate = new Date(
                date.getFullYear(),
                date.getMonth(),
                date.getDate() - 1);
            scope.endDate = date;
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.startDate = new Date(value.startTimeMs);
                scope.endDate = new Date(value.endTimeMs);
            }
        }

        scope.updateMinMaxDates = function () {
            scope.maxStartDate = angular.copy(new Date(scope.endDate.getTime() - 1000));
            scope.minEndDate = angular.copy(new Date(scope.startDate.getTime() + 1000));
            scope.maxEndDate = new Date();
        }

        scope.updateView = function () {
            var value = null;
            if (scope.startDate && scope.endDate) {
                value = {
                    startTimeMs: scope.startDate.getTime(),
                    endTimeMs: scope.endDate.getTime()
                };
                ngModelCtrl.$setValidity('datetimePeriod', true);
            } else {
                ngModelCtrl.$setValidity('datetimePeriod', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
        }

        scope.$watch('required', function () {
            scope.updateView();
        });

        scope.$watch('startDate', function (newDate) {
            if (newDate) {
                if (newDate.getTime() > scope.maxStartDate) {
                    scope.startDate = angular.copy(scope.maxStartDate);
                }
                scope.updateMinMaxDates();
            }
            scope.updateView();
        });

        scope.$watch('endDate', function (newDate) {
            if (newDate) {
                if (newDate.getTime() < scope.minEndDate) {
                    scope.endDate = angular.copy(scope.minEndDate);
                } else if (newDate.getTime() > scope.maxEndDate) {
                    scope.endDate = angular.copy(scope.maxEndDate);
                }
                scope.updateMinMaxDates();
            }
            scope.updateView();
        });

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            required: '=ngRequired'
        },
        link: linker
    };
}
