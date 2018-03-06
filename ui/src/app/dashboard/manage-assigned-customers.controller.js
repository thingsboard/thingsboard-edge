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
/*@ngInject*/
export default function ManageAssignedCustomersController($mdDialog, $q, types, dashboardService, actionType, dashboardIds, assignedCustomers) {

    var vm = this;

    vm.types = types;
    vm.actionType = actionType;
    vm.dashboardIds = dashboardIds;
    vm.assignedCustomers = assignedCustomers;
    if (actionType != 'manage') {
        vm.assignedCustomers = [];
    }

    if (actionType == 'manage') {
        vm.titleText = 'dashboard.manage-assigned-customers';
        vm.labelText = 'dashboard.assigned-customers';
        vm.actionName = 'action.update';
    } else if (actionType == 'assign') {
        vm.titleText = 'dashboard.assign-to-customers';
        vm.labelText = 'dashboard.assign-to-customers-text';
        vm.actionName = 'action.assign';
    } else if (actionType == 'unassign') {
        vm.titleText = 'dashboard.unassign-from-customers';
        vm.labelText = 'dashboard.unassign-from-customers-text';
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
                promise = dashboardService.updateDashboardCustomers(dashboardId, vm.assignedCustomers);
            } else if (vm.actionType == 'assign') {
                promise = dashboardService.addDashboardCustomers(dashboardId, vm.assignedCustomers);
            } else if (vm.actionType == 'unassign') {
                promise = dashboardService.removeDashboardCustomers(dashboardId, vm.assignedCustomers);
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
