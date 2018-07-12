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

import entityTypeListTemplate from './entity-type-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-type-list.scss';

/*@ngInject*/
export default function EntityTypeListDirective($compile, $templateCache, $q, $mdUtil, $translate, $filter, types, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityTypeListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.placeholder = scope.tbRequired ? $translate.instant('entity.enter-entity-type')
                                : $translate.instant('entity.any-entity');
        scope.secondaryPlaceholder = '+' + $translate.instant('entity.entity-type');

        var entityTypes;

        if (scope.ignoreAuthorityFilter && scope.allowedEntityTypes
            && scope.allowedEntityTypes.length) {
            entityTypes = {};
            scope.allowedEntityTypes.forEach((entityTypeValue) => {
                var entityType = entityTypeFromValue(entityTypeValue);
                if (entityType) {
                    entityTypes[entityType] = entityTypeValue;
                }
            });
        } else {
            entityTypes = entityService.prepareAllowedEntityTypesList(scope.allowedEntityTypes);
        }

        function entityTypeFromValue(entityTypeValue) {
            for (var entityType in types.entityType) {
                if (types.entityType[entityType] === entityTypeValue) {
                    return entityType;
                }
            }
            return null;
        }

        scope.entityTypesList = [];
        for (var type in entityTypes) {
            var entityTypeInfo = {};
            entityTypeInfo.value = entityTypes[type];
            entityTypeInfo.name = $translate.instant(types.entityTypeTranslations[entityTypeInfo.value].type) + '';
            scope.entityTypesList.push(entityTypeInfo);
        }

        scope.$watch('tbRequired', function () {
            scope.updateValidity();
        });

        scope.fetchEntityTypes = function(searchText) {
            var deferred = $q.defer();
            var entityTypes = $filter('filter')(scope.entityTypesList, {name: searchText});
            deferred.resolve(entityTypes);
            return deferred.promise;
        }

        scope.updateValidity = function() {
            var value = ngModelCtrl.$viewValue;
            var valid = !scope.tbRequired || value && value.length > 0;
            ngModelCtrl.$setValidity('entityTypeList', valid);
        }

        ngModelCtrl.$render = function () {
            if (scope.entityTypeListWatch) {
                scope.entityTypeListWatch();
                scope.entityTypeListWatch = null;
            }
            var entityTypeList = [];
            var value = ngModelCtrl.$viewValue;
            if (value && value.length) {
                value.forEach(function(type) {
                    var entityTypeInfo = {};
                    entityTypeInfo.value = type;
                    entityTypeInfo.name = $translate.instant(types.entityTypeTranslations[entityTypeInfo.value].type) + '';
                    entityTypeList.push(entityTypeInfo);
                });
            }
            scope.entityTypeList = entityTypeList;
            scope.entityTypeListWatch = scope.$watch('entityTypeList', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    updateEntityTypeList();
                }
            }, true);
        }

        function updateEntityTypeList() {
            var values = ngModelCtrl.$viewValue;
            if (!values) {
                values = [];
                ngModelCtrl.$setViewValue(values);
            } else {
                values.length = 0;
            }
            if (scope.entityTypeList && scope.entityTypeList.length) {
                scope.entityTypeList.forEach(function (entityType) {
                    values.push(entityType.value);
                });
            }
            scope.updateValidity();
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
            allowedEntityTypes: '=?',
            ignoreAuthorityFilter: '=?'
        }
    };

}
