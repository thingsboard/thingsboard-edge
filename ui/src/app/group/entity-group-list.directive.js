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

/* eslint-disable import/no-unresolved, import/default */

import entityGroupListTemplate from './entity-group-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-group-list.scss';

/*@ngInject*/
export default function EntityGroupListDirective($compile, $templateCache, $q, $mdUtil, types, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityGroupListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.$watch('tbRequired', function () {
            scope.updateValidity();
        });

        scope.fetchEntityGroups = function(searchText, limit) {
            if (scope.groupType) {
                var deferred = $q.defer();
                entityService.getEntitiesByNameFilter(types.entityType.entityGroup, searchText, limit, null, scope.groupType).then(
                    function success(result) {
                        if (result) {
                            deferred.resolve(result);
                        } else {
                            deferred.resolve([]);
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                return deferred.promise;
            } else {
                return $q.when([]);
            }
        }

        scope.updateValidity = function() {
            var value = ngModelCtrl.$viewValue;
            var valid = !scope.tbRequired || value && value.length > 0;
            ngModelCtrl.$setValidity('entityGroupList', valid);
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            var value = ngModelCtrl.$viewValue;
            scope.entityGroupList = [];
            if (value && value.length > 0) {
                entityService.getEntities(types.entityType.entityGroup, value).then(function (entities) {
                    scope.entityGroupList = entities;
                    initWatchers();
                });
            } else {
                initWatchers();
            }
        }

        function initWatchers() {
            scope.groupTypeDeregistration = scope.$watch('groupType', function (newGroupType, prevGroupType) {
                if (!angular.equals(newGroupType, prevGroupType)) {
                    scope.entityGroupList = [];
                }
            });
            scope.entityGroupListDeregistration = scope.$watch('entityGroupList', function () {
                var ids = [];
                if (scope.entityGroupList && scope.entityGroupList.length > 0) {
                    for (var i=0;i<scope.entityGroupList.length;i++) {
                        ids.push(scope.entityGroupList[i].id.id);
                    }
                }
                var value = ngModelCtrl.$viewValue;
                if (!angular.equals(ids, value)) {
                    ngModelCtrl.$setViewValue(ids);
                }
                scope.updateValidity();
            }, true);
        }

        function destroyWatchers() {
            if (scope.groupTypeDeregistration) {
                scope.groupTypeDeregistration();
                scope.groupTypeDeregistration = null;
            }
            if (scope.entityGroupListDeregistration) {
                scope.entityGroupListDeregistration();
                scope.entityGroupListDeregistration = null;
            }
        }

        $compile(element.contents())(scope);

        $mdUtil.nextTick(function(){
            var inputElement = angular.element('input', element);
            inputElement.on('blur', function() {
                scope.inputTouched = true;
            } );
        });

    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            disabled:'=ngDisabled',
            tbRequired: '=?',
            groupType: '='
        }
    };

}
