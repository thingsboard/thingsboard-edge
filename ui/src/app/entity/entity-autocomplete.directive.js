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
import './entity-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityAutocompleteTemplate from './entity-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityAutocomplete($compile, $templateCache, $q, $filter, entityService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityAutocompleteTemplate);
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
            entityService.getEntitiesByNameFilter(scope.entityType, searchText, limit, null, scope.entitySubtype).then(function success(result) {
                if (result) {
                    if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                        var entities = [];
                        result.forEach(function(entity) {
                            if (scope.excludeEntityIds.indexOf(entity.id.id) == -1) {
                                entities.push(entity);
                            }
                        });
                        deferred.resolve(entities);
                    } else {
                        deferred.resolve(result);
                    }
                } else {
                    deferred.resolve([]);
                }
            }, function fail() {
                deferred.reject();
            });
            return deferred.promise;
        }

        scope.entitySearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.entity ? scope.entity.id.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                entityService.getEntity(scope.entityType, ngModelCtrl.$viewValue).then(
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

        scope.$watch('entityType', function () {
            load();
        });

        scope.$watch('entitySubtype', function () {
            if (scope.entity && scope.entity.type != scope.entitySubtype) {
                scope.entity = null;
                scope.updateView();
            }
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
            switch (scope.entityType) {
                case types.entityType.asset:
                    scope.selectEntityText = 'asset.select-asset';
                    scope.entityText = 'asset.asset';
                    scope.noEntitiesMatchingText = 'asset.no-assets-matching';
                    scope.entityRequiredText = 'asset.asset-required'
                    break;
                case types.entityType.device:
                    scope.selectEntityText = 'device.select-device';
                    scope.entityText = 'device.device';
                    scope.noEntitiesMatchingText = 'device.no-devices-matching';
                    scope.entityRequiredText = 'device.device-required'
                    break;
                case types.entityType.rule:
                    scope.selectEntityText = 'rule.select-rule';
                    scope.entityText = 'rule.rule';
                    scope.noEntitiesMatchingText = 'rule.no-rules-matching';
                    scope.entityRequiredText = 'rule.rule-required'
                    break;
                case types.entityType.plugin:
                    scope.selectEntityText = 'plugin.select-plugin';
                    scope.entityText = 'plugin.plugin';
                    scope.noEntitiesMatchingText = 'plugin.no-plugins-matching';
                    scope.entityRequiredText = 'plugin.plugin-required'
                    break;
                case types.entityType.tenant:
                    scope.selectEntityText = 'tenant.select-tenant';
                    scope.entityText = 'tenant.tenant';
                    scope.noEntitiesMatchingText = 'tenant.no-tenants-matching';
                    scope.entityRequiredText = 'tenant.tenant-required'
                    break;
                case types.entityType.customer:
                    scope.selectEntityText = 'customer.select-customer';
                    scope.entityText = 'customer.customer';
                    scope.noEntitiesMatchingText = 'customer.no-customers-matching';
                    scope.entityRequiredText = 'customer.customer-required'
                    break;
                case types.entityType.user:
                    scope.selectEntityText = 'user.select-user';
                    scope.entityText = 'user.user';
                    scope.noEntitiesMatchingText = 'user.no-users-matching';
                    scope.entityRequiredText = 'user.user-required'
                    break;
                case types.entityType.dashboard:
                    scope.selectEntityText = 'dashboard.select-dashboard';
                    scope.entityText = 'dashboard.dashboard';
                    scope.noEntitiesMatchingText = 'dashboard.no-dashboards-matching';
                    scope.entityRequiredText = 'dashboard.dashboard-required'
                    break;
                case types.entityType.alarm:
                    scope.selectEntityText = 'alarm.select-alarm';
                    scope.entityText = 'alarm.alarm';
                    scope.noEntitiesMatchingText = 'alarm.no-alarms-matching';
                    scope.entityRequiredText = 'alarm.alarm-required'
                    break;
            }
            if (scope.entity && scope.entity.id.entityType != scope.entityType) {
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
            entityType: '=',
            entitySubtype: '=?',
            excludeEntityIds: '=?'
        }
    };
}
