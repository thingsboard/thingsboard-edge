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
import './related-entity-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import relatedEntityAutocompleteTemplate from './related-entity-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.relatedEntityAutocomplete', [])
    .directive('tbRelatedEntityAutocomplete', RelatedEntityAutocomplete)
    .name;

/*@ngInject*/
function RelatedEntityAutocomplete($compile, $templateCache, $q, $filter, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(relatedEntityAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entity = null;
        scope.entitySearchText = '';

        scope.allEntities = null;

        scope.fetchEntities = function(searchText) {
            var deferred = $q.defer();
            if (!scope.allEntities) {
                entityService.getRelatedEntities(scope.rootEntityId, scope.entityType, scope.entitySubtypes, -1, []).then(
                    function success(entities) {
                        if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                            var filteredEntities = [];
                            entities.forEach(function(entity) {
                                if (scope.excludeEntityIds.indexOf(entity.id.id) == -1) {
                                    filteredEntities.push(entity);
                                }
                            });
                            entities = filteredEntities;
                        }
                        scope.allEntities = entities;
                        filterEntities(searchText, deferred);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                filterEntities(searchText, deferred);
            }
            return deferred.promise;
        }

        function filterEntities(searchText, deferred) {
            var result = $filter('filter')(scope.allEntities, {name: searchText});
            deferred.resolve(result);
        }

        scope.entitySearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.entity ? scope.entity.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                entityService.getRelatedEntity(ngModelCtrl.$viewValue).then(
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
        }

        scope.$watch('entity', function () {
            scope.updateView();
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
            rootEntityId: '=',
            entityType: '=',
            entitySubtypes: '=',
            excludeEntityIds: '=?',
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@'
        }
    };
}
