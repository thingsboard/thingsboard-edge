/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
import './user-menu.scss';

/* eslint-disable import/no-unresolved, import/default */

import userMenuTemplate from './user-menu.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.usermenu', [])
    .directive('tbUserMenu', UserMenu)
    .name;

/*@ngInject*/
function UserMenu() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            displayUserInfo: '=',
        },
        controller: UserMenuController,
        controllerAs: 'vm',
        templateUrl: userMenuTemplate
    };
}

/*@ngInject*/
function UserMenuController($scope, userService, $translate, $state) {

    var vm = this;

    var dashboardUser = userService.getCurrentUser();

    vm.authorityName = authorityName;
    vm.logout = logout;
    vm.openProfile = openProfile;
    vm.userDisplayName = userDisplayName;

    function authorityName() {
        var name = "user.anonymous";
        if (dashboardUser) {
            var authority = dashboardUser.authority;
            if (authority === 'SYS_ADMIN') {
                name = 'user.sys-admin';
            } else if (authority === 'TENANT_ADMIN') {
                name = 'user.tenant-admin';
            } else if (authority === 'CUSTOMER_USER') {
                name = 'user.customer';
            }
        }
        return $translate.instant(name);
    }

    function userDisplayName() {
        var name = "";
        if (dashboardUser) {
            if ((dashboardUser.firstName && dashboardUser.firstName.length > 0) ||
                (dashboardUser.lastName && dashboardUser.lastName.length > 0)) {
                if (dashboardUser.firstName) {
                    name += dashboardUser.firstName;
                }
                if (dashboardUser.lastName) {
                    if (name.length > 0) {
                        name += " ";
                    }
                    name += dashboardUser.lastName;
                }
            } else {
                name = dashboardUser.email;
            }
        }
        return name;
    }

    function openProfile() {
        $state.go('home.profile');
    }

    function logout() {
        userService.logout();
    }
}