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
export default function ManageAssignedEdgesToDashboardController($mdDialog, $q, types, dashboardService, actionType, dashboardIds, assignedEdges) {

    var vm = this;

    vm.types = types;
    vm.actionType = actionType;
    vm.dashboardIds = dashboardIds;
    vm.assignedEdges = assignedEdges;
    if (actionType != 'manage') {
        vm.assignedEdges = [];
    }

    if (actionType == 'manage') {
        vm.titleText = 'dashboard.manage-assigned-edges';
        vm.labelText = 'dashboard.assigned-edges';
        vm.actionName = 'action.update';
    } else if (actionType == 'assign') {
        vm.titleText = 'dashboard.assign-to-edges';
        vm.labelText = 'dashboard.assign-to-edges-text';
        vm.actionName = 'action.assign';
    } else if (actionType == 'unassign') {
        vm.titleText = 'dashboard.unassign-from-edges';
        vm.labelText = 'dashboard.unassign-from-edges-text';
        vm.actionName = 'action.unassign';
    }

    vm.submit = submit;
    vm.cancel = cancel;

    function cancel () {
        $mdDialog.cancel();
    }

    function submit () {
        var tasks = [];
        for (var i=0;i<vm.dashboardIds.length;i++) {
            var dashboardId = vm.dashboardIds[i];
            var promise;
            if (vm.actionType == 'manage') {
                promise = dashboardService.updateDashboardEdges(dashboardId, vm.assignedEdges);
            } else if (vm.actionType == 'assign') {
                promise = dashboardService.addDashboardEdges(dashboardId, vm.assignedEdges);
            } else if (vm.actionType == 'unassign') {
                promise = dashboardService.removeDashboardEdges(dashboardId, vm.assignedEdges);
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
