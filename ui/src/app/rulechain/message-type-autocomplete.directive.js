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
import './message-type-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import messageTypeAutocompleteTemplate from './message-type-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function MessageTypeAutocomplete($compile, $templateCache, $q, $filter, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(messageTypeAutocompleteTemplate);
        element.html(template);

        var messageTypeList = [];
        for (var t in types.messageType) {
            var type = types.messageType[t];
            messageTypeList.push(type);
        }

        scope.messageType = null;
        scope.messageTypeSearchText = '';

        scope.fetchMessageTypes = function(searchText) {
            var deferred = $q.defer();
            var result = $filter('filter')(messageTypeList, {'name': searchText});
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([{name: searchText, value: searchText}]);
            }
            return deferred.promise;
        };

        scope.messageTypeSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = null;
                if (scope.messageType) {
                    value = scope.messageType.value;
                }
                ngModelCtrl.$setViewValue(value);
            }
        };

        ngModelCtrl.$render = function () {
            var value = ngModelCtrl.$viewValue;
            if (value) {
                var result = $filter('filter')(messageTypeList, {'value': value}, true);
                if (result && result.length) {
                    scope.messageType = result[0];
                } else {
                    scope.messageType = {
                        name: value,
                        value: value
                    };
                }
            } else {
                scope.messageType = null;
            }
        };

        scope.$watch('messageType', function (newValue, prevValue) {
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
            required:'=ngRequired'
        }
    };
}
