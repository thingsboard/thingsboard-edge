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
import './manage-dashboard-states.scss';

/* eslint-disable import/no-unresolved, import/default */

import dashboardStateDialogTemplate from './dashboard-state-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ManageDashboardStatesController($scope, $mdDialog, $filter, $document, $translate, states) {

    var vm = this;

    vm.allStates = [];
    for (var id in states) {
        var state = states[id];
        state.id = id;
        vm.allStates.push(state);
    }

    vm.states = [];
    vm.statesCount = 0;

    vm.query = {
        order: 'name',
        limit: 5,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addState = addState;
    vm.editState = editState;
    vm.deleteState = deleteState;

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateStates();
        }
    });

    updateStates ();

    function updateStates () {
        var result = $filter('orderBy')(vm.allStates, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.statesCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.states = result.slice(startIndex, startIndex + vm.query.limit);
    }

    function enterFilterMode () {
        vm.query.search = '';
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateStates();
    }

    function onReorder () {
        updateStates();
    }

    function onPaginate () {
        updateStates();
    }

    function addState ($event) {
        openStateDialog($event, null, true);
    }

    function editState ($event, alertRule) {
        if ($event) {
            $event.stopPropagation();
        }
        openStateDialog($event, alertRule, false);
    }

    function openStateDialog($event, state, isAdd) {
        var prevStateId = null;
        if (!isAdd) {
            prevStateId = state.id;
        }
        $mdDialog.show({
            controller: 'DashboardStateDialogController',
            controllerAs: 'vm',
            templateUrl: dashboardStateDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {isAdd: isAdd, allStates: vm.allStates, state: angular.copy(state)},
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (state) {
            saveState(state, prevStateId);
            updateStates();
        });
    }

    function getStateIndex(id) {
        var result = $filter('filter')(vm.allStates, {id: id}, true);
        if (result && result.length) {
            return vm.allStates.indexOf(result[0]);
        }
        return -1;
    }

    function saveState(state, prevStateId) {
        if (prevStateId) {
            var index = getStateIndex(prevStateId);
            if (index > -1) {
                vm.allStates[index] = state;
            }
        } else {
            vm.allStates.push(state);
        }
        if (state.root) {
            for (var i=0; i < vm.allStates.length; i++) {
                var otherState = vm.allStates[i];
                if (otherState.id !== state.id) {
                    otherState.root = false;
                }
            }
        }
        $scope.theForm.$setDirty();
    }

    function deleteState ($event, state) {
        if ($event) {
            $event.stopPropagation();
        }
        if (state) {
            var title = $translate.instant('dashboard.delete-state-title');
            var content = $translate.instant('dashboard.delete-state-text', {stateName: state.name});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));

            confirm._options.multiple = true;
            confirm._options.fullscreen = true;

            $mdDialog.show(confirm).then(function () {
                var index = getStateIndex(state.id);
                if (index > -1) {
                    vm.allStates.splice(index, 1);
                }
                $scope.theForm.$setDirty();
                updateStates();
            });
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        var savedStates = {};
        for (var i=0;i<vm.allStates.length;i++) {
            var state = vm.allStates[i];
            var id = state.id;
            delete state.id;
            savedStates[id] = state;
        }
        $mdDialog.hide(savedStates);
    }
}
