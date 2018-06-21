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
import './scheduler-event-type-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import schedulerEventTypeAutocompleteTemplate from './scheduler-event-type-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function SchedulerEventTypeAutocomplete($compile, $templateCache, $q, $filter) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(schedulerEventTypeAutocompleteTemplate);
        element.html(template);

        scope.eventType = null;
        scope.eventTypeSearchText = '';

        scope.fetchEventTypes = function(searchText) {
            var deferred = $q.defer();
            var result = $filter('filter')(scope.eventTypes, {'name': searchText});
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([{name: searchText, value: searchText}]);
            }
            return deferred.promise;
        };

        scope.eventTypeSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = null;
                if (scope.eventType) {
                    value = scope.eventType.value;
                }
                ngModelCtrl.$setViewValue(value);
            }
        };

        ngModelCtrl.$render = function () {
            var value = ngModelCtrl.$viewValue;
            if (value) {
                var result = $filter('filter')(scope.eventTypes, {'value': value}, true);
                if (result && result.length) {
                    scope.eventType = result[0];
                } else {
                    scope.eventType = {
                        name: value,
                        value: value
                    };
                }
            } else {
                scope.eventType = null;
            }
        };

        scope.$watch('eventType', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            disabled:'=ngDisabled',
            eventTypes:'='
        }
    };
}
