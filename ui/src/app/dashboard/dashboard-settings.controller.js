/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import './dashboard-settings.scss';

/*@ngInject*/
export default function DashboardSettingsController($scope, $mdDialog, statesControllerService, settings, gridSettings) {

    var vm = this;

    vm.cancel = cancel;
    vm.save = save;
    vm.imageAdded = imageAdded;
    vm.clearImage = clearImage;

    vm.stateControllerIdChanged = stateControllerIdChanged;

    vm.settings = settings;
    vm.gridSettings = gridSettings;
    vm.stateControllers = statesControllerService.getStateControllers();

    if (vm.settings) {
        if (angular.isUndefined(vm.settings.stateControllerId)) {
            vm.settings.stateControllerId = 'entity';
        }

        if (angular.isUndefined(vm.settings.showTitle)) {
            vm.settings.showTitle = false;
        }

        if (angular.isUndefined(vm.settings.titleColor)) {
            vm.settings.titleColor = 'rgba(0,0,0,0.870588)';
        }

        if (angular.isUndefined(vm.settings.showDashboardsSelect)) {
            vm.settings.showDashboardsSelect = true;
        }

        if (angular.isUndefined(vm.settings.showEntitiesSelect)) {
            vm.settings.showEntitiesSelect = true;
        }

        if (angular.isUndefined(vm.settings.showDashboardTimewindow)) {
            vm.settings.showDashboardTimewindow = true;
        }

        if (angular.isUndefined(vm.settings.showDashboardExport)) {
            vm.settings.showDashboardExport = true;
        }

        if (angular.isUndefined(vm.settings.toolbarAlwaysOpen)) {
            vm.settings.toolbarAlwaysOpen = true;
        }
    }

    if (vm.gridSettings) {
        vm.gridSettings.backgroundColor = vm.gridSettings.backgroundColor || 'rgba(0,0,0,0)';
        vm.gridSettings.color = vm.gridSettings.color || 'rgba(0,0,0,0.870588)';
        vm.gridSettings.columns = vm.gridSettings.columns || 24;
        vm.gridSettings.margins = vm.gridSettings.margins || [10, 10];
        vm.gridSettings.autoFillHeight = angular.isDefined(vm.gridSettings.autoFillHeight) ? vm.gridSettings.autoFillHeight : false;
        vm.gridSettings.mobileAutoFillHeight = angular.isDefined(vm.gridSettings.mobileAutoFillHeight) ? vm.gridSettings.mobileAutoFillHeight : false;
        vm.gridSettings.mobileRowHeight = angular.isDefined(vm.gridSettings.mobileRowHeight) ? vm.gridSettings.mobileRowHeight : 70;
        vm.hMargin = vm.gridSettings.margins[0];
        vm.vMargin = vm.gridSettings.margins[1];
        vm.gridSettings.backgroundSizeMode = vm.gridSettings.backgroundSizeMode || '100%';
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function imageAdded($file) {
        var reader = new FileReader();
        reader.onload = function(event) {
            $scope.$apply(function() {
                if (event.target.result && event.target.result.startsWith('data:image/')) {
                    $scope.theForm.$setDirty();
                    vm.gridSettings.backgroundImageUrl = event.target.result;
                }
            });
        };
        reader.readAsDataURL($file.file);
    }

    function clearImage() {
        $scope.theForm.$setDirty();
        vm.gridSettings.backgroundImageUrl = null;
    }

    function stateControllerIdChanged() {
        if (vm.settings.stateControllerId != 'default') {
            vm.settings.toolbarAlwaysOpen = true;
        }
    }

    function save() {
        $scope.theForm.$setPristine();
        if (vm.gridSettings) {
            vm.gridSettings.margins = [vm.hMargin, vm.vMargin];
        }
        $mdDialog.hide(
            {
                settings: vm.settings,
                gridSettings: vm.gridSettings
            }
        );
    }
}
