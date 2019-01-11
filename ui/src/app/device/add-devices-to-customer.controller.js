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
export default function AddDevicesToCustomerController(/*deviceService, $mdDialog, $q, customerId, devices*/) {
/*
    var vm = this;

    vm.devices = devices;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchDeviceTextUpdated = searchDeviceTextUpdated;
    vm.toggleDeviceSelection = toggleDeviceSelection;

    vm.theDevices = {
        getItemAtIndex: function (index) {
            if (index > vm.devices.data.length) {
                vm.theDevices.fetchMoreItems_(index);
                return null;
            }
            var item = vm.devices.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.devices.hasNext) {
                return vm.devices.data.length + vm.devices.nextPageLink.limit;
            } else {
                return vm.devices.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.devices.hasNext && !vm.devices.pending) {
                vm.devices.pending = true;
                deviceService.getTenantDevices(vm.devices.nextPageLink, false).then(
                    function success(devices) {
                        vm.devices.data = vm.devices.data.concat(devices.data);
                        vm.devices.nextPageLink = devices.nextPageLink;
                        vm.devices.hasNext = devices.hasNext;
                        if (vm.devices.hasNext) {
                            vm.devices.nextPageLink.limit = vm.devices.pageSize;
                        }
                        vm.devices.pending = false;
                    },
                    function fail() {
                        vm.devices.hasNext = false;
                        vm.devices.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var deviceId in vm.devices.selections) {
            tasks.push(deviceService.assignDeviceToCustomer(customerId, deviceId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.devices.data.length == 0 && !vm.devices.hasNext;
    }

    function hasData() {
        return vm.devices.data.length > 0;
    }

    function toggleDeviceSelection($event, device) {
        $event.stopPropagation();
        var selected = angular.isDefined(device.selected) && device.selected;
        device.selected = !selected;
        if (device.selected) {
            vm.devices.selections[device.id.id] = true;
            vm.devices.selectedCount++;
        } else {
            delete vm.devices.selections[device.id.id];
            vm.devices.selectedCount--;
        }
    }

    function searchDeviceTextUpdated() {
        vm.devices = {
            pageSize: vm.devices.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.devices.pageSize,
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
