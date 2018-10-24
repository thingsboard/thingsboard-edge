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
import './entity-group-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityGroupAutocompleteTemplate from './entity-group-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupAutocompleteDirective($compile, $templateCache, $q, $filter, entityGroupService, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityGroupAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entityGroup = null;
        scope.entityGroupSearchText = '';

        scope.utils = utils;

        scope.allEntityGroups = null;

        scope.fetchEntityGroups = function(searchText) {
            var deferred = $q.defer();
            if (!scope.allEntityGroups) {
                entityGroupService.getTenantEntityGroups(scope.groupType, false, {ignoreLoading: true}).then(
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
                        if (scope.onEntityGroupLoaded) {
                            scope.onEntityGroupLoaded({entityGroup: entityGroup});
                        }
                    },
                    function fail() {
                        scope.entityGroup = null;
                        if (scope.onEntityGroupLoaded) {
                            scope.onEntityGroupLoaded({entityGroup: null});
                        }
                    }
                );
            } else {
                scope.entityGroup = null;
                if (scope.onEntityGroupLoaded) {
                    scope.onEntityGroupLoaded({entityGroup: null});
                }
            }
        }

        scope.$watch('entityGroup', function (newEntityGroup, prevEntityGroup) {
            if (!angular.equals(newEntityGroup, prevEntityGroup)) {
                scope.updateView();
            }
        });

        scope.$watch('groupType', function (newGroupType, prevGroupType) {
            if (!angular.equals(newGroupType, prevGroupType)) {
                if (!scope.entityGroup || scope.entityGroup.type !== newGroupType) {
                    scope.allEntityGroups = null;
                    scope.entityGroup = null;
                    scope.updateView();
                }
            }
        });

        scope.$watch('disabled', function (newDisabled, prevDisabled) {
            if (!angular.equals(newDisabled, prevDisabled)) {
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
            groupType: '=',
            theForm: '=?',
            tbRequired: '=?',
            excludeGroupIds: '=?',
            excludeGroupAll: '=?',
            disabled:'=ngDisabled',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@',
            onEntityGroupLoaded: '&?'
        }
    };
}
