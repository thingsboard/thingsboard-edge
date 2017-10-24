/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
export default function AddDashboardsToCustomerController(dashboardService, $mdDialog, $q, customerId, dashboards) {

    var vm = this;

    vm.dashboards = dashboards;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchDashboardTextUpdated = searchDashboardTextUpdated;
    vm.toggleDashboardSelection = toggleDashboardSelection;

    vm.theDashboards = {
        getItemAtIndex: function (index) {
            if (index > vm.dashboards.data.length) {
                vm.theDashboards.fetchMoreItems_(index);
                return null;
            }
            var item = vm.dashboards.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.dashboards.hasNext) {
                return vm.dashboards.data.length + vm.dashboards.nextPageLink.limit;
            } else {
                return vm.dashboards.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.dashboards.hasNext && !vm.dashboards.pending) {
                vm.dashboards.pending = true;
                dashboardService.getTenantDashboards(vm.dashboards.nextPageLink, false).then(
                    function success(dashboards) {
                        vm.dashboards.data = vm.dashboards.data.concat(dashboards.data);
                        vm.dashboards.nextPageLink = dashboards.nextPageLink;
                        vm.dashboards.hasNext = dashboards.hasNext;
                        if (vm.dashboards.hasNext) {
                            vm.dashboards.nextPageLink.limit = vm.dashboards.pageSize;
                        }
                        vm.dashboards.pending = false;
                    },
                    function fail() {
                        vm.dashboards.hasNext = false;
                        vm.dashboards.pending = false;
                    });
            }
        }
    }

    function cancel () {
        $mdDialog.cancel();
    }

    function assign () {
        var tasks = [];
        for (var dashboardId in vm.dashboards.selections) {
            tasks.push(dashboardService.assignDashboardToCustomer(customerId, dashboardId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData () {
        return vm.dashboards.data.length == 0 && !vm.dashboards.hasNext;
    }

    function hasData () {
        return vm.dashboards.data.length > 0;
    }

    function toggleDashboardSelection ($event, dashboard) {
        $event.stopPropagation();
        var selected = angular.isDefined(dashboard.selected) && dashboard.selected;
        dashboard.selected = !selected;
        if (dashboard.selected) {
            vm.dashboards.selections[dashboard.id.id] = true;
            vm.dashboards.selectedCount++;
        } else {
            delete vm.dashboards.selections[dashboard.id.id];
            vm.dashboards.selectedCount--;
        }
    }

    function searchDashboardTextUpdated () {
        vm.dashboards = {
            pageSize: vm.dashboards.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.dashboards.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }
}