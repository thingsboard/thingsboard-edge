/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
import 'angular-material-data-table/dist/md-data-table.min.css';
import './user-group-roles.scss';

/* eslint-disable import/no-unresolved, import/default */

import userGroupRolesTemplate from './user-group-roles.tpl.html';
import userGroupRoleDialogTemplate from './user-group-role-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import UserGroupRoleDialogController from './user-group-role-dialog.controller';

/*@ngInject*/
export default function UserGroupRoles($compile, $templateCache, $rootScope, $q, $mdEditDialog, $mdDialog,
                                       $mdUtil, $document, $translate, $filter, $timeout, utils, types, dashboardUtils,
                                       entityService, attributeService) {

    var linker = function (scope, element, attrs) {

        var template = $templateCache.get(userGroupRolesTemplate);

        element.html(template);

        scope.types = types;

        scope.entityType = attrs.entityType;

        scope.attributes = {
            count: 0,
            data: []
        };

        scope.selectedAttributes = [];
        scope.aaa = [];
        scope.aaa.push({roleType: "Pepsi", roleName: "Manager", groupType: "Device", groupName: "Class A Devices"});

        scope.mode = 'default'; // 'widget'
        scope.subscriptionId = null;

        scope.query = {
            order: 'key',
            limit: 5,
            page: 1,
            search: null
        };

        scope.$watch("entityId", function (newVal) {
            if (newVal) {
                scope.resetFilter();
                scope.getEntityAttributes(false, true);
            }
        });

        scope.$watch("attributeScope", function (newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                scope.mode = 'default';
                scope.query.search = null;
                scope.selectedAttributes = [];
                scope.getEntityAttributes(false, true);
            }
        });

        scope.resetFilter = function () {
            scope.mode = 'default';
            scope.query.search = null;
            scope.selectedAttributes = [];
        }

        scope.enterFilterMode = function (event) {
            let $button = angular.element(event.currentTarget);
            let $toolbarsContainer = $button.closest('.toolbarsContainer');

            scope.query.search = '';

            $timeout(() => {
                $toolbarsContainer.find('.searchInput').focus();
            })
        }

        scope.exitFilterMode = function () {
            scope.query.search = null;
            scope.getEntityAttributes();
        }

        scope.$watch("query.search", function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal) && scope.query.search != null) {
                scope.getEntityAttributes();
            }
        });

        function success(attributes, update, apply) {
            scope.attributes = attributes;
            if (!update) {
                scope.selectedAttributes = [];
            }
            if (apply) {
                scope.$digest();
            }
        }

        scope.onReorder = function () {
            scope.getEntityAttributes(false, false);
        }

        scope.onPaginate = function () {
            scope.getEntityAttributes(false, false);
        }

        scope.getEntityAttributes = function (forceUpdate, reset) {
            if (scope.attributesDeferred) {
                scope.attributesDeferred.resolve();
            }
            if (scope.entityId && scope.entityType && scope.attributeScope) {
                if (reset) {
                    scope.attributes = {
                        count: 0,
                        data: []
                    };
                }
                scope.checkSubscription();
                scope.attributesDeferred = attributeService.getEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value,
                    scope.query, function (attributes, update, apply) {
                        success(attributes, update || forceUpdate, apply);
                    }
                );
            } else {
                var deferred = $q.defer();
                scope.attributesDeferred = deferred;
                success({
                    count: 0,
                    data: []
                });
                deferred.resolve();
            }
        }

        scope.editAttribute = function ($event) {
            $event.stopPropagation();
            $mdDialog.show({
                controller: UserGroupRoleDialogController,
                controllerAs: 'vm',
                templateUrl: userGroupRoleDialogTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    isAdd: false,
                    entityType: scope.entityType,
                    entityId: scope.entityId
                },
                fullscreen: true,
                targetEvent: $event
            }).then(function () {
                scope.getEntityAttributes();
            });
        }

        scope.addAttribute = function ($event) {
            $event.stopPropagation();
            $mdDialog.show({
                controller: 'UserGroupRoleDialogController',
                controllerAs: 'vm',
                templateUrl: userGroupRoleDialogTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    isAdd: true,
                    entityType: scope.entityType,
                    entityId: scope.entityId
                },
                fullscreen: true,
                targetEvent: $event
            }).then(function () {
                scope.getEntityAttributes();
            });
        }

        scope.deleteAttributes = function ($event) {
            $event.stopPropagation();
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title($translate.instant('attribute.delete-attributes-title', {count: scope.selectedAttributes.length}, 'messageformat'))
                .htmlContent($translate.instant('attribute.delete-attributes-text'))
                .ariaLabel($translate.instant('attribute.delete-attributes'))
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                attributeService.deleteEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value, scope.selectedAttributes).then(
                    function success() {
                        scope.selectedAttributes = [];
                        scope.getEntityAttributes();
                    }
                )
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            entityId: '=',
            entityName: '='
        }
    };
}
