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
import './owner-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import ownerAutocompleteTemplate from './owner-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function OwnerAutocompleteDirective($compile, $templateCache, $q, $filter, entityService, entityGroupService, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(ownerAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.owner = null;
        scope.ownerSearchText = '';

        scope.utils = utils;

        scope.fetchOwners = function(searchText) {
            var limit = 50;
            if (scope.excludeOwnerIds && scope.excludeOwnerIds.length) {
                limit += scope.excludeOwnerIds.length;
            }
            var pageLink = {limit: limit, textSearch: searchText};
            var deferred = $q.defer();

            var promise = entityGroupService.getOwners(pageLink, {ignoreLoading: true});

            promise.then(function success(result) {
                var owners = result.data;
                if (scope.excludeOwnerIds && scope.excludeOwnerIds.length) {
                    scope.excludeOwnerIds.forEach((excludeId) => {
                        var toExclude = $filter('filter')(owners, {id: {id: excludeId}}, true);
                        if (toExclude && toExclude.length) {
                            var index = owners.indexOf(toExclude[0]);
                            if (index > -1) {
                                owners.splice(index, 1);
                            }
                        }
                    });
                }
                deferred.resolve(owners);
            }, function fail() {
                deferred.reject();
            });
            return deferred.promise;
        };

        scope.ownerSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.owner ? scope.owner.id : null);
            }
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.id) {
                var ownerId = ngModelCtrl.$viewValue;
                entityService.getEntity(ownerId.entityType, ownerId.id, {ignoreLoading: true}).then(
                    function success(owner) {
                        scope.owner = owner;
                    },
                    function fail() {
                        scope.owner = null;
                    }
                );
            } else {
                scope.owner = null;
            }
        };

        scope.$watch('owner', function (newOwner, prevOwner) {
            if (!angular.equals(newOwner, prevOwner)) {
                scope.updateView();
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
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            excludeOwnerIds: '=?',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@'
        }
    };
}
