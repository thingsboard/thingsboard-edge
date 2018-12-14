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
import './resource-type-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import resourceTypeAutocompleteTemplate from './resource-type-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ResourceTypeAutocompleteDirective($compile, $templateCache, $q, $filter, $translate, securityTypes) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(resourceTypeAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.resource = null;
        scope.resourceSearchText = '';
        scope.resources = null;

        scope.fetchResources = function(searchText) {
            var deferred = $q.defer();
            loadResources();
            var result = $filter('filter')(scope.resources, {'name': searchText});
            deferred.resolve(result);
            return deferred.promise;
        };

        scope.resourceSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = null;
                if (scope.resource) {
                    value = scope.resource.value;
                }
                ngModelCtrl.$setViewValue(value);
            }
        };

        ngModelCtrl.$render = function () {
            loadResources();
            var resource = null;
            if (angular.isDefined(ngModelCtrl.$viewValue) && ngModelCtrl.$viewValue != null) {
                var result = $filter('filter')(scope.resources, {'value': ngModelCtrl.$viewValue}, true);
                if (result && result.length) {
                    resource = result[0];
                }
            }
            scope.resource = resource;
        };

        scope.$watch('resource', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        function loadResources() {
            if (!scope.resources) {
                scope.resources = [];
                for (var resourceType in securityTypes.resource) {
                    var resource = {
                        value: securityTypes.resource[resourceType],
                        name: $translate.instant('permission.resource.display-type.' + securityTypes.resource[resourceType])
                    };
                    scope.resources.push(resource);
                }
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled'
        }
    };
}
