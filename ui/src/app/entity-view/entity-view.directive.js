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
import './entity-view.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityViewFieldsetTemplate from './entity-view-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityViewDirective($q, $compile, $templateCache, $filter, toast, $translate, $mdConstant, $mdExpansionPanel,
                                            types, clipboardService, entityViewService, entityService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(entityViewFieldsetTemplate);
        element.html(template);

        scope.attributesPanelId = (Math.random()*1000).toFixed(0);
        scope.timeseriesPanelId = (Math.random()*1000).toFixed(0);
        scope.$mdExpansionPanel = $mdExpansionPanel;

        scope.types = types;
        /*scope.isAssignedToCustomer = false;
        scope.isPublic = false;
        scope.assignedCustomer = null;*/

        scope.allowedEntityTypes = [types.entityType.device, types.entityType.asset];

        var semicolon = 186;
        scope.separatorKeys = [$mdConstant.KEY_CODE.ENTER, $mdConstant.KEY_CODE.COMMA, semicolon];

        scope.$watch('entityView', function(newVal) {
            if (newVal) {
                /*if (scope.entityView.customerId && scope.entityView.customerId.id !== types.id.nullUid) {
                    scope.isAssignedToCustomer = true;
                    customerService.getShortCustomerInfo(scope.entityView.customerId.id).then(
                        function success(customer) {
                            scope.assignedCustomer = customer;
                            scope.isPublic = customer.isPublic;
                        }
                    );
                } else {
                    scope.isAssignedToCustomer = false;
                    scope.isPublic = false;
                    scope.assignedCustomer = null;
                }*/
                if (scope.entityView.startTimeMs > 0) {
                    scope.startTimeMs = new Date(scope.entityView.startTimeMs);
                } else {
                    scope.startTimeMs = null;
                }
                if (scope.entityView.endTimeMs > 0) {
                    scope.endTimeMs = new Date(scope.entityView.endTimeMs);
                } else {
                    scope.endTimeMs = null;
                }
                if (!scope.entityView.keys) {
                    scope.entityView.keys = {};
                    scope.entityView.keys.timeseries = [];
                    scope.entityView.keys.attributes = {};
                    scope.entityView.keys.attributes.ss = [];
                    scope.entityView.keys.attributes.cs = [];
                    scope.entityView.keys.attributes.sh = [];
                }
            }
        });

        scope.dataKeysSearch = function (searchText, type) {
            var deferred = $q.defer();
            entityService.getEntityKeys(scope.entityView.entityId.entityType, scope.entityView.entityId.id, searchText, type, {ignoreLoading: true}).then(
                function success(keys) {
                    deferred.resolve(keys);
                },
                function fail() {
                    deferred.resolve([]);
                }
            );
            return deferred.promise;

        };

        scope.$watch('startTimeMs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() > scope.maxStartTimeMs) {
                    scope.startTimeMs = angular.copy(scope.maxStartTimeMs);
                }
            }
            updateMinMaxDates();
        });

        scope.$watch('endTimeMs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() < scope.minEndTimeMs) {
                    scope.endTimeMs = angular.copy(scope.minEndTimeMs);
                }
            }
            updateMinMaxDates();
        });

        function updateMinMaxDates() {
            if (scope.entityView) {
                if (scope.endTimeMs) {
                    scope.maxStartTimeMs = angular.copy(new Date(scope.endTimeMs.getTime()));
                    scope.entityView.endTimeMs = scope.endTimeMs.getTime();
                } else {
                    scope.entityView.endTimeMs = 0;
                }
                if (scope.startTimeMs) {
                    scope.minEndTimeMs = angular.copy(new Date(scope.startTimeMs.getTime()));
                    scope.entityView.startTimeMs = scope.startTimeMs.getTime();
                } else {
                    scope.entityView.startTimeMs = 0;
                }
            }
        }

        scope.onEntityViewIdCopied = function() {
            toast.showSuccess($translate.instant('entity-view.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            entityGroup: '=',
            entityView: '=',
            isEdit: '=',
            theForm: '=',
            onAssignToCustomer: '&',
            onMakePublic: '&',
            onUnassignFromCustomer: '&',
            onDeleteEntityView: '&',
            hideAssignmentActions: '=',
            hideDelete: '='
        }
    };
}
