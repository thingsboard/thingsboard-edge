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
/* eslint-disable import/no-unresolved, import/default */

import addRoleTemplate from './add-role.tpl.html';
import roleCard from './role-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function RoleCardController(types) {

    var vm = this;

    vm.types = types;
}


/*@ngInject*/
export function RoleController($rootScope, userService, roleService, $state, $stateParams,
                                     $document, $mdDialog, $q, $translate, types) {

    var roleActionsList = [];

    var roleGroupActionsList = [];

    var vm = this;

    vm.types = types;

    vm.roleGridConfig = {
        deleteItemTitleFunc: deleteRoleTitle,
        deleteItemContentFunc: deleteRoleText,
        deleteItemsTitleFunc: deleteRolesTitle,
        deleteItemsActionTitleFunc: deleteRolesActionTitle,
        deleteItemsContentFunc: deleteRolesText,

        saveItemFunc: saveRole,

        getItemTitleFunc: getRoleTitle,

        itemCardController: 'RoleCardController',
        itemCardTemplateUrl: roleCard,
        parentCtl: vm,

        actionsList: roleActionsList,
        groupActionsList: roleGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addRoleTemplate,

        addItemText: function() { return $translate.instant('role.add-role-text') },
        noItemsText: function() { return $translate.instant('role.no-roles-text') },
        itemDetailsText: function() { return $translate.instant('role.role-details') }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.roleGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.roleGridConfig.topIndex = $stateParams.topIndex;
    }

    initController();

    function initController() {
        var fetchRolesFunction = null;
        var deleteRoleFunction = null;
        var refreshRolesParamsFunction = null;

        // var user = userService.getCurrentUser(); dsfas

        fetchRolesFunction = function (pageLink, roleType) {
            return roleService.getTenantRoles(pageLink, true, roleType);
        };
        deleteRoleFunction = function (roleId) {
            return roleService.deleteRole(roleId);
        };
        refreshRolesParamsFunction = function() {
            return {"topIndex": vm.topIndex};
        };

        roleActionsList.push(
            {
                onAction: function ($event, item) {
                    vm.grid.deleteItem($event, item);
                },
                name: function() { return $translate.instant('action.delete') },
                details: function() { return $translate.instant('role.delete') },
                icon: "delete"
            }
        );

        roleGroupActionsList.push(
            {
                onAction: function ($event) {
                    vm.grid.deleteItems($event);
                },
                name: function() { return $translate.instant('role.delete-roles') },
                details: deleteRolesActionTitle,
                icon: "delete"
            }
        );
        vm.roleGridConfig.refreshParamsFunc = refreshRolesParamsFunction;
        vm.roleGridConfig.fetchItemsFunc = fetchRolesFunction;
        vm.roleGridConfig.deleteItemFunc = deleteRoleFunction;

    }

    function deleteRoleTitle(role) {
        return $translate.instant('role.delete-role-title', {roleName: role.name});
    }

    function deleteRoleText() {
        return $translate.instant('role.delete-role-text');
    }

    function deleteRolesTitle(selectedCount) {
        return $translate.instant('role.delete-roles-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRolesActionTitle(selectedCount) {
        return $translate.instant('role.delete-roles-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRolesText () {
        return $translate.instant('role.delete-roles-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getRoleTitle(role) {
        return role ? role.name : '';
    }

    function saveRole(role) {
        var deferred = $q.defer();
        roleService.saveRole(role).then(
            function success(savedRole) {
                $rootScope.$broadcast('roleSaved');
                deferred.resolve(savedRole);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }
}
