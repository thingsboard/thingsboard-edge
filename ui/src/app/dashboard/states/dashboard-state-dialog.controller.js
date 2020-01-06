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
/*@ngInject*/
export default function DashboardStateDialogController($scope, $mdDialog, $filter, dashboardUtils, isAdd, allStates, state) {

    var vm = this;

    vm.isAdd = isAdd;
    vm.allStates = allStates;
    vm.state = state;

    vm.stateIdTouched = false;

    if (vm.isAdd) {
        vm.state = dashboardUtils.createDefaultState('', false);
        vm.state.id = '';
        vm.prevStateId = '';
    } else {
        vm.state = state;
        vm.prevStateId = vm.state.id;
    }

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.state.name", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.state.name != null) {
            checkStateName();
        }
    });

    $scope.$watch("vm.state.id", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.state.id != null) {
            checkStateId();
        }
    });

    function checkStateName() {
        if (!vm.stateIdTouched && vm.isAdd) {
            vm.state.id = vm.state.name.toLowerCase().replace(/\W/g,"_");
        }
    }

    function checkStateId() {
        var result = $filter('filter')(vm.allStates, {id: vm.state.id}, true);
        if (result && result.length && result[0].id !== vm.prevStateId) {
            $scope.theForm.stateId.$setValidity('stateExists', false);
        } else {
            $scope.theForm.stateId.$setValidity('stateExists', true);
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        vm.state.id = vm.state.id.trim();
        $mdDialog.hide(vm.state);
    }
}
