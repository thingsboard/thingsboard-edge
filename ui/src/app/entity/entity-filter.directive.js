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

import entityFilterTemplate from './entity-filter.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-filter.scss';

/*@ngInject*/
export default function EntityFilterDirective($compile, $templateCache, $q, $document, $mdDialog, types, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityFilterTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;
        scope.aliasFilterTypes = entityService.getAliasFilterTypesByEntityTypes(scope.allowedEntityTypes);

        scope.entityGroupTypes = [types.entityType.device, types.entityType.asset, types.entityType.customer].filter((entityType) =>
            scope.allowedEntityTypes ? scope.allowedEntityTypes.indexOf(entityType) > -1 : true
        );

        scope.$watch('filter.type', function (newType, prevType) {
            if (newType && newType != prevType) {
                updateFilter();
            }
        });

        function updateFilter() {
            var filter = {};
            filter.type = scope.filter.type;
            filter.resolveMultiple = true;
            switch (filter.type) {
                case types.aliasFilterType.singleEntity.value:
                    filter.singleEntity = null;
                    filter.resolveMultiple = false;
                    break;
                case types.aliasFilterType.entityGroup.value:
                    filter.groupStateEntity = false;
                    filter.stateEntityParamName = null;
                    filter.defaultStateGroupType = null;
                    filter.defaultStateEntityGroup = null;
                    filter.groupType = null;
                    filter.entityGroup = null;
                    break;
                case types.aliasFilterType.entityList.value:
                    filter.entityType = null;
                    filter.entityList = [];
                    break;
                case types.aliasFilterType.entityName.value:
                    filter.entityType = null;
                    filter.entityNameFilter = '';
                    break;
                case types.aliasFilterType.entityGroupList.value:
                    filter.groupType = null;
                    filter.entityGroupList = [];
                    break;
                case types.aliasFilterType.entityGroupName.value:
                    filter.groupType = null;
                    filter.entityGroupNameFilter = '';
                    break;
                case types.aliasFilterType.stateEntity.value:
                    filter.stateEntityParamName = null;
                    filter.defaultStateEntity = null;
                    filter.resolveMultiple = false;
                    break;
                case types.aliasFilterType.assetType.value:
                    filter.assetType = null;
                    filter.assetNameFilter = '';
                    break;
                case types.aliasFilterType.deviceType.value:
                    filter.deviceType = null;
                    filter.deviceNameFilter = '';
                    break;
                case types.aliasFilterType.relationsQuery.value:
                case types.aliasFilterType.assetSearchQuery.value:
                case types.aliasFilterType.deviceSearchQuery.value:
                    filter.rootStateEntity = false;
                    filter.stateEntityParamName = null;
                    filter.defaultStateEntity = null;
                    filter.rootEntity = null;
                    filter.direction = types.entitySearchDirection.from;
                    filter.maxLevel = 1;
                    if (filter.type === types.aliasFilterType.relationsQuery.value) {
                        filter.filters = [];
                    } else if (filter.type === types.aliasFilterType.assetSearchQuery.value) {
                        filter.relationType = null;
                        filter.assetTypes = [];
                    } else if (filter.type === types.aliasFilterType.deviceSearchQuery.value) {
                        filter.relationType = null;
                        filter.deviceTypes = [];
                    }
                    break;
            }
            scope.filter = filter;
        }

        scope.$watch('filter', function () {
            scope.updateView();
        });

        scope.updateView = function() {
            ngModelCtrl.$setViewValue(scope.filter);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.filter = ngModelCtrl.$viewValue;
            } else {
                scope.filter = {
                    type: null,
                    resolveMultiple: false
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
            theForm: '=',
            allowedEntityTypes: '=?'
        }
    };

}
