/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import './entity-group-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityGroupAutocompleteTemplate from './entity-group-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupAutocompleteDirective($compile, $templateCache, $q, $filter, entityGroupService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityGroupAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entityGroup = null;
        scope.entityGroupSearchText = '';

        scope.allEntityGroups = null;

        scope.fetchEntityGroups = function(searchText) {
            var deferred = $q.defer();
            if (!scope.allEntityGroups) {
                entityGroupService.getTenantEntityGroups(scope.groupType).then(
                    function success(entityGroups) {
                        if (scope.excludeGroupAll) {
                            scope.allEntityGroups = $filter('filter')(entityGroups, {groupAll: false});
                        } else {
                            scope.allEntityGroups = entityGroups;
                        }
                        if (scope.excludeGroupIds) {
                            scope.excludeGroupIds.forEach((excludeId) => {
                                var toExclude = $filter('filter')(scope.allEntityGroups, {id: {id: excludeId}}, true);
                                if (toExclude && toExclude.length) {
                                    var index = scope.allEntityGroups.indexOf(toExclude[0]);
                                    if (index > -1) {
                                        scope.allEntityGroups.splice(index, 1);
                                    }
                                }
                            });
                        }
                        filterEntityGroups(searchText, deferred);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                filterEntityGroups(searchText, deferred);
            }
            return deferred.promise;
        }

        function filterEntityGroups(searchText, deferred) {
            var result = $filter('filter')(scope.allEntityGroups, {name: searchText});
            deferred.resolve(result);
        }

        scope.entityGroupSearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.entityGroup ? scope.entityGroup.id.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                entityGroupService.getEntityGroup(ngModelCtrl.$viewValue).then(
                    function success(entityGroup) {
                        scope.entityGroup = entityGroup;
                    },
                    function fail() {
                        scope.entityGroup = null;
                    }
                );
            } else {
                scope.entityGroup = null;
            }
        }

        scope.$watch('entityGroup', function () {
            scope.updateView();
        });

        scope.$watch('groupType', function (newGroupType, prevGroupType) {
            if (!angular.equals(newGroupType, prevGroupType)) {
                scope.allEntityGroups = null;
                scope.entityGroup = null;
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            groupType: '=',
            theForm: '=?',
            tbRequired: '=?',
            excludeGroupIds: '=?',
            excludeGroupAll: '=?',
            disabled:'=ngDisabled',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@'
        }
    };
}
