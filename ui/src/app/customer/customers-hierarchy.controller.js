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

    vm.entityGroupNodesMap = {};
    vm.customerNodesMap = {};
    vm.customerGroupsNodesMap = {};

    vm.viewMode = 'groups';
    vm.groupsStateParams = {
        hierarchyView: true,
        groupType: types.entityType.customer,
        hierarchyCallbacks: {
            viewLoaded: () => {
                vm.viewLoading = false;
            },
            groupSelected: onGroupSelected,
            refreshNode: refreshNode
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
            refreshNode: refreshNode
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
        if (node.id === '#') {
            entityGroupService.getEntityGroups(vm.types.entityType.customer).then(
                (entityGroups) => {
                    cb(entityGroupsToNodes(node.id, entityGroups));
                    vm.groupsStateParams.nodeId = node.id;
                    vm.groupsStateParams.hierarchyCallbacks.reload();
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
                cb(loadNodesForCustomer(node.id, customer));
            } else if (node.data.type === "groups") {
                var owner = node.data.customer;
                entityGroupService.getEntityGroupsByOwnerId(owner.id.entityType, owner.id.id, node.data.groupsType).then(
                    (entityGroups) => {
                        cb(entityGroupsToNodes(node.id, entityGroups));
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

    function refreshNode(nodeId, entityId, entityGroupIds, selectParentNode) {
        if (vm.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
            vm.nodeEditCallbacks.refreshNode(nodeId);
        }
        if (entityId) {
            //vm.entityGroupNodesMap = {};
            //vm.customerNodesMap = {};
            //vm.customerGroupsNodesMap = {};
            for (var parentNodeId in vm.customerNodesMap) {
                var nodesMap = vm.customerNodesMap[parentNodeId];
                if (nodesMap[entityId]) {
                    if (vm.nodeEditCallbacks.nodeIsLoaded(parentNodeId)) {
                        vm.nodeEditCallbacks.refreshNode(parentNodeId);
                    }
                    continue;
                }
            }
            /*for (parentNodeId in vm.customerGroupsNodesMap) {
                nodesMap = vm.customerGroupsNodesMap[parentNodeId];
                if (nodesMap[entityId]) {
                    if (vm.nodeEditCallbacks.nodeIsLoaded(parentNodeId)) {
                        vm.nodeEditCallbacks.refreshNode(parentNodeId);
                    }
                    continue;
                }
            }*/
        }
        if (entityGroupIds) {
            for (var i=0;i<entityGroupIds.length;i++) {
                var entityGroupId = entityGroupIds[i];
                for (parentNodeId in vm.entityGroupNodesMap) {
                    nodesMap = vm.entityGroupNodesMap[parentNodeId];
                    if (nodesMap[entityGroupId]) {
                        if (vm.nodeEditCallbacks.nodeIsLoaded(parentNodeId)) {
                            vm.nodeEditCallbacks.refreshNode(parentNodeId);
                        }
                        continue;
                    }
                }
            }
        }
    }

    function openNode(nodeId, openCb) {
        if (!vm.nodeEditCallbacks.nodeIsOpen(nodeId)) {
            vm.nodeEditCallbacks.openNode(nodeId, openCb);
        }
    }

    function entityGroupsToNodes(parentNodeId, entityGroups) {
        var nodes = [];
        var nodesMap = {};
        vm.entityGroupNodesMap[parentNodeId] = nodesMap;
        if (entityGroups) {
            for (var i = 0; i < entityGroups.length; i++) {
                var entityGroup = entityGroups[i];
                var node = {
                    id: ++vm.nodeIdCounter,
                    icon: 'material-icons ' + iconForGroupType(entityGroup.type),
                    text: entityGroup.name,
                    children: entityGroup.type === vm.types.entityType.customer,
                    data: {
                        type: "group",
                        entity: entityGroup
                    }
                };
                nodes.push(node);
                nodesMap[entityGroup.id.id] = node.id;
            }
        }
        return nodes;
    }

    function customersToNodes(parentNodeId, groupId, customers) {
        var nodes = [];
        var nodesMap = {};
        vm.customerNodesMap[parentNodeId] = nodesMap;
        if (customers) {
            for (var i = 0; i < customers.length; i++) {
                var customer = customers[i];
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
                        entity: customer
                    }
                };
                nodes.push(node);
                nodesMap[customer.id.id] = node.id;
            }
        }
        return nodes;
    }

    function loadNodesForCustomer(parentNodeId, customer) {
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
                        customer: customer
                    }
                };
                nodes.push(node);
                nodesMap[groupType] = node.id;
            }
        }
        return nodes;
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
        if (!node) {
            return;
        }
        if (vm.selectedNodeId !== node.id) {
            vm.selectedNodeId = node.id;
            $scope.$apply(() => {
                if (node.data.type === "groups" || node.data.type === "group") {
                    vm.viewLoading = true;
                    $mdUtil.nextTick(() => {
                        if (node.data.type === "groups") {
                            vm.viewMode = 'groups';
                            vm.groupsStateParams.customerId = node.data.customer.id.id;
                            vm.groupsStateParams.groupType = node.data.groupsType;
                            vm.groupsStateParams.nodeId = node.id;
                            vm.groupsStateParams.hierarchyCallbacks.reload();
                        } else if (node.data.type === "group") {
                            vm.viewMode = 'group';
                            var entityGroup = node.data.entity;
                            vm.groupStateParams.entityGroupId = entityGroup.id.id;
                            if (entityGroup.ownerId.entityType === vm.types.entityType.customer) {
                                vm.groupStateParams.customerId = entityGroup.ownerId.id;
                            } else {
                                vm.groupStateParams.customerId = null;
                            }
                            vm.groupStateParams.entityGroup = angular.copy(entityGroup);
                            vm.groupStateParams.nodeId = node.id;
                            vm.groupStateParams.hierarchyCallbacks.reload();
                        }
                    });
                }
            });
        }
    }
}
