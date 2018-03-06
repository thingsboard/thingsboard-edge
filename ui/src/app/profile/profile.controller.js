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
/* eslint-disable import/no-unresolved, import/default */

import changePasswordTemplate from './change-password.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ProfileController(userService, $scope, $document, $mdDialog, $translate) {
    var vm = this;

    vm.profileUser = {};

    vm.save = save;
    vm.changePassword = changePassword;
    vm.languageList = {
        en_US: {value : "en_US", name: "language.en_US"}, 
        ko_KR: {value : "ko_KR", name: "language.ko_KR"},
        zh_CN: {value : "zh_CN", name: "language.zh_CN"},
        ru_RU: {value : "ru_RU", name: "language.ru_RU"},
        es_ES: {value : "es_ES", name: "language.es_ES"},
        fr_FR: {value : "fr_FR", name: "language.fr_FR"},
    };

    loadProfile();

    function loadProfile() {
        userService.getUser(userService.getCurrentUser().userId).then(function success(user) {
            vm.profileUser = user;
            if (!vm.profileUser.additionalInfo) {
                vm.profileUser.additionalInfo = {};
            }
            if (!vm.profileUser.additionalInfo.lang) {
                vm.profileUser.additionalInfo.lang = $translate.use();
            }
        });
    }

    function save() {
        userService.saveUser(vm.profileUser).then(function success(user) {
            $translate.use(vm.profileUser.additionalInfo.lang);
            vm.profileUser = user;
            $scope.theForm.$setPristine();
        });
    }

    function changePassword($event) {
        $mdDialog.show({
            controller: 'ChangePasswordController',
            controllerAs: 'vm',
            templateUrl: changePasswordTemplate,
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
        }, function () {
        });
    }
}
