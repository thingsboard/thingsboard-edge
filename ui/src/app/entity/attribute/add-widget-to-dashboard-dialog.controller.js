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
/* eslint-disable import/no-unresolved, import/default */

import selectTargetStateTemplate from '../../dashboard/states/select-target-state.tpl.html';
import selectTargetLayoutTemplate from '../../dashboard/layouts/select-target-layout.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AddWidgetToDashboardDialogController($scope, $mdDialog, $state, $q, $document, dashboardUtils,
                                                             utils, types, itembuffer, dashboardService, entityId, entityType, entityName, widget) {

    var vm = this;

    vm.widget = widget;
    vm.dashboardId = null;
    vm.addToDashboardType = 0;
    vm.newDashboard = {};
    vm.openDashboard = false;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function selectTargetState($event, dashboard) {
        var deferred = $q.defer();
        var states = dashboard.configuration.states;
        var stateIds = Object.keys(states);
        if (stateIds.length > 1) {
            $mdDialog.show({
                controller: 'SelectTargetStateController',
                controllerAs: 'vm',
                templateUrl: selectTargetStateTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    states: states
                },
                fullscreen: true,
                multiple: true,
                targetEvent: $event
            }).then(
                function success(stateId) {
                    deferred.resolve(stateId);
                },
                function fail() {
                    deferred.reject();
                }
            );

        } else {
            deferred.resolve(stateIds[0]);
        }
        return deferred.promise;
    }

    function selectTargetLayout($event, dashboard, targetState) {
        var deferred = $q.defer();
        var layouts = dashboard.configuration.states[targetState].layouts;
        var layoutIds = Object.keys(layouts);
        if (layoutIds.length > 1) {
            $mdDialog.show({
                controller: 'SelectTargetLayoutController',
                controllerAs: 'vm',
                templateUrl: selectTargetLayoutTemplate,
                parent: angular.element($document[0].body),
                fullscreen: true,
                multiple: true,
                targetEvent: $event
            }).then(
                function success(layoutId) {
                    deferred.resolve(layoutId);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.resolve(layoutIds[0]);
        }
        return deferred.promise;
    }

    function add($event) {
        if (vm.addToDashboardType === 0) {
            dashboardService.getDashboard(vm.dashboardId).then(
                function success(dashboard) {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    selectTargetState($event, dashboard).then(
                        function(targetState) {
                            selectTargetLayout($event, dashboard, targetState).then(
                                function(targetLayout) {
                                    addWidgetToDashboard(dashboard, targetState, targetLayout);
                                }
                            );
                       }
                    );
                },
                function fail() {}
            );
        } else {
            addWidgetToDashboard(vm.newDashboard, 'default', 'main');
        }

    }

    function addWidgetToDashboard(theDashboard, targetState, targetLayout) {
        var aliasesInfo = {
            datasourceAliases: {},
            targetDeviceAliases: {}
        };
        dashboardUtils.createSingleEntityFilter(entityType, entityId).then(
            (filter) => {
                aliasesInfo.datasourceAliases[0] = {
                    alias: entityName,
                    filter: filter
                };
                itembuffer.addWidgetToDashboard(theDashboard, targetState, targetLayout, vm.widget, aliasesInfo, null, 48, null, -1, -1).then(
                    function(theDashboard) {
                        dashboardService.saveDashboard(theDashboard).then(
                            function success(dashboard) {
                                $scope.theForm.$setPristine();
                                $mdDialog.hide();
                                if (vm.openDashboard) {
                                    var stateParams = {
                                        dashboardId: dashboard.id.id
                                    }
                                    var stateIds = Object.keys(dashboard.configuration.states);
                                    var stateIndex = stateIds.indexOf(targetState);
                                    if (stateIndex > 0) {
                                        stateParams.state = utils.objToBase64([ {id: targetState, params: {}} ]);
                                    }
                                    stateParams.edit = true;
                                    $state.go('home.dashboard', stateParams);
                                }
                            }
                        );
                    }
                );
            }
        );
    }

}
