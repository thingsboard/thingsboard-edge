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
import './converter-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import converterAutocompleteTemplate from './converter-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ConverterAutocomplete($compile, $templateCache, $q, $filter, converterService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(converterAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entity = null;
        scope.entitySearchText = '';

        scope.fetchEntities = function(searchText) {
            var deferred = $q.defer();
            var limit = 50;
            if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                limit += scope.excludeEntityIds.length;
            }
            var pageLink = {limit: limit, textSearch: searchText};
            converterService.getConverters(pageLink, {ignoreLoading: true}).then(function success(result) {
                if (result && result.data) {
                    var entities = [];
                    if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                        result.data.forEach(function (entity) {
                            if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                                if (scope.excludeEntityIds.indexOf(entity.id.id) == -1) {
                                    entities.push(entity);
                                }
                            }
                        });
                    } else {
                        entities = result.data;
                    }
                    if (scope.converterType) {
                        entities = $filter('filter')(entities, {type: scope.converterType}, true);
                    }
                    deferred.resolve(entities);
                } else {
                    deferred.resolve([]);
                }
            }, function fail() {
                deferred.reject();
            });
            return deferred.promise;
        };

        scope.entitySearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var entityId = null;
                if (scope.entity) {
                    if (scope.useFullEntityId) {
                        entityId = scope.entity.id;
                    } else {
                        entityId = scope.entity.id.id;
                    }
                }
                ngModelCtrl.$setViewValue(entityId);
            }
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var id = null;
                if (scope.useFullEntityId) {
                    id = ngModelCtrl.$viewValue.id;
                } else {
                    id = ngModelCtrl.$viewValue;
                }
                converterService.getConverter(id).then(
                    function success(entity) {
                        scope.entity = entity;
                    },
                    function fail() {
                        scope.entity = null;
                    }
                );
            } else {
                scope.entity = null;
            }
        };

        scope.$watch('converterType', function () {
            load();
        });

        scope.$watch('entity', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                scope.updateView();
            }
        });

        function load() {
            scope.selectEntityText = 'converter.select-converter';
            scope.entityText = 'converter.converter';
            scope.entityRequiredText = 'converter.converter-required';

            if (scope.labelText && scope.labelText.length) {
                scope.entityText = scope.labelText;
            }
            if (scope.requiredText && scope.requiredText.length) {
                scope.entityRequiredText = scope.requiredText;
            }
            if (scope.entity && scope.converterType && scope.entity.type != scope.converterType) {
                scope.entity = null;
                scope.updateView();
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
            disabled:'=ngDisabled',
            converterType: '=',
            excludeEntityIds: '=?',
            labelText: '=?',
            requiredText: '=?',
            useFullEntityId: '=?'
        }
    };
}
