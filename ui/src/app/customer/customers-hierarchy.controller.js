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
export default function CustomersHierarchyController(types, securityTypes, $timeout, $translate, entityGroupService, entityService, userPermissionsService) {

    var vm = this;

    vm.types = types;

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


    /*$timeout(() => {
        vm.nodeEditCallbacks.updateNode("ENTITY_GROUP_1", "New name");
    }, 5000);*/


    function loadNodes(node, cb) {
        if (node.id === '#') {
            entityGroupService.getEntityGroups(vm.types.entityType.customer).then(
                (entityGroups) => {
                    cb(entityGroupsToNodes(entityGroups));
                }
            );
        } else if (node.data && node.data.type) {
            if (node.data.type === "group") {
                var entityGroup = node.data.entity;
                if (entityGroup.type === vm.types.entityType.customer) {
                    entityService.getEntityGroupEntities(entityGroup.id.id, -1).then(
                        (customers) => {
                            cb(customersToNodes(entityGroup.id.id, customers));
                        }
                    );
                } else {
                    cb([]);
                }
            } else if (node.data.type === "customer") {
                var customer = node.data.entity;
                cb(loadNodesForCustomer(customer));
            } else if (node.data.type === "groups") {
                var owner = node.data.customer;
                entityGroupService.getEntityGroupsByOwnerId(owner.id.entityType, owner.id.id, node.data.groupsType).then(
                    (entityGroups) => {
                        cb(entityGroupsToNodes(entityGroups));
                    }
                );
            }
        }
    }

    function entityGroupsToNodes(entityGroups) {
        var nodes = [];
        if (entityGroups) {
            for (var i = 0; i < entityGroups.length; i++) {
                var entityGroup = entityGroups[i];
                var node = {
                    id: entityGroup.id.entityType + "_" + entityGroup.id.id,
                    icon: 'material-icons ' + iconForGroupType(entityGroup.type),
                    text: entityGroup.name,
                    children: entityGroup.type === vm.types.entityType.customer,
                    data: {
                        type: "group",
                        entity: entityGroup
                    }
                };
                nodes.push(node);
            }
        }
        return nodes;
    }

    function customersToNodes(groupId, customers) {
        var nodes = [];
        if (customers) {
            for (var i = 0; i < customers.length; i++) {
                var customer = customers[i];
                var node = {
                    id: groupId + "_" + customer.id.entityType + "_" + customer.id.id,
                    icon: 'material-icons tb-customer',
                    text: customer.title,
                    children: true,
                    data: {
                        type: "customer",
                        entity: customer
                    }
                };
                nodes.push(node);
            }
        }
        return nodes;
    }

    function loadNodesForCustomer(customer) {
        var nodes = [];
        for (var i=0; i<groupTypes.length;i++) {
            var groupType = groupTypes[i];
            if (userPermissionsService.hasGenericPermission(securityTypes.groupResourceByGroupType[groupType], securityTypes.operation.read)) {
                var node = {
                    id: "GROUPS_"+ groupType + "_" + customer.id.id,
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
        console.log('node selected!'); //eslint-disable-line
        console.log(node); //eslint-disable-line
    }
}
