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
import $ from 'jquery';

import './entity-alias-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityAliasSelectTemplate from './entity-alias-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.entityAliasSelect', [])
    .directive('tbEntityAliasSelect', EntityAliasSelect)
    .name;

/*@ngInject*/
function EntityAliasSelect($compile, $templateCache, $mdConstant, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityAliasSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;

        scope.ngModelCtrl = ngModelCtrl;
        scope.entityAliasList = [];
        scope.entityAlias = null;

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('entityAlias', valid);
        };

        scope.$watch('aliasController', function () {
            scope.entityAliasList = [];
            var entityAliases = scope.aliasController.getEntityAliases();
            for (var aliasId in entityAliases) {
                if (scope.allowedEntityTypes) {
                    if (!entityService.filterAliasByEntityTypes(entityAliases[aliasId], scope.allowedEntityTypes)) {
                        continue;
                    }
                }
                scope.entityAliasList.push(entityAliases[aliasId]);
            }
        });

        scope.$watch('entityAlias', function () {
            scope.updateView();
        });

        scope.entityAliasSearch = function (entityAliasSearchText) {
            return entityAliasSearchText ? scope.entityAliasList.filter(
                scope.createFilterForEntityAlias(entityAliasSearchText)) : scope.entityAliasList;
        };

        scope.createFilterForEntityAlias = function (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(entityAlias) {
                return (angular.lowercase(entityAlias.alias).indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.entityAlias);
            scope.updateValidity();
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.entityAlias = ngModelCtrl.$viewValue;
            }
        }

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.entityAliasEnter = function($event) {
            if ($event.keyCode === $mdConstant.KEY_CODE.ENTER) {
                $event.preventDefault();
                if (!scope.entityAlias) {
                    var found = scope.entityAliasSearch(scope.entityAliasSearchText);
                    found = found.length > 0;
                    if (!found) {
                        scope.createEntityAlias($event, scope.entityAliasSearchText);
                    }
                }
            }
        }

        scope.createEntityAlias = function (event, alias) {
            var autoChild = $('#entity-autocomplete', element)[0].firstElementChild;
            var el = angular.element(autoChild);
            el.scope().$mdAutocompleteCtrl.hidden = true;
            el.scope().$mdAutocompleteCtrl.hasNotFound = false;
            event.preventDefault();
            var promise = scope.onCreateEntityAlias({event: event, alias: alias, allowedEntityTypes: scope.allowedEntityTypes});
            if (promise) {
                promise.then(
                    function success(newAlias) {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                        if (newAlias) {
                            scope.entityAliasList.push(newAlias);
                            scope.entityAlias = newAlias;
                        }
                    },
                    function fail() {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                    }
                );
            } else {
                el.scope().$mdAutocompleteCtrl.hasNotFound = true;
            }
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            aliasController: '=',
            allowedEntityTypes: '=?',
            onCreateEntityAlias: '&'
        }
    };
}

/* eslint-enable angular/angularelement */
