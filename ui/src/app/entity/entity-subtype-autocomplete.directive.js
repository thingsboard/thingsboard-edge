/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import './entity-subtype-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySubtypeAutocompleteTemplate from './entity-subtype-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySubtypeAutocomplete($compile, $templateCache, $q, $filter, assetService, deviceService, entityViewService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySubtypeAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.subType = null;
        scope.subTypeSearchText = '';
        scope.entitySubtypes = null;

        var comparator = function(actual, expected) {
            if (angular.isUndefined(actual)) {
                return false;
            }
            if ((actual === null) || (expected === null)) {
                return actual === expected;
            }
            return actual.startsWith(expected);
        };

        scope.fetchSubTypes = function(searchText) {
            var deferred = $q.defer();
            loadSubTypes().then(
                function success(subTypes) {
                    var result = $filter('filter')(subTypes, {'$': searchText}, comparator);
                    if (result && result.length) {
                        if (searchText && searchText.length && result.indexOf(searchText) === -1) {
                            result.push(searchText);
                        }
                        result.sort();
                        deferred.resolve(result);
                    } else {
                        deferred.resolve([searchText]);
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );
            return deferred.promise;
        }

        scope.subTypeSearchTextChanged = function() {
            //scope.subType = scope.subTypeSearchText;
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.subType);
            }
        }

        ngModelCtrl.$render = function () {
            scope.subType = ngModelCtrl.$viewValue;
        }

        scope.$watch('entityType', function () {
            load();
        });

        scope.$watch('subType', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        function loadSubTypes() {
            var deferred = $q.defer();
            if (!scope.entitySubtypes) {
                var entitySubtypesPromise;
                if (scope.entityType == types.entityType.asset) {
                    entitySubtypesPromise = assetService.getAssetTypes({ignoreLoading: true});
                } else if (scope.entityType == types.entityType.device) {
                    entitySubtypesPromise = deviceService.getDeviceTypes({ignoreLoading: true});
                } else if (scope.entityType == types.entityType.entityView) {
                    entitySubtypesPromise = entityViewService.getEntityViewTypes({ignoreLoading: true});
                }
                if (entitySubtypesPromise) {
                    entitySubtypesPromise.then(
                        function success(types) {
                            scope.entitySubtypes = [];
                            types.forEach(function (type) {
                                scope.entitySubtypes.push(type.type);
                            });
                            deferred.resolve(scope.entitySubtypes);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    deferred.reject();
                }
            } else {
                deferred.resolve(scope.entitySubtypes);
            }
            return deferred.promise;
        }

        function load() {
            if (scope.entityType == types.entityType.asset) {
                scope.selectEntitySubtypeText = 'asset.select-asset-type';
                scope.entitySubtypeText = 'asset.asset-type';
                scope.entitySubtypeRequiredText = 'asset.asset-type-required';
                scope.$on('assetSaved', function() {
                    scope.entitySubtypes = null;
                });
            } else if (scope.entityType == types.entityType.device) {
                scope.selectEntitySubtypeText = 'device.select-device-type';
                scope.entitySubtypeText = 'device.device-type';
                scope.entitySubtypeRequiredText = 'device.device-type-required';
                scope.$on('deviceSaved', function() {
                    scope.entitySubtypes = null;
                });
            } else if (scope.entityType == types.entityType.entityView) {
                scope.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
                scope.entitySubtypeText = 'entity-view.entity-view-type';
                scope.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
                scope.$on('entityViewSaved', function() {
                    scope.entitySubtypes = null;
                });
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
            entityType: "="
        }
    };
}
