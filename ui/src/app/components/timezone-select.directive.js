/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import './timezone-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import timezoneSelectTemplate from './timezone-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.timezoneSelect', [])
    .directive('tbTimezoneSelect', TimezoneSelect)
    .name;

/*@ngInject*/
function TimezoneSelect($compile, $templateCache, $q, $filter) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(timezoneSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.timezone = null;
        scope.timezoneSearchText = '';

        scope.timezones = null;

        if (scope.defaultTimezoneId) {
            if (!scope.timezones) {
                loadTimezones();
            }
            var result = $filter('filter')(scope.timezones, {id: scope.defaultTimezoneId}, true);
            if (result && result.length) {
                scope.defaultTimezone = result[0];
            }
        }

        scope.fetchTimezones = function(searchText) {
            var deferred = $q.defer();

            if (!scope.timezones) {
                loadTimezones();
            }

            var result = $filter('filter')(scope.timezones, {name: searchText});
            deferred.resolve(result);

            return deferred.promise;
        };

        scope.timezoneSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.timezone ? scope.timezone.id : null);
            }
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                if (!scope.timezones) {
                    loadTimezones();
                }
                var result = $filter('filter')(scope.timezones, {id: ngModelCtrl.$viewValue}, true);
                if (result && result.length) {
                    scope.timezone = result[0];
                } else {
                    scope.timezone = defaultTimezone();
                    scope.updateView();
                }
            } else {
                scope.timezone = defaultTimezone();
                scope.updateView();
            }
        };

        scope.$watch('timezone', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                scope.updateView();
            }
        });

        scope.onBlur = function() {
            if (!scope.timezone) {
                scope.timezone = defaultTimezone();
            }
        }

        function loadTimezones() {
            scope.timezones = [];
            moment.tz.names().forEach(function (zoneName) { //eslint-disable-line
                var tz = moment.tz(zoneName); //eslint-disable-line
                scope.timezones.push({
                    id: zoneName,
                    name: zoneName.replace(/_/g, ' '),
                    offset: 'UTC' + tz.format('Z'),
                    nOffset: tz.utcOffset()
                });
            });
        }

        function defaultTimezone() {
            if (scope.defaultTimezone) {
                return scope.defaultTimezone;
            } else {
                return null;
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            disabled:'=ngDisabled',
            defaultTimezoneId:'=?defaultTimezone'
        }
    };
}
