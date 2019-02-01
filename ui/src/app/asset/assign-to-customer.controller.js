/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
export default function AssignAssetToCustomerController(/*customerService, assetService, $mdDialog, $q, assetIds, customers*/) {
/*
    var vm = this;

    vm.customers = customers;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isCustomerSelected = isCustomerSelected;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchCustomerTextUpdated = searchCustomerTextUpdated;
    vm.toggleCustomerSelection = toggleCustomerSelection;

    vm.theCustomers = {
        getItemAtIndex: function (index) {
            if (index > vm.customers.data.length) {
                vm.theCustomers.fetchMoreItems_(index);
                return null;
            }
            var item = vm.customers.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.customers.hasNext) {
                return vm.customers.data.length + vm.customers.nextPageLink.limit;
            } else {
                return vm.customers.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.customers.hasNext && !vm.customers.pending) {
                vm.customers.pending = true;
                customerService.getCustomers(vm.customers.nextPageLink).then(
                    function success(customers) {
                        vm.customers.data = vm.customers.data.concat(customers.data);
                        vm.customers.nextPageLink = customers.nextPageLink;
                        vm.customers.hasNext = customers.hasNext;
                        if (vm.customers.hasNext) {
                            vm.customers.nextPageLink.limit = vm.customers.pageSize;
                        }
                        vm.customers.pending = false;
                    },
                    function fail() {
                        vm.customers.hasNext = false;
                        vm.customers.pending = false;
                    });
            }
        }
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var i=0;i<assetIds.length;i++) {
            tasks.push(assetService.assignAssetToCustomer(vm.customers.selection.id.id, assetIds[i]));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.customers.data.length == 0 && !vm.customers.hasNext;
    }

    function hasData() {
        return vm.customers.data.length > 0;
    }

    function toggleCustomerSelection($event, customer) {
        $event.stopPropagation();
        if (vm.isCustomerSelected(customer)) {
            vm.customers.selection = null;
        } else {
            vm.customers.selection = customer;
        }
    }

    function isCustomerSelected(customer) {
        return vm.customers.selection != null && customer &&
            customer.id.id === vm.customers.selection.id.id;
    }

    function searchCustomerTextUpdated() {
        vm.customers = {
            pageSize: vm.customers.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.customers.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }*/
}
