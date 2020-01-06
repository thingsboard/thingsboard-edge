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
import './entity-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityAutocompleteTemplate from './entity-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityAutocomplete($compile, $templateCache, $q, $filter, entityService, types, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entity = null;
        scope.entitySearchText = '';
        scope.utils = utils;

        scope.fetchEntities = function(searchText) {
            var deferred = $q.defer();
            var limit = 50;
            if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                limit += scope.excludeEntityIds.length;
            }
            var targetType = scope.entityType;
            if (targetType == types.aliasEntityType.current_customer) {
                targetType = types.entityType.customer;
            }

            entityService.getEntitiesByNameFilter(targetType, searchText, limit, {ignoreLoading: true}, scope.entitySubtype).then(function success(result) {
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
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var id = null;
                if (scope.useFullEntityId) {
                    id = ngModelCtrl.$viewValue.id;
                } else {
                    id = ngModelCtrl.$viewValue;
                }
                var targetType = scope.entityType;
                if (targetType == types.aliasEntityType.current_customer) {
                    targetType = types.entityType.customer;
                }
                entityService.getEntity(targetType, id).then(
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
            if (scope.entity && angular.isDefined(scope.entity.type) && scope.entity.type != scope.entitySubtype) {
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
                    scope.entityRequiredText = 'asset.asset-required';
                    break;
                case types.entityType.device:
                    scope.selectEntityText = 'device.select-device';
                    scope.entityText = 'device.device';
                    scope.noEntitiesMatchingText = 'device.no-devices-matching';
                    scope.entityRequiredText = 'device.device-required';
                    break;
                case types.entityType.entityView:
                    scope.selectEntityText = 'entity-view.select-entity-view';
                    scope.entityText = 'entity-view.entity-view';
                    scope.noEntitiesMatchingText = 'entity-view.no-entity-views-matching';
                    scope.entityRequiredText = 'entity-view.entity-view-required';
                    break;
                case types.entityType.rulechain:
                    scope.selectEntityText = 'rulechain.select-rulechain';
                    scope.entityText = 'rulechain.rulechain';
                    scope.noEntitiesMatchingText = 'rulechain.no-rulechains-matching';
                    scope.entityRequiredText = 'rulechain.rulechain-required';
                    break;
                case types.entityType.tenant:
                    scope.selectEntityText = 'tenant.select-tenant';
                    scope.entityText = 'tenant.tenant';
                    scope.noEntitiesMatchingText = 'tenant.no-tenants-matching';
                    scope.entityRequiredText = 'tenant.tenant-required';
                    break;
                case types.entityType.customer:
                    scope.selectEntityText = 'customer.select-customer';
                    scope.entityText = 'customer.customer';
                    scope.noEntitiesMatchingText = 'customer.no-customers-matching';
                    scope.entityRequiredText = 'customer.customer-required';
                    break;
                case types.entityType.user:
                    scope.selectEntityText = 'user.select-user';
                    scope.entityText = 'user.user';
                    scope.noEntitiesMatchingText = 'user.no-users-matching';
                    scope.entityRequiredText = 'user.user-required';
                    break;
                case types.entityType.dashboard:
                    scope.selectEntityText = 'dashboard.select-dashboard';
                    scope.entityText = 'dashboard.dashboard';
                    scope.noEntitiesMatchingText = 'dashboard.no-dashboards-matching';
                    scope.entityRequiredText = 'dashboard.dashboard-required';
                    break;
                case types.entityType.alarm:
                    scope.selectEntityText = 'alarm.select-alarm';
                    scope.entityText = 'alarm.alarm';
                    scope.noEntitiesMatchingText = 'alarm.no-alarms-matching';
                    scope.entityRequiredText = 'alarm.alarm-required';
                    break;
                case types.aliasEntityType.current_customer:
                    scope.selectEntityText = 'customer.select-default-customer';
                    scope.entityText = 'customer.default-customer';
                    scope.noEntitiesMatchingText = 'customer.no-customers-matching';
                    scope.entityRequiredText = 'customer.default-customer-required';
                    break;
                case types.entityType.converter:
                    scope.selectEntityText = 'converter.select-converter';
                    scope.entityText = 'converter.converter';
                    scope.noEntitiesMatchingText = 'converter.no-converters-matching';
                    scope.entityRequiredText = 'converter.converter-required'
                    break;
                case types.entityType.integration:
                    scope.selectEntityText = 'integration.select-integration';
                    scope.entityText = 'integration.integration';
                    scope.noEntitiesMatchingText = 'integration.no-integrations-matching';
                    scope.entityRequiredText = 'integration.integration-required'
                    break;
                case types.entityType.schedulerEvent:
                    scope.selectEntityText = 'scheduler.select-scheduler-event';
                    scope.entityText = 'scheduler.scheduler-event';
                    scope.noEntitiesMatchingText = 'scheduler.no-scheduler-events-matching';
                    scope.entityRequiredText = 'scheduler.scheduler-event-required'
                    break;
                case types.entityType.blobEntity:
                    scope.selectEntityText = 'blob-entity.select-blob-entity';
                    scope.entityText = 'blob-entity.blob-entity';
                    scope.noEntitiesMatchingText = 'blob-entity.no-blob-entities-matching';
                    scope.entityRequiredText = 'blob-entity.blob-entity-required'
                    break;
                case types.entityType.role:
                    scope.selectEntityText = 'role.select-role';
                    scope.entityText = 'role.role';
                    scope.noEntitiesMatchingText = 'role.no-roles-matching';
                    scope.entityRequiredText = 'role.role-required';
                    scope.theForm.$setPristine();
                    break;
            }
            if (scope.labelText && scope.labelText.length) {
                scope.entityText = scope.labelText;
            }
            if (scope.requiredText && scope.requiredText.length) {
                scope.entityRequiredText = scope.requiredText;
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
            excludeEntityIds: '=?',
            labelText: '=?',
            requiredText: '=?',
            useFullEntityId: '=?'
        }
    };
}
