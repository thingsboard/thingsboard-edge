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

import './group-permissions.scss';

/* eslint-disable import/no-unresolved, import/default */

import groupPermissionsTemplate from './group-permissions.tpl.html';
import groupPermissionDialogTemplate from './group-permission-dialog.tpl.html';
import viewRoleDialogTemplate from './view-role.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function GroupPermissions() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            entityId: '=',
            groupType: '@'
        },
        controller: GroupPermissionsController,
        controllerAs: 'vm',
        templateUrl: groupPermissionsTemplate
    };
}

/*@ngInject*/
function GroupPermissionsController($scope, $q, $mdEditDialog, $mdDialog,
                                       $mdUtil, $document, $translate, $filter, $timeout, utils, types, securityTypes, dashboardUtils,
                                       entityService, roleService, userPermissionsService) {

    let vm = this;

    vm.editEnabled = userPermissionsService.hasGenericPermission(securityTypes.resource.groupPermission, securityTypes.operation.write);
    vm.addEnabled = userPermissionsService.hasGenericPermission(securityTypes.resource.groupPermission, securityTypes.operation.create);
    vm.deleteEnabled = userPermissionsService.hasGenericPermission(securityTypes.resource.groupPermission, securityTypes.operation.delete);

    vm.types = types;

    vm.groupPermissions = [];
    vm.groupPermissionsCount = 0;
    vm.allGroupPermissions = [];
    vm.selectedGroupPermissions = [];

    vm.query = {
        order: 'roleName',
        limit: 5,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addGroupPermission = addGroupPermission;
    vm.editGroupPermission = editGroupPermission;
    vm.deleteGroupPermission = deleteGroupPermission;
    vm.deleteGroupPermissions = deleteGroupPermissions;
    vm.reloadGroupPermissions = reloadGroupPermissions;
    vm.updateGroupPermissions = updateGroupPermissions;
    vm.viewRole = viewRole;

    $scope.$watch("vm.entityId", function(newVal, prevVal) {
        if (newVal && !angular.equals(newVal, prevVal)) {
            reloadGroupPermissions();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateGroupPermissions();
        }
    });

    function enterFilterMode (event) {
        let $button = angular.element(event.currentTarget);
        let $toolbarsContainer = $button.closest('.toolbarsContainer');

        vm.query.search = '';

        $timeout(()=>{
            $toolbarsContainer.find('.searchInput').focus();
        })
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateGroupPermissions();
    }

    function onReorder () {
        updateGroupPermissions();
    }

    function onPaginate () {
        updateGroupPermissions();
    }

    function addGroupPermission($event) {
        if ($event) {
            $event.stopPropagation();
        }
        openGroupPermissionDialog($event);
    }

    function editGroupPermission($event, groupPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        openGroupPermissionDialog($event, groupPermission);
    }

    function openGroupPermissionDialog($event, groupPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        var isAdd = false;
        if (!groupPermission) {
            isAdd = true;
            groupPermission = {
                userGroupId: {
                    entityType: vm.types.entityType.entityGroup,
                    id: vm.entityId
                }
            };
        } else {
            groupPermission = angular.copy(groupPermission);
        }
        $mdDialog.show({
            controller: 'GroupPermissionDialogController',
            controllerAs: 'vm',
            templateUrl: groupPermissionDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                isAdd: isAdd,
                groupPermission: groupPermission
            },
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            reloadGroupPermissions();
        });
    }

    function deleteGroupPermission ($event, groupPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        if (groupPermission) {
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title($translate.instant('group-permission.delete-group-permission-title', {roleName: groupPermission.roleName}))
                .htmlContent($translate.instant('group-permission.delete-group-permission-text'))
                .ariaLabel($translate.instant('group-permission.delete-group-permission'))
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                roleService.deleteGroupPermission(groupPermission.id.id).then(
                    function success() {
                        reloadGroupPermissions();
                    }
                )
            });
        }
    }

    function deleteGroupPermissions ($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.selectedGroupPermissions && vm.selectedGroupPermissions.length > 0) {
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title($translate.instant('group-permission.delete-group-permissions-title', {count: vm.selectedGroupPermissions.length}, 'messageformat'))
                .htmlContent($translate.instant('group-permission.delete-group-permissions-text'))
                .ariaLabel($translate.instant('group-permission.delete-group-permissions'))
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                var tasks = [];
                for (var i=0;i<vm.selectedGroupPermissions.length;i++) {
                    var groupPermission = vm.selectedGroupPermissions[i];
                    tasks.push(roleService.deleteGroupPermission(groupPermission.id.id));
                }
                $q.all(tasks).then(function () {
                    reloadGroupPermissions();
                });
            });
        }
    }

    function reloadGroupPermissions () {
        vm.allGroupPermissions.length = 0;
        vm.groupPermissions.length = 0;
        vm.groupPermissionsPromise;
        if (vm.groupType == vm.types.entityType.user) {
            vm.groupPermissionsPromise = roleService.getUserGroupPermissions(vm.entityId);
        } else {
            vm.groupPermissionsPromise = roleService.getEntityGroupPermissions(vm.entityId);
        }
        vm.groupPermissionsPromise.then(
            function success(allGroupPermissions) {
                allGroupPermissions.forEach(function(groupPermission) {
                    groupPermission.roleName = groupPermission.role.name;
                    groupPermission.roleTypeName = $translate.instant('role.display-type.' + groupPermission.role.type);
                    if (groupPermission.entityGroupType) {
                        groupPermission.entityGroupTypeName = $translate.instant(vm.types.entityTypeTranslations[groupPermission.entityGroupType].typePlural);
                    } else {
                        groupPermission.entityGroupTypeName = '';
                    }
                });
                vm.allGroupPermissions = allGroupPermissions;
                vm.selectedGroupPermissions = [];
                vm.updateGroupPermissions();
                vm.groupPermissionsPromise = null;
            },
            function fail() {
                vm.allGroupPermissions = [];
                vm.selectedGroupPermissions = [];
                vm.updateGroupPermissions();
                vm.groupPermissionsPromise = null;
            }
        )
    }

    function updateGroupPermissions () {
        vm.selectedGroupPermissions = [];
        var result = $filter('orderBy')(vm.allGroupPermissions, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.groupPermissionsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.groupPermissions = result.slice(startIndex, startIndex + vm.query.limit);
    }

    function viewRole($event, groupPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: ViewRoleController,
            controllerAs: 'vm',
            templateUrl: viewRoleDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                role: groupPermission.role
            },
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
        });
    }
}

/*@ngInject*/
function ViewRoleController($scope, $mdDialog, role) {

    var vm = this;
    vm.role = role;

    vm.close = close;

    function close() {
        $mdDialog.hide();
    }

}
