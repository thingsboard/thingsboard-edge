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
