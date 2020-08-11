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
export default function AddEntityGroupsToEdgeController(entityGroupService, $mdDialog, $q, $filter, types, edgeId, entityGroups, groupType, utils) {

    var vm = this;

    vm.entityGroups = entityGroups;
    vm.groupType = groupType;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchEntityGroupTextUpdated = searchEntityGroupTextUpdated;
    vm.toggleEntityGroupSelection = toggleEntityGroupSelection;

    vm.noEntitiesText = 'entity-group.no-entity-groups-text';

    if (vm.groupType == types.entityType.user) {
        vm.titleText = 'entity-group.assign-user-group-to-edge';
        vm.assignText = 'entity-group.assign-user-group-to-edge-text';
    } else if (vm.groupType == types.entityType.asset) {
        vm.titleText = 'entity-group.assign-asset-group-to-edge';
        vm.assignText = 'entity-group.assign-asset-group-to-edge-text';
    } else if (vm.groupType == types.entityType.device) {
        vm.titleText = 'entity-group.assign-device-group-to-edge';
        vm.assignText = 'entity-group.assign-device-group-to-edge-text';
    } else if (vm.groupType == types.entityType.entityView) {
        vm.titleText = 'entity-group.assign-entity-view-group-to-edge';
        vm.assignText = 'entity-group.assign-entity-view-group-to-edge-text';
    } else if (vm.groupType == types.entityType.dashboard) {
        vm.titleText = 'entity-group.assign-dashboard-group-to-edge';
        vm.assignText = 'entity-group.assign-dashboard-group-to-edge-text';
    }

    vm.theEntityGroups = {
        getItemAtIndex: function (index) {
            if (index > vm.entityGroups.data.length) {
                vm.theEntityGroups.fetchMoreItems_(index);
                return null;
            }
            var item = vm.entityGroups.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.entityGroups.hasNext) {
                return vm.entityGroups.data.length + vm.entityGroups.nextPageLink.limit;
            } else {
                return vm.entityGroups.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.entityGroups.hasNext && !vm.entityGroups.pending) {
                vm.entityGroups.pending = true;
                fetchEntityGroups().then(
                    function success(_entityGroups) {
                        var entityGroupsExcludeAll = $filter('filter')(_entityGroups.data, {groupAll: false, $: vm.searchText});
                        vm.entityGroups.data = entityGroupsExcludeAll;
                        vm.entityGroups.nextPageLink = _entityGroups.nextPageLink;
                        vm.entityGroups.hasNext = _entityGroups.hasNext;
                        if (vm.entityGroups.hasNext) {
                            vm.entityGroups.nextPageLink.limit = vm.entityGroups.pageSize;
                        }
                        vm.entityGroups.pending = false;
                    },
                    function fail() {
                        vm.entityGroups.hasNext = false;
                        vm.entityGroups.pending = false;
                    });
            }
        }
    }

    function fetchEntityGroups() {
        var deferred = $q.defer();
        var fetchPromise;
        if (vm.customerId) {
            fetchPromise = entityGroupService.getEntityGroupsByOwnerId(types.entityType.customer, vm.customerId, vm.groupType);
        } else {
            fetchPromise = entityGroupService.getEntityGroups(vm.groupType);
        }
        fetchPromise.then(
            function success(_entityGroups) {
                utils.filterSearchTextEntities(_entityGroups, 'name', {}, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function cancel () {
        $mdDialog.cancel();
    }

    function assign () {
        var tasks = [];
        for (var entityGroupId in vm.entityGroups.selections) {
            tasks.push(entityGroupService.assignEntityGroupToEdge(edgeId, entityGroupId, groupType));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData () {
        return vm.entityGroups.data.length == 0 && !vm.entityGroups.hasNext;
    }

    function hasData () {
        return vm.entityGroups.data.length > 0;
    }

    function toggleEntityGroupSelection ($event, entityGroup) {
        $event.stopPropagation();
        var selected = angular.isDefined(entityGroup.selected) && entityGroup.selected;
        entityGroup.selected = !selected;
        if (entityGroup.selected) {
            vm.entityGroups.selections[entityGroup.id.id] = true;
            vm.entityGroups.selectedCount++;
        } else {
            delete vm.entityGroups.selections[entityGroup.id.id];
            vm.entityGroups.selectedCount--;
        }
    }

    function searchEntityGroupTextUpdated () {
        vm.entityGroups = {
            pageSize: vm.entityGroups.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.entityGroups.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }
}
