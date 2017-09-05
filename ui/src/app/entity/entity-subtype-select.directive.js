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
import './entity-subtype-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySubtypeSelectTemplate from './entity-subtype-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySubtypeSelect($compile, $templateCache, $translate, assetService, deviceService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySubtypeSelectTemplate);
        element.html(template);

        if (angular.isDefined(attrs.hideLabel)) {
            scope.showLabel = false;
        } else {
            scope.showLabel = true;
        }

        scope.ngModelCtrl = ngModelCtrl;

        scope.entitySubtypes = [];

        scope.subTypeName = function(subType) {
            if (subType && subType.length) {
                if (scope.typeTranslatePrefix) {
                    return $translate.instant(scope.typeTranslatePrefix + '.' + subType);
                } else {
                    return subType;
                }
            } else {
                return $translate.instant('entity.all-subtypes');
            }
        }

        scope.$watch('entityType', function () {
            load();
        });

        scope.$watch('entitySubtype', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.entitySubtype);
        };

        ngModelCtrl.$render = function () {
            scope.entitySubtype = ngModelCtrl.$viewValue;
        };

        function loadSubTypes() {
            scope.entitySubtypes = [];
            var entitySubtypesPromise;
            if (scope.entityType == types.entityType.asset) {
                entitySubtypesPromise = assetService.getAssetTypes();
            } else if (scope.entityType == types.entityType.device) {
                entitySubtypesPromise = deviceService.getDeviceTypes();
            }
            if (entitySubtypesPromise) {
                entitySubtypesPromise.then(
                    function success(types) {
                        scope.entitySubtypes.push('');
                        types.forEach(function(type) {
                            scope.entitySubtypes.push(type.type);
                        });
                        if (scope.entitySubtypes.indexOf(scope.entitySubtype) == -1) {
                            scope.entitySubtype = '';
                        }
                    },
                    function fail() {}
                );
            }

        }

        function load() {
            if (scope.entityType == types.entityType.asset) {
                scope.entitySubtypeTitle = 'asset.asset-type';
                scope.entitySubtypeRequiredText = 'asset.asset-type-required';
            } else if (scope.entityType == types.entityType.device) {
                scope.entitySubtypeTitle = 'device.device-type';
                scope.entitySubtypeRequiredText = 'device.device-type-required';
            }
            scope.entitySubtypes.length = 0;
            if (scope.entitySubtypesList && scope.entitySubtypesList.length) {
                scope.entitySubtypesList.forEach(function(subType) {
                    scope.entitySubtypes.push(subType);
                });
            } else {
                loadSubTypes();
                if (scope.entityType == types.entityType.asset) {
                    scope.$on('assetSaved', function() {
                        loadSubTypes();
                    });
                } else if (scope.entityType == types.entityType.device) {
                    scope.$on('deviceSaved', function() {
                        loadSubTypes();
                    });
                }
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
            entityType: "=",
            entitySubtypesList: "=?",
            typeTranslatePrefix: "@?"
        }
    };
}
