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
/* eslint-disable import/no-unresolved, import/default */

import entityListTemplate from './entity-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-list.scss';

/*@ngInject*/
export default function EntityListDirective($compile, $templateCache, $q, $mdUtil, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.$watch('tbRequired', function () {
            scope.updateValidity();
        });

        scope.fetchEntities = function(searchText, limit) {
             var deferred = $q.defer();
             entityService.getEntitiesByNameFilter(scope.entityType, searchText, limit, {ignoreLoading: true}).then(
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
         }

        scope.updateValidity = function() {
            var value = ngModelCtrl.$viewValue;
            var valid = !scope.tbRequired || value && value.length > 0;
            ngModelCtrl.$setValidity('entityList', valid);
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            var value = ngModelCtrl.$viewValue;
            scope.entityList = [];
            if (value && value.length > 0) {
                entityService.getEntities(scope.entityType, value).then(function (entities) {
                    scope.entityList = entities;
                    initWatchers();
                });
            } else {
                initWatchers();
            }
        }

        function initWatchers() {
            scope.entityTypeDeregistration = scope.$watch('entityType', function (newEntityType, prevEntityType) {
                if (!angular.equals(newEntityType, prevEntityType)) {
                    scope.entityList = [];
                }
            });
            scope.entityListDeregistration = scope.$watch('entityList', function () {
                var ids = [];
                if (scope.entityList && scope.entityList.length > 0) {
                    for (var i=0;i<scope.entityList.length;i++) {
                        ids.push(scope.entityList[i].id.id);
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
            if (scope.entityTypeDeregistration) {
                scope.entityTypeDeregistration();
                scope.entityTypeDeregistration = null;
            }
            if (scope.entityListDeregistration) {
                scope.entityListDeregistration();
                scope.entityListDeregistration = null;
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
            entityType: '='
        }
    };

}
