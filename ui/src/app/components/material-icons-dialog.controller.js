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
import './material-icons-dialog.scss';

/*@ngInject*/
export default function MaterialIconsDialogController($scope, $mdDialog, $timeout, utils, icon) {

    var vm = this;

    vm.selectedIcon = icon;

    vm.showAll = false;
    vm.loadingIcons = false;

    $scope.$watch('vm.showAll', function(showAll) {
        if (showAll) {
            vm.loadingIcons = true;
            $timeout(function() {
                utils.getMaterialIcons().then(
                    function success(icons) {
                        vm.icons = icons;
                    }
                );
            });
        } else {
            vm.icons = utils.getCommonMaterialIcons();
        }
    });

    $scope.$on('iconsLoadFinished', function() {
        vm.loadingIcons = false;
    });

    vm.cancel = cancel;
    vm.selectIcon = selectIcon;

    function cancel() {
        $mdDialog.cancel();
    }

    function selectIcon($event, icon) {
        vm.selectedIcon = icon;
        $mdDialog.hide(vm.selectedIcon);
    }
}
