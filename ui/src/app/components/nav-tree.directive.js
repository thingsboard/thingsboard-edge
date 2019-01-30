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

import './nav-tree.scss';

/* eslint-disable import/no-unresolved, import/default */

import navTreeTemplate from './nav-tree.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.navTree', [])
    .directive('tbNavTree', NavTree)
    .name;

/*@ngInject*/
function NavTree() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            loadNodes: '=',
            editCallbacks: '=',
            onNodeSelected: '&'
        },
        controller: NavTreeController,
        controllerAs: 'vm',
        templateUrl: navTreeTemplate
    };
}

/*@ngInject*/
function NavTreeController($scope, $element, types) {

    var vm = this;
    vm.types = types;

    $scope.$watch('vm.loadNodes', (newVal) => {
        if (newVal) {
            initTree();
        }
    });

    function initTree() {
        vm.treeElement = angular.element('.tb-nav-tree-container', $element)
            .jstree(
                {
                    core: {
                        multiple: false,
                        check_callback: true,
                        themes: { name: 'proton', responsive: true },
                        data: vm.loadNodes
                    }
                }
            );

        vm.treeElement.on("changed.jstree", function (e, data) {
            if (vm.onNodeSelected) {
                vm.onNodeSelected({node: data.instance.get_selected(true)[0]});
            }
        });

        if (vm.editCallbacks) {
            vm.editCallbacks.selectNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('deselect_all', true);
                    vm.treeElement.jstree('select_node', node);
                }
            };
            vm.editCallbacks.deselectAll = () => {
                vm.treeElement.jstree('deselect_all');
            };
            vm.editCallbacks.getNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                return node;
            };
            vm.editCallbacks.getParentNodeId = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('get_parent', node);
                }
            };
            vm.editCallbacks.openNode = (id, cb) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('open_node', node, cb);
                }
            };
            vm.editCallbacks.nodeIsOpen = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('is_open', node);
                } else {
                    return true;
                }
            };
            vm.editCallbacks.nodeIsLoaded = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('is_loaded', node);
                } else {
                    return true;
                }
            };
            vm.editCallbacks.refreshNode = (id) => {
                if (id === '#') {
                    vm.treeElement.jstree('refresh');
                    vm.treeElement.jstree('redraw');
                } else {
                    var node = vm.treeElement.jstree('get_node', id);
                    if (node) {
                        var opened = vm.treeElement.jstree('is_open', node);
                        vm.treeElement.jstree('refresh_node', node);
                        vm.treeElement.jstree('redraw');
                        if (node.children && opened/* && !node.children.length*/) {
                            vm.treeElement.jstree('open_node', node);
                        }
                    }
                }
            };
            vm.editCallbacks.updateNode = (id, newName) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('rename_node', node, newName);
                }
            };
            vm.editCallbacks.createNode = (parentId, node, pos) => {
                var parentNode = vm.treeElement.jstree('get_node', parentId);
                if (parentNode) {
                    vm.treeElement.jstree('create_node', parentNode, node, pos);
                }
            };
            vm.editCallbacks.deleteNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('delete_node', node);
                }
            };
        }
    }
}
