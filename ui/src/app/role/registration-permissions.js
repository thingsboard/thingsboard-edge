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
import './registration-permissions.scss';

/* eslint-disable import/no-unresolved, import/default */

import registrationPermissionsTemplate from './registration-permissions.tpl.html';
import registrationPermissionDialogTemplate from './registration-permission-dialog.tpl.html';
import viewRoleDialogTemplate from './view-role.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RegistrationPermissions() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            userPermissions: "="
        },
        controller: RegistrationPermissionsController,
        controllerAs: 'vm',
        templateUrl: registrationPermissionsTemplate
    };
}

/*@ngInject*/
function RegistrationPermissionsController($scope, $q, $mdEditDialog, $mdDialog, selfRegistrationService,
                                    $mdUtil, $document, $translate, $filter, $timeout, types) {

    let vm = this;

    vm.types = types;

    vm.groupPermissions = [];
    vm.groupPermissionsCount = 0;
    vm.allUserPermissions = [];
    vm.selectedUserPermissions = [];

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
    vm.addUsersPermission = addUsersPermission;
    vm.editUsersPermission = editUsersPermission;
    vm.deleteUsersPermission = deleteUsersPermission;
    vm.deleteUsersPermissions = deleteUsersPermissions;
    vm.updateUsersPermissions = updateUsersPermissions;
    vm.viewRole = viewRole;

    if(angular.isUndefined(vm.userPermissions) || vm.userPermissions === null){
        vm.userPermissions = [];
    }

    reloadUsersPermissions();

    $scope.$watch("vm.userPermissions", function(newVal, prevVal) {
        if (newVal && !angular.equals(newVal, prevVal)) {
            reloadUsersPermissions();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateUsersPermissions();
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
        updateUsersPermissions();
    }

    function onReorder () {
        updateUsersPermissions();
    }

    function onPaginate () {
        updateUsersPermissions();
    }

    function addUsersPermission($event) {
        if ($event) {
            $event.stopPropagation();
        }
        openUsersPermissionDialog($event);
    }

    function editUsersPermission($event, userPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        openUsersPermissionDialog($event, userPermission);
    }

    function openUsersPermissionDialog($event, userPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        var isAdd = false;
        if (!userPermission) {
            isAdd = true;
            userPermission = {};
            if (vm.isUserGroup) {
                userPermission.userGroupId = {
                    entityType: vm.types.entityType.entityGroup,
                    id: "3c1826ee-ec73-4739-9945-c37ad8990aff"
                };
            }
        } else {
            var index = vm.allUserPermissions.indexOf(userPermission);
            userPermission = angular.copy(userPermission);
        }
        $mdDialog.show({
            controller: 'RegistrationPermissionDialogController',
            controllerAs: 'vm',
            templateUrl: registrationPermissionDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                isUserGroup: vm.isUserGroup,
                isAdd: isAdd,
                groupPermission: userPermission
            },
            fullscreen: true,
            targetEvent: $event
        }).then(function success (formData) {
            if (!isAdd && index > -1)
                vm.userPermissions[index] = formData;
            else
                vm.userPermissions.push(formData);
            reloadUsersPermissions();
        })
    }

    function deleteUsersPermission ($event, userPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        if (userPermission) {
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title($translate.instant('group-permission.delete-group-permission-title', {roleName: userPermission.roleName}))
                .htmlContent($translate.instant('group-permission.delete-group-permission-text'))
                .ariaLabel($translate.instant('group-permission.delete-group-permission'))
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                var index = vm.allUserPermissions.indexOf(userPermission);
                if(index > -1)
                    vm.userPermissions.splice(index, 1);
                reloadUsersPermissions();
            });
        }
    }

    function deleteUsersPermissions ($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.selectedUserPermissions && vm.selectedUserPermissions.length > 0) {
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title($translate.instant('group-permission.delete-group-permissions-title', {count: vm.selectedUserPermissions.length}, 'messageformat'))
                .htmlContent($translate.instant('group-permission.delete-group-permissions-text'))
                .ariaLabel($translate.instant('group-permission.delete-group-permissions'))
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                var tasks = [];
                for (let i=0;i<vm.selectedUserPermissions.length;i++) {
                    let index = vm.allUserPermissions.indexOf(vm.selectedUserPermissions[i]);
                    tasks.push(index);
                }
                tasks.reverse();
                for(let i = 0; i < tasks.length; i++){
                    if(tasks[i] > -1)
                        vm.userPermissions.splice(tasks[i], 1);
                }
                reloadUsersPermissions();
            });
        }
    }

    function reloadUsersPermissions () {
        if (vm.userPermissions) {
            vm.editEnabled = true;
            vm.addEnabled = true;
            vm.deleteEnabled = true;

            vm.isUserGroup = true;

            let allUserPermissions = angular.copy(vm.userPermissions);

            let tasks = [];

            allUserPermissions.forEach(function (userPermission) {
                tasks.push(selfRegistrationService.getPermissionUser(userPermission));
            });

            $q.all(tasks).then(function success(userPermissions) {
                vm.allUserPermissions.length = 0;
                userPermissions.forEach(function (userPermission) {
                    userPermission.roleName = userPermission.role.name;
                    if (vm.isUserGroup) {
                        userPermission.roleTypeName = $translate.instant('role.display-type.' + userPermission.role.type);
                        if (userPermission.entity && userPermission.entity.type) {
                            userPermission.entityGroupTypeName = $translate.instant(vm.types.entityTypeTranslations[userPermission.entity.type].typePlural);
                        } else {
                            userPermission.entityGroupTypeName = '';
                        }
                    }
                });
                vm.allUserPermissions = allUserPermissions;
                vm.selectedUserPermissions = [];
                vm.groupPermissions.length = 0;
                vm.updateUsersPermissions();
            })
        } else {
            vm.allUserPermissions.length = 0;
            vm.groupPermissions.length = 0;
        }
    }

    function updateUsersPermissions () {
        vm.selectedUserPermissions = [];
        var result = $filter('orderBy')(vm.allUserPermissions, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.groupPermissionsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.groupPermissions = result.slice(startIndex, startIndex + vm.query.limit);
    }

    function viewRole($event, userPermission) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: ViewRoleController,
            controllerAs: 'vm',
            templateUrl: viewRoleDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                role: userPermission.role
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
