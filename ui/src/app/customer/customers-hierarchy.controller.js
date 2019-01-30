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

import './customers-hierarchy.scss';

/*@ngInject*/
export default function CustomersHierarchyController($scope, types, securityTypes, $templateCache, $controller, $mdUtil,
                                                     $translate, $timeout, entityGroupService, entityService, userPermissionsService) {

    var vm = this;

    vm.types = types;

    vm.nodeIdCounter = 0;
    vm.selectedNodeId = -1;

    vm.parentIdToGroupAllNodeId = {};

    vm.entityGroupNodesMap = {};
    vm.customerNodesMap = {};
    vm.customerGroupsNodesMap = {};

    vm.internalIdToNodeIds = {};
    vm.internalIdToParentNodeIds = {};
    vm.nodeIdToInternalId = {};

    vm.viewLoaded = false;
    vm.viewMode = 'groups';
    vm.groupsStateParams = {
        hierarchyView: true,
        groupType: types.entityType.customer,
        hierarchyCallbacks: {
            viewLoaded: () => {
                vm.viewLoading = false;
            },
            groupSelected: onGroupSelected,
            refreshEntityGroups: refreshEntityGroups
        }
    };
    vm.groupsLocals = {};

    vm.groupStateParams = {
        hierarchyView: true,
        hierarchyCallbacks: {
            viewLoaded: () => {
                vm.viewLoading = false;
            },
            customerGroupsSelected: onCustomerGroupsSelected,
            customerAdded: customerAdded,
            customerUpdated: customerUpdated,
            customersDeleted: customersDeleted,
            groupUpdated: groupUpdated,
            groupDeleted: groupDeleted,
            refreshCustomerGroups: refreshCustomerGroups,
            groupAdded: groupAdded
        }
    };
    vm.groupLocals = {};

    vm.isFullscreen = false;

    vm.isNavTreeOpen = true;

    Object.defineProperty(vm, 'isNavTreeOpenReadonly', {
        get: function() { return vm.isNavTreeOpen },
        set: function() {}
    });

    vm.loadNodes = loadNodes;
    vm.onNodeSelected = onNodeSelected;
    vm.selectRootNode = selectRootNode;
    vm.nodeEditCallbacks = {};

    var groupTypes = [
        types.entityType.user,
        types.entityType.customer,
        types.entityType.asset,
        types.entityType.device,
        types.entityType.entityView,
        types.entityType.dashboard
    ];

    function loadNodes(node, cb) {
        var parentEntityGroupId;
        if (node.id === '#') {
            entityGroupService.getEntityGroups(vm.types.entityType.customer).then(
                (entityGroups) => {
                    cb(entityGroupsToNodes(node.id, null, entityGroups));
                    vm.groupsStateParams.nodeId = node.id;
                    if (!vm.viewLoaded) {
                        vm.groupsStateParams.hierarchyCallbacks.reload();
                        vm.viewLoaded = true;
                    }
                }
            );
        } else if (node.data && node.data.type) {
            if (node.data.type === "group") {
                var entityGroup = node.data.entity;
                if (entityGroup.type === vm.types.entityType.customer) {
                    entityService.getEntityGroupEntities(entityGroup.id.id, -1).then(
                        (customers) => {
                            cb(customersToNodes(node.id, entityGroup.id.id, customers));
                        }
                    );
                } else {
                    cb([]);
                }
            } else if (node.data.type === "customer") {
                var customer = node.data.entity;
                parentEntityGroupId = node.data.parentEntityGroupId;
                cb(loadNodesForCustomer(node.id, parentEntityGroupId, customer));
            } else if (node.data.type === "groups") {
                var owner = node.data.customer;
                parentEntityGroupId = node.data.parentEntityGroupId;
                entityGroupService.getEntityGroupsByOwnerId(owner.id.entityType, owner.id.id, node.data.groupsType).then(
                    (entityGroups) => {
                        cb(entityGroupsToNodes(node.id, parentEntityGroupId, entityGroups));
                    }
                );
            }
        }
    }

    function onGroupSelected(parentNodeId, groupId) {
        var nodesMap = vm.entityGroupNodesMap[parentNodeId];
        if (nodesMap) {
            var nodeId = nodesMap[groupId];
            if (nodeId) {
                $timeout(() => {
                    vm.nodeEditCallbacks.selectNode(nodeId);
                    if (!vm.nodeEditCallbacks.nodeIsOpen(nodeId)) {
                        vm.nodeEditCallbacks.openNode(nodeId);
                    }
                }, 0, false);
            }
        } else {
            openNode(parentNodeId, () => {onGroupSelected(parentNodeId, groupId)});
        }
    }

    function onCustomerGroupsSelected(parentNodeId, customerId, groupsType) {
        var nodesMap = vm.customerNodesMap[parentNodeId];
        if (nodesMap) {
            var customerNodeId = nodesMap[customerId];
            if (customerNodeId) {
                var customerGroupNodeMap = vm.customerGroupsNodesMap[customerNodeId];
                if (customerGroupNodeMap) {
                    var nodeId = customerGroupNodeMap[groupsType];
                    if (nodeId) {
                        $timeout(() => {
                            vm.nodeEditCallbacks.selectNode(nodeId);
                            if (!vm.nodeEditCallbacks.nodeIsOpen(nodeId)) {
                                vm.nodeEditCallbacks.openNode(nodeId);
                            }
                        }, 0, false);
                    }
                } else {
                    openNode(customerNodeId, () => {onCustomerGroupsSelected(parentNodeId, customerId, groupsType)});
                }
            }
        } else {
            openNode(parentNodeId, () => {onCustomerGroupsSelected(parentNodeId, customerId, groupsType)});
        }
    }

    //entity group level
    function customerAdded(parentNodeId, customer) {

        var parentInternalId = vm.nodeIdToInternalId[parentNodeId];
        var parentNodeIds = vm.internalIdToNodeIds[parentInternalId];
        if (parentNodeIds) {
            var targetParentNodeIds = angular.copy(parentNodeIds);
            for (var i=0;i<parentNodeIds.length;i++) {
                var nodeId = parentNodeIds[i];
                var groupAllParentId = vm.nodeEditCallbacks.getParentNodeId(nodeId);
                if (groupAllParentId) {
                    var groupAllNodeId = vm.parentIdToGroupAllNodeId[groupAllParentId];
                    if (groupAllNodeId && targetParentNodeIds.indexOf(groupAllNodeId) === -1) {
                        targetParentNodeIds.push(groupAllNodeId);
                    }
                }
            }
            for (var n=0;n<targetParentNodeIds.length;n++) {
                var targetParentNodeId = targetParentNodeIds[n];
                if (vm.nodeEditCallbacks.nodeIsLoaded(targetParentNodeId)) {
                    var groupId = vm.nodeIdToInternalId[targetParentNodeId];
                    if (groupId) {
                        var node = createCustomerNode(targetParentNodeId, customer, groupId);
                        vm.nodeEditCallbacks.createNode(targetParentNodeId, node, 'last');
                    }
                }
            }
        }
    }

    //entity group level
    function customerUpdated(customer) {
        var nodeIds = vm.internalIdToNodeIds[customer.id.id];
        if (nodeIds) {
            for (var i=0;i<nodeIds.length;i++) {
                var nodeId = nodeIds[i];
                vm.nodeEditCallbacks.updateNode(nodeId, customer.title);
            }
        }
    }

    //entity group level
    function customersDeleted(customerIds) {
        for (var i=0;i<customerIds.length;i++) {
            var customerId = customerIds[i];
            var nodeIds = vm.internalIdToNodeIds[customerId];
            for (var n=0;n<nodeIds.length;n++) {
                var nodeId = nodeIds[n];
                vm.nodeEditCallbacks.deleteNode(nodeId);
            }
        }
    }

    //entity group level
    function groupUpdated(entityGroup) {
        var nodeIds = vm.internalIdToNodeIds[entityGroup.id.id];
        if (nodeIds) {
            for (var i=0;i<nodeIds.length;i++) {
                var nodeId = nodeIds[i];
                vm.nodeEditCallbacks.updateNode(nodeId, entityGroup.name);
            }
        }
    }

    //entity group level
    function groupDeleted(groupNodeId, entityGroupId) {
        var parentId = vm.nodeEditCallbacks.getParentNodeId(groupNodeId);
        if (parentId) {
            $timeout(() => {
                vm.nodeEditCallbacks.selectNode(parentId);
            }, 0, false);
        }
        var nodeIds = vm.internalIdToNodeIds[entityGroupId];
        for (var n=0;n<nodeIds.length;n++) {
            var nodeId = nodeIds[n];
            vm.nodeEditCallbacks.deleteNode(nodeId);
        }
    }

    //entity group level
    function refreshCustomerGroups(customerGroupIds) {
        for (var i=0;i<customerGroupIds.length;i++) {
            var groupId = customerGroupIds[i];
            var nodeIds = vm.internalIdToNodeIds[groupId];
            if (nodeIds) {
                for (var n = 0; n < nodeIds.length; n++) {
                    var nodeId = nodeIds[n];
                    if (vm.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
                        vm.nodeEditCallbacks.refreshNode(nodeId);
                    }
                }
            }
        }
    }

    //entity group level
    function groupAdded(entityGroup, existingGroupId) {
        var parentNodeIds = vm.internalIdToParentNodeIds[existingGroupId];
        if (parentNodeIds) {
            for (var i=0;i<parentNodeIds.length; i++) {
                var parentNodeId = parentNodeIds[i];
                if (vm.nodeEditCallbacks.nodeIsLoaded(parentNodeId)) {
                    var parentEntityGroupId = null;
                    var parentNode = vm.nodeEditCallbacks.getNode(parentNodeId);
                    if (parentNode && parentNode.data && parentNode.data.parentEntityGroupId) {
                        parentEntityGroupId = parentNode.data.parentEntityGroupId;
                    }
                    var node = createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId);
                    vm.nodeEditCallbacks.createNode(parentNodeId, node, 'last');
                }
            }
        }
    }

    //entity groups level
    function refreshEntityGroups(internalId) {
        var nodeIds;
        if (internalId && internalId != '#') {
            nodeIds = vm.internalIdToNodeIds[internalId];
        } else {
            nodeIds = ['#'];
        }
        if (nodeIds) {
            for (var i=0;i<nodeIds.length; i++) {
                var nodeId = nodeIds[i];
                if (vm.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
                    vm.nodeEditCallbacks.refreshNode(nodeId);
                }
            }
        }
    }

    function openNode(nodeId, openCb) {
        if (!vm.nodeEditCallbacks.nodeIsOpen(nodeId)) {
            vm.nodeEditCallbacks.openNode(nodeId, openCb);
        }
    }

    function selectRootNode() {
        $timeout(() => {
            vm.nodeEditCallbacks.deselectAll();
        }, 0, false);
    }

    function entityGroupsToNodes(parentNodeId, parentEntityGroupId, entityGroups) {
        var nodes = [];
        var nodesMap = {};
        vm.entityGroupNodesMap[parentNodeId] = nodesMap;
        if (entityGroups) {
            for (var i = 0; i < entityGroups.length; i++) {
                var entityGroup = entityGroups[i];
                var node = createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId);
                nodes.push(node);
                if (entityGroup.groupAll) {
                    vm.parentIdToGroupAllNodeId[parentNodeId] = node.id;
                }
            }
        }
        return nodes;
    }

    function createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId) {
        var nodesMap = vm.entityGroupNodesMap[parentNodeId];
        if (!nodesMap) {
            nodesMap = {};
            vm.entityGroupNodesMap[parentNodeId] = nodesMap;
        }
        var node = {
            id: ++vm.nodeIdCounter,
            icon: 'material-icons ' + iconForGroupType(entityGroup.type),
            text: entityGroup.name,
            children: entityGroup.type === vm.types.entityType.customer,
            data: {
                type: "group",
                entity: entityGroup,
                parentEntityGroupId: parentEntityGroupId,
                internalId: entityGroup.id.id
            }
        };
        nodesMap[entityGroup.id.id] = node.id;
        registerNode(node, parentNodeId);
        return node;
    }

    function customersToNodes(parentNodeId, groupId, customers) {
        var nodes = [];
        var nodesMap = {};
        vm.customerNodesMap[parentNodeId] = nodesMap;
        if (customers) {
            for (var i = 0; i < customers.length; i++) {
                var customer = customers[i];
                var node = createCustomerNode(parentNodeId, customer, groupId);
                nodes.push(node);
            }
        }
        return nodes;
    }

    function createCustomerNode(parentNodeId, customer, groupId) {
        var nodesMap = vm.customerNodesMap[parentNodeId];
        if (!nodesMap) {
            nodesMap = {};
            vm.customerNodesMap[parentNodeId] = nodesMap;
        }
        var node = {
            id: ++vm.nodeIdCounter,
            icon: 'material-icons tb-customer',
            text: customer.title,
            children: true,
            state: {
                disabled: true
            },
            data: {
                type: "customer",
                entity: customer,
                parentEntityGroupId: groupId,
                internalId: customer.id.id
            }
        };
        nodesMap[customer.id.id] = node.id;
        registerNode(node, parentNodeId);
        return node;
    }

    function loadNodesForCustomer(parentNodeId, parentEntityGroupId, customer) {
        var nodes = [];
        var nodesMap = {};
        vm.customerGroupsNodesMap[parentNodeId] = nodesMap;
        for (var i=0; i<groupTypes.length;i++) {
            var groupType = groupTypes[i];
            if (userPermissionsService.hasGenericPermission(securityTypes.groupResourceByGroupType[groupType], securityTypes.operation.read)) {
                var node = {
                    id: ++vm.nodeIdCounter,
                    icon: 'material-icons ' + iconForGroupType(groupType),
                    text: textForGroupType(groupType),
                    children: true,
                    data: {
                        type: "groups",
                        groupsType: groupType,
                        customer: customer,
                        parentEntityGroupId: parentEntityGroupId,
                        internalId: customer.id.id + '_' + groupType
                    }
                };
                nodes.push(node);
                nodesMap[groupType] = node.id;

                registerNode(node, parentNodeId);
            }
        }
        return nodes;
    }


    function registerNode(node, parentNodeId) {
        var nodeIds = vm.internalIdToNodeIds[node.data.internalId];
        if (!nodeIds) {
            nodeIds = [];
            vm.internalIdToNodeIds[node.data.internalId] = nodeIds;
        }
        if (nodeIds.indexOf(node.id) === -1) {
            nodeIds.push(node.id);
        }
        var parentNodeIds = vm.internalIdToParentNodeIds[node.data.internalId];
        if (!parentNodeIds) {
            parentNodeIds = [];
            vm.internalIdToParentNodeIds[node.data.internalId] = parentNodeIds;
        }
        if (parentNodeIds.indexOf(parentNodeId) === -1) {
            parentNodeIds.push(parentNodeId);
        }

        vm.nodeIdToInternalId[node.id] = node.data.internalId;
    }

    function iconForGroupType(groupType) {
        switch (groupType) {
            case vm.types.entityType.user:
                return 'tb-user-group';
            case vm.types.entityType.customer:
                return 'tb-customer-group';
            case vm.types.entityType.asset:
                return 'tb-asset-group';
            case vm.types.entityType.device:
                return 'tb-device-group';
            case vm.types.entityType.entityView:
                return 'tb-entity-view-group';
            case vm.types.entityType.dashboard:
                return 'tb-dashboard-group';
        }
        return '';
    }

    function textForGroupType(groupType) {
        switch (groupType) {
            case vm.types.entityType.user:
                return $translate.instant('entity-group.user-groups');
            case vm.types.entityType.customer:
                return $translate.instant('entity-group.customer-groups');
            case vm.types.entityType.asset:
                return $translate.instant('entity-group.asset-groups');
            case vm.types.entityType.device:
                return $translate.instant('entity-group.device-groups');
            case vm.types.entityType.entityView:
                return $translate.instant('entity-group.entity-view-groups');
            case vm.types.entityType.dashboard:
                return $translate.instant('entity-group.dashboard-groups');
        }
        return '';
    }

    function onNodeSelected(node) {
        var nodeId;
        if (!node) {
            nodeId = -1;
        } else {
            nodeId = node.id;
        }
        if (vm.selectedNodeId !== nodeId) {
            vm.selectedNodeId = nodeId;
            if (nodeId === -1) {
                vm.viewMode = 'groups';
                vm.groupsStateParams.groupType = vm.types.entityType.customer;
                vm.groupsStateParams.nodeId = '#';
                vm.groupsStateParams.internalId = '#';
                delete vm.groupsStateParams.entityGroupId;
                delete vm.groupsStateParams.childGroupType;
                delete vm.groupsStateParams.customerId;
                vm.groupsStateParams.hierarchyCallbacks.reload();
            } else if (node.data.type === "groups" || node.data.type === "group") {
                vm.viewLoading = true;
                $mdUtil.nextTick(() => {
                    var parentEntityGroupId;
                    if (node.data.type === "groups") {
                        vm.viewMode = 'groups';
                        parentEntityGroupId = node.data.parentEntityGroupId;
                        if (parentEntityGroupId) {
                            vm.groupsStateParams.entityGroupId = parentEntityGroupId;
                            vm.groupsStateParams.groupType = vm.types.entityType.customer;
                            vm.groupsStateParams.childGroupType = node.data.groupsType;
                        } else {
                            vm.groupsStateParams.groupType = node.data.groupsType;
                        }
                        vm.groupsStateParams.customerId = node.data.customer.id.id;
                        vm.groupsStateParams.nodeId = node.id;
                        vm.groupsStateParams.internalId = node.data.internalId;
                        vm.groupsStateParams.hierarchyCallbacks.reload();
                    } else if (node.data.type === "group") {
                        vm.viewMode = 'group';
                        var entityGroup = node.data.entity;
                        parentEntityGroupId = node.data.parentEntityGroupId;
                        if (parentEntityGroupId) {
                            vm.groupStateParams.entityGroupId = parentEntityGroupId;
                            vm.groupStateParams.groupType = vm.types.entityType.customer;
                            vm.groupStateParams.childEntityGroupId = entityGroup.id.id;
                            vm.groupStateParams.childGroupType = entityGroup.type;
                        } else {
                            vm.groupStateParams.entityGroupId = entityGroup.id.id;
                            vm.groupStateParams.groupType = entityGroup.type;
                            delete vm.groupStateParams.childEntityGroupId;
                            delete vm.groupStateParams.childGroupType;
                        }
                        if (entityGroup.ownerId.entityType === vm.types.entityType.customer) {
                            vm.groupStateParams.customerId = entityGroup.ownerId.id;
                        } else {
                            vm.groupStateParams.customerId = null;
                        }
                        vm.groupStateParams.entityGroup = angular.copy(entityGroup);
                        vm.groupStateParams.nodeId = node.id;
                        vm.groupStateParams.internalId = node.data.internalId;
                        vm.groupStateParams.hierarchyCallbacks.reload();
                    }
                });
            }
        }
    }
}
