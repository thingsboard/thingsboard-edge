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
/*@ngInject*/
export default function WidgetActionDialogController($scope, $mdDialog, $filter, $q, dashboardService, dashboardUtils, types, utils,
                                                     isAdd, fetchDashboardStates, actionSources, actionTypes, customFunctionArgs, widgetActions, action) {

    var vm = this;

    vm.types = types;

    vm.isAdd = isAdd;
    vm.fetchDashboardStates = fetchDashboardStates;
    vm.actionSources = actionSources;
    vm.actionTypes = actionTypes;
    vm.widgetActions = widgetActions;
    vm.customFunctionArgs = customFunctionArgs;

    vm.targetDashboardStateSearchText = '';

    vm.selectedDashboardStateIds = [];

    if (vm.isAdd) {
        vm.action = {
            id: utils.guid()
        };
    } else {
        vm.action = action;
    }

    vm.actionSourceName = actionSourceName;

    vm.targetDashboardStateSearchTextChanged = function() {
    }

    vm.dashboardStateSearch = dashboardStateSearch;
    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.action.name", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.action.name != null) {
            checkActionName();
        }
    });

    $scope.$watch("vm.action.actionSourceId", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.action.actionSourceId != null) {
            checkActionName();
        }
    });

    $scope.$watch("vm.action.targetDashboardId", function() {
        vm.selectedDashboardStateIds = [];
        if (vm.action.targetDashboardId) {
            dashboardService.getDashboard(vm.action.targetDashboardId).then(
                function success(dashboard) {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    var states = dashboard.configuration.states;
                    vm.selectedDashboardStateIds = Object.keys(states);
                }
            );
        }
    });

    $scope.$watch('vm.action.type', function(newType) {
        if (newType) {
            switch (newType) {
                case vm.types.widgetActionTypes.openDashboardState.value:
                case vm.types.widgetActionTypes.updateDashboardState.value:
                case vm.types.widgetActionTypes.openDashboard.value:
                    if (angular.isUndefined(vm.action.setEntityId)) {
                        vm.action.setEntityId = true;
                    }
                    break;
            }
        }
    });

    function checkActionName() {
        var actionNameIsUnique = true;
        if (vm.action.actionSourceId && vm.action.name) {
            var sourceActions = vm.widgetActions[vm.action.actionSourceId];
            if (sourceActions) {
                var result = $filter('filter')(sourceActions, {name: vm.action.name}, true);
                if (result && result.length && result[0].id !== vm.action.id) {
                    actionNameIsUnique = false;
                }
            }
        }
        $scope.theForm.name.$setValidity('actionNameNotUnique', actionNameIsUnique);
    }

    function actionSourceName (actionSource) {
        if (actionSource) {
            return utils.customTranslation(actionSource.name, actionSource.name);
        } else {
            return '';
        }
    }

    function dashboardStateSearch (query) {
        if (vm.action.type == vm.types.widgetActionTypes.openDashboard.value) {
            var deferred = $q.defer();
            var result = query ? vm.selectedDashboardStateIds.filter(
                createFilterForDashboardState(query)) : vm.selectedDashboardStateIds;
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([query]);
            }
            return deferred.promise;
        } else {
            return vm.fetchDashboardStates({query: query});
        }
    }

    function createFilterForDashboardState (query) {
        var lowercaseQuery = angular.lowercase(query);
        return function filterFn(stateId) {
            return (angular.lowercase(stateId).indexOf(lowercaseQuery) === 0);
        };
    }

    function cleanupAction(action) {
        var result = {};
        result.id = action.id;
        result.actionSourceId = action.actionSourceId;
        result.name = action.name;
        result.icon = action.icon;
        result.type = action.type;
        switch (action.type) {
            case vm.types.widgetActionTypes.openDashboardState.value:
            case vm.types.widgetActionTypes.updateDashboardState.value:
                result.targetDashboardStateId = action.targetDashboardStateId;
                result.openRightLayout = action.openRightLayout;
                result.setEntityId = action.setEntityId;
                result.stateEntityParamName = action.stateEntityParamName;
                break;
            case vm.types.widgetActionTypes.openDashboard.value:
                result.targetDashboardId = action.targetDashboardId;
                result.targetDashboardStateId = action.targetDashboardStateId;
                result.setEntityId = action.setEntityId;
                result.stateEntityParamName = action.stateEntityParamName;
                break;
            case vm.types.widgetActionTypes.custom.value:
                result.customFunction = action.customFunction;
                break;
        }
        return result;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(cleanupAction(vm.action));
    }
}
