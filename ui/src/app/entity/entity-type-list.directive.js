/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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

        var entityTypes = entityService.prepareAllowedEntityTypesList(scope.allowedEntityTypes);
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
            scope.entityTypeList = [];
            var value = ngModelCtrl.$viewValue;
            if (value && value.length) {
                value.forEach(function(type) {
                    var entityTypeInfo = {};
                    entityTypeInfo.value = type;
                    entityTypeInfo.name = $translate.instant(types.entityTypeTranslations[entityTypeInfo.value].type) + '';
                    scope.entityTypeList.push(entityTypeInfo);
                });
            }
        }

        scope.$watch('entityTypeList', function () {
            var values = [];
            if (scope.entityTypeList && scope.entityTypeList.length) {
                scope.entityTypeList.forEach(function(entityType) {
                    values.push(entityType.value);
                });
            }
            ngModelCtrl.$setViewValue(values);
            scope.updateValidity();
        }, true);

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
            allowedEntityTypes: '=?'
        }
    };

}
