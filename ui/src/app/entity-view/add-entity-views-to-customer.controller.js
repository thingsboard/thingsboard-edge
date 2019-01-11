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
/*@ngInject*/
export default function AddEntityViewsToCustomerController(/*entityViewService, $mdDialog, $q, customerId, entityViews*/) {
/*
    var vm = this;

    vm.entityViews = entityViews;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchEntityViewTextUpdated = searchEntityViewTextUpdated;
    vm.toggleEntityViewSelection = toggleEntityViewSelection;

    vm.theEntityViews = {
        getItemAtIndex: function (index) {
            if (index > vm.entityViews.data.length) {
                vm.theEntityViews.fetchMoreItems_(index);
                return null;
            }
            var item = vm.entityViews.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.entityViews.hasNext) {
                return vm.entityViews.data.length + vm.entityViews.nextPageLink.limit;
            } else {
                return vm.entityViews.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.entityViews.hasNext && !vm.entityViews.pending) {
                vm.entityViews.pending = true;
                entityViewService.getTenantEntityViews(vm.entityViews.nextPageLink, false).then(
                    function success(entityViews) {
                        vm.entityViews.data = vm.entityViews.data.concat(entityViews.data);
                        vm.entityViews.nextPageLink = entityViews.nextPageLink;
                        vm.entityViews.hasNext = entityViews.hasNext;
                        if (vm.entityViews.hasNext) {
                            vm.entityViews.nextPageLink.limit = vm.entityViews.pageSize;
                        }
                        vm.entityViews.pending = false;
                    },
                    function fail() {
                        vm.entityViews.hasNext = false;
                        vm.entityViews.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var entityViewId in vm.entityViews.selections) {
            tasks.push(entityViewService.assignEntityViewToCustomer(customerId, entityViewId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.entityViews.data.length == 0 && !vm.entityViews.hasNext;
    }

    function hasData() {
        return vm.entityViews.data.length > 0;
    }

    function toggleEntityViewSelection($event, entityView) {
        $event.stopPropagation();
        var selected = angular.isDefined(entityView.selected) && entityView.selected;
        entityView.selected = !selected;
        if (entityView.selected) {
            vm.entityViews.selections[entityView.id.id] = true;
            vm.entityViews.selectedCount++;
        } else {
            delete vm.entityViews.selections[entityView.id.id];
            vm.entityViews.selectedCount--;
        }
    }

    function searchEntityViewTextUpdated() {
        vm.entityViews = {
            pageSize: vm.entityViews.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.entityViews.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }
*/
}
