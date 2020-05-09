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
export default function AssignDeviceToEdgeController(edgeService, deviceService, $mdDialog, $q, deviceIds, edges) {

    var vm = this;

    vm.edges = edges;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isEdgeSelected = isEdgeSelected;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchEdgeTextUpdated = searchEdgeTextUpdated;
    vm.toggleEdgeSelection = toggleEdgeSelection;

    vm.theEdges = {
        getItemAtIndex: function (index) {
            if (index > vm.edges.data.length) {
                vm.theEdges.fetchMoreItems_(index);
                return null;
            }
            var item = vm.edges.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.edges.hasNext) {
                return vm.edges.data.length + vm.edges.nextPageLink.limit;
            } else {
                return vm.edges.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.edges.hasNext && !vm.edges.pending) {
                vm.edges.pending = true;
                edgeService.getEdges(vm.edges.nextPageLink).then(
                    function success(edges) {
                        vm.edges.data = vm.edges.data.concat(edges.data);
                        vm.edges.nextPageLink = edges.nextPageLink;
                        vm.edges.hasNext = edges.hasNext;
                        if (vm.edges.hasNext) {
                            vm.edges.nextPageLink.limit = vm.edges.pageSize;
                        }
                        vm.edges.pending = false;
                    },
                    function fail() {
                        vm.edges.hasNext = false;
                        vm.edges.pending = false;
                    });
            }
        }
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var i=0;i<deviceIds.length;i++) {
            tasks.push(deviceService.assignDeviceToEdge(vm.edges.selection.id.id, deviceIds[i]));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.edges.data.length == 0 && !vm.edges.hasNext;
    }

    function hasData() {
        return vm.edges.data.length > 0;
    }

    function toggleEdgeSelection($event, edge) {
        $event.stopPropagation();
        if (vm.isEdgeSelected(edge)) {
            vm.edges.selection = null;
        } else {
            vm.edges.selection = edge;
        }
    }

    function isEdgeSelected(edge) {
        return vm.edges.selection != null && edge &&
            edge.id.id === vm.edges.selection.id.id;
    }

    function searchEdgeTextUpdated() {
        vm.edges = {
            pageSize: vm.edges.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.edges.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }
}
