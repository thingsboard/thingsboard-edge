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
export default function ManageAssignedEdgeGroupsController($mdDialog, $q, types, entityService, actionType, entityIds, assignedEdgeGroupIds, targetGroupType) {

    var vm = this;

    vm.types = types;
    vm.entityService = entityService;
    vm.actionType = actionType;
    vm.entityIds = entityIds;
    vm.assignedEdgeGroupIds = assignedEdgeGroupIds;
    vm.targetGroupType = targetGroupType;

    if (actionType != 'manage') {
        vm.assignedEdgeGroupIds = [];
    }

    if (actionType == 'manage') {
        vm.actionName = 'action.update';
        switch (vm.targetGroupType) {
            case 'EntityGroup':
                vm.titleText = 'entity-group.manage-assigned-edge-groups';
                vm.labelText = 'entity-group.assigned-edge-groups';
                break;
            case 'RuleChain':
                vm.titleText = 'rulechain.manage-assigned-edge-groups';
                vm.labelText = 'rulechain.assigned-edge-groups';
                break;
            case 'Scheduler':
                vm.titleText = 'scheduler.manage-assigned-edge-groups';
                vm.labelText = 'scheduler.assigned-edge-groups';
                break;
        }
    } else if (actionType == 'assign') {
        vm.actionName = 'action.assign';
        switch (vm.entityService) {
            case 'EntityGroup':
                vm.titleText = 'entity-group.assign-to-edge-groups';
                vm.labelText = 'entity-group.assign-to-edge-groups-text';
                break;
            case 'RuleChain':
                vm.titleText = 'rulechain.manage-assigned-edge-groups';
                vm.labelText = 'rulechain.assigned-edge-groups';
                break;
            case 'Scheduler':
                vm.titleText = 'scheduler.manage-assigned-edge-groups';
                vm.labelText = 'scheduler.assigned-edge-groups';
                break;
        }
    } else if (actionType == 'unassign') {
        vm.actionName = 'action.unassign';
        switch (vm.targetGroupType) {
            case 'EntityGroup':
                vm.titleText = 'entity-group.unassign-from-edge-groups';
                vm.labelText = 'entity-group.unassign-from-edge-groups-text';
                break;
            case 'RuleChain':
                vm.titleText = 'rulechain.manage-assigned-edge-groups';
                vm.labelText = 'rulechain.assigned-edge-groups';
                break;
            case 'Scheduler':
                vm.titleText = 'scheduler.manage-assigned-edge-groups';
                vm.labelText = 'scheduler.assigned-edge-groups';
                break;
        }
    }

    vm.submit = submit;
    vm.cancel = cancel;

    function cancel () {
        $mdDialog.cancel();
    }

    function submit () {
        var tasks = [];
        for (var i=0;i<vm.entityIds.length;i++) {
            var entityId = vm.entityIds[i];
            var promise;
            switch (vm.actionType) {
                case 'manage':
                    switch (vm.targetGroupType) {
                        case 'EntityGroup':
                            promise = entityService.updateEntityGroupEdges(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'RuleChain':
                            promise = entityService.updateRuleChainEdgeGroups(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'Scheduler':
                            promise = entityService.updateSchedulerEdgeGroups();
                            break;
                    }
                    break;
                case 'assign':
                    switch (vm.targetGroupType) {
                        case 'EntityGroup':
                            promise = entityService.addEntityGroupEdges(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'RuleChain':
                            promise = entityService.addRuleChainEdgeGroups(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'Scheduler':
                            promise = entityService.addSchedulerEdgeGroups(entityId, vm.assignedEdgeGroupIds);
                            break;
                    }
                    break;
                case 'unassign':
                    switch (vm.targetGroupType) {
                        case 'EntityGroup':
                            promise = entityService.removeEntityGroupEdges(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'RuleChain':
                            promise = entityService.removeRuleChainEdgeGroups(entityId, vm.assignedEdgeGroupIds);
                            break;
                        case 'Scheduler':
                            promise = entityService.removeSchedulerEdgeGroups(entityId, vm.assignedEdgeGroupIds);
                            break;
                    }
                    break;
            }
            tasks.push(promise);
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

}
