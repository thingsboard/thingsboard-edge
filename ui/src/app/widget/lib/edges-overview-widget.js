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
import './edges-overview-widget.scss';

/* eslint-disable import/no-unresolved, import/default */
import edgesOverviewWidgetTemplate from './edges-overview-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.edgesOverviewWidget', [])
    .directive('tbEdgesOverviewWidget', EdgesOverviewWidget)
    .name;
/* eslint-disable no-unused-vars, no-undef */
/*@ngInject*/
function EdgesOverviewWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: EdgesOverviewWidgetController,
        controllerAs: 'vm',
        templateUrl: edgesOverviewWidgetTemplate
    };
}

/*@ngInject*/
function EdgesOverviewWidgetController($scope, $translate, types, utils, entityService, edgeService, customerService, userService, entityGroupService) {
    var vm = this;

    vm.showData = true;

    vm.nodeIdCounter = 0;

    vm.entityNodesMap = {};
    vm.entityGroupsNodesMap = {};

    vm.customerTitle = null;
    vm.edgeIsDatasource = true;

    var edgeGroupsTypes = [
        types.entityType.user,
        types.entityType.customer,
        types.entityType.asset,
        types.entityType.device,
        types.entityType.entityView,
        types.entityType.dashboard,
        types.entityType.rulechain
    ];

    vm.onNodeSelected = onNodeSelected;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            updateDatasources();
        }
    });

    function onNodeSelected(node, event) {
        var nodeId;
        if (!node) {
            nodeId = -1;
        } else {
            nodeId = node.id;
        }
        if (nodeId !== -1) {
            var selectedNode = vm.entityNodesMap[nodeId];
            if (selectedNode) {
                var descriptors = vm.ctx.actionsApi.getActionDescriptors('nodeSelected');
                if (descriptors.length) {
                    var entity = selectedNode.data.nodeCtx.entity;
                    vm.ctx.actionsApi.handleWidgetAction(event, descriptors[0], entity.id, entity.name, { nodeCtx: selectedNode.data.nodeCtx });
                }
            }
        }
    }

    function updateDatasources() {
        vm.loadNodes = loadNodes;
    }

    function loadNodes(node, cb) {
        var datasource = vm.datasources[0];
        if (node.id === '#' && datasource) {
            if (datasource.type === types.datasourceType.entity && datasource.entity && datasource.entity.id.entityType === types.entityType.edge) {
                var selectedEdge = datasource.entity;
                vm.customerTitle = getCustomerTitle(selectedEdge.id.id);
                cb(loadNodesForEdge(selectedEdge.id.id, selectedEdge));
            } else if (datasource.type === types.datasourceType.function) {
                cb(loadNodesForEdge(null, null));
            } else {
                vm.edgeIsDatasource = false;
                cb([]);
            }
        } else if (node.data && node.data.entity && node.data.entity.id.entityType === types.entityType.edge) {
            var edgeId = node.data.entity.id.id;
            var groupType = node.data.entityType;
            entityGroupService.getEdgeEntityGroups(edgeId, groupType).then(
                (entities) => {
                    if (entities.data.length > 0) {
                        cb(entitiesToNodes(node.id, entities.data));
                    } else {
                        cb([]);
                    }
                }
            );
        } else {
            cb([]);
        }
    }

    function entitiesToNodes(parentNodeId, entities) {
        var nodes = [];
        vm.entityNodesMap[parentNodeId] = {};
        if (entities) {
            entities.forEach(
                (entity) => {
                    var node = createEntityNode(parentNodeId, entity, entity.id.entityType);
                    nodes.push(node);
                }
            );
        }
        return nodes;
    }

    function createEntityNode(parentNodeId, entity, entityType) {
        var nodesMap = vm.entityNodesMap[parentNodeId];
        if (!nodesMap) {
            nodesMap = {};
            vm.entityNodesMap[parentNodeId] = nodesMap;
        }
        var node = {
            id: ++vm.nodeIdCounter,
            icon: 'material-icons ' + iconForGroupType(entityType),
            text: entity.name,
            children: false,
            data: {
                entity,
                internalId: entity.id.id
            }
        };
        nodesMap[entity.id.id] = node.id;
        return node;
    }

    function loadNodesForEdge(parentNodeId, entity) {
        var nodes = [];
        vm.entityGroupsNodesMap[parentNodeId] = {};
        var allowedGroupTypes = groupTypes;
        if (userService.getAuthority() === 'CUSTOMER_USER') {
            allowedGroupTypes = groupTypes.filter(type => type !== types.entityType.rulechain);
        }
        allowedGroupTypes.forEach(
            (entityType) => {
                var node = {
                    id: ++vm.nodeIdCounter,
                    icon: 'material-icons ' + iconForGroupType(entityType),
                    text: textForGroupType(entityType),
                    children: true,
                    data: {
                        entityType,
                        entity,
                        internalId: entity ? entity.id.id + '_' + entityType : utils.guid()
                    }
                };
                nodes.push(node);
            }
        )
        return nodes;
    }

    function iconForGroupType(entityType) {
        switch (entityType) {
            case types.entityType.asset:
                return 'tb-asset-group';
            case types.entityType.device:
                return 'tb-device-group';
            case types.entityType.entityView:
                return 'tb-entity-view-group';
            case types.entityType.dashboard:
                return 'tb-dashboard-group';
            case types.entityType.rulechain:
                return 'tb-rule-chain-group';
        }
        return '';
    }

    function textForGroupType(entityType) {
        switch (entityType) {
            case types.entityType.asset:
                return $translate.instant('asset.assets');
            case types.entityType.device:
                return $translate.instant('device.devices');
            case types.entityType.entityView:
                return $translate.instant('entity-view.entity-views');
            case types.entityType.rulechain:
                return $translate.instant('rulechain.rulechains');
            case types.entityType.dashboard:
                return $translate.instant('dashboard.dashboards');
        }
        return '';
    }

    function getCustomerTitle(edgeId) {
        edgeService.getEdge(edgeId, true).then(
            function success(edge) {
                if (edge.customerId.id !== types.id.nullUid) {
                    customerService.getCustomer(edge.customerId.id, { ignoreErrors: true }).then(
                        function success(customer) {
                            vm.customerTitle = $translate.instant('edge.assigned-to-customer-widget', { customerTitle: customer.title });
                        },
                        function fail() {
                        }
                    );
                }
            },
            function fail() {
            }
        )
    }

}
