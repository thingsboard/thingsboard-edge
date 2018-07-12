/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import addDeviceTemplate from './add-device.tpl.html';
import deviceCard from './device-card.tpl.html';
import addDevicesToCustomerTemplate from './add-devices-to-customer.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function DeviceCardController(types) {

    var vm = this;

    vm.types = types;

    vm.isAssignedToCustomer = function() {
        if (vm.item && vm.item.customerId && vm.parentCtl.devicesScope === 'tenant' &&
            vm.item.customerId.id != vm.types.id.nullUid && !vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }

    vm.isPublic = function() {
        if (vm.item && vm.item.assignedCustomer && vm.parentCtl.devicesScope === 'tenant' && vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }
}


/*@ngInject*/
export function DeviceController($rootScope, tbDialogs, userService, deviceService, customerService, $state, $stateParams,
                                 $document, $mdDialog, $q, $translate, types) {

    var customerId = $stateParams.customerId;

    var deviceActionsList = [];

    var deviceGroupActionsList = [];

    var vm = this;

    vm.types = types;

    vm.deviceGridConfig = {
        deleteItemTitleFunc: deleteDeviceTitle,
        deleteItemContentFunc: deleteDeviceText,
        deleteItemsTitleFunc: deleteDevicesTitle,
        deleteItemsActionTitleFunc: deleteDevicesActionTitle,
        deleteItemsContentFunc: deleteDevicesText,

        saveItemFunc: saveDevice,

        getItemTitleFunc: getDeviceTitle,

        itemCardController: 'DeviceCardController',
        itemCardTemplateUrl: deviceCard,
        parentCtl: vm,

        actionsList: deviceActionsList,
        groupActionsList: deviceGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addDeviceTemplate,

        addItemText: function() { return $translate.instant('device.add-device-text') },
        noItemsText: function() { return $translate.instant('device.no-devices-text') },
        itemDetailsText: function() { return $translate.instant('device.device-details') },
        isDetailsReadOnly: isCustomerUser,
        isSelectionEnabled: function () {
            return !isCustomerUser();
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.deviceGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.deviceGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.devicesScope = $state.$current.data.devicesType;

    vm.assignToCustomer = assignToCustomer;
    vm.makePublic = makePublic;
    vm.unassignFromCustomer = unassignFromCustomer;
    vm.manageCredentials = manageCredentials;

    initController();

    function initController() {
        var fetchDevicesFunction = null;
        var deleteDeviceFunction = null;
        var refreshDevicesParamsFunction = null;

        var user = userService.getCurrentUser();

        if (user.authority === 'CUSTOMER_USER') {
            vm.devicesScope = 'customer_user';
            customerId = user.customerId;
        }
        if (customerId) {
            vm.customerDevicesTitle = $translate.instant('customer.devices');
            customerService.getShortCustomerInfo(customerId).then(
                function success(info) {
                    if (info.isPublic) {
                        vm.customerDevicesTitle = $translate.instant('customer.public-devices');
                    }
                }
            );
        }

        if (vm.devicesScope === 'tenant') {
            fetchDevicesFunction = function (pageLink, deviceType) {
                return deviceService.getTenantDevices(pageLink, true, null, deviceType);
            };
            deleteDeviceFunction = function (deviceId) {
                return deviceService.deleteDevice(deviceId);
            };
            refreshDevicesParamsFunction = function() {
                return {"topIndex": vm.topIndex};
            };

            deviceActionsList.push({
                onAction: function ($event, item) {
                    makePublic($event, item);
                },
                name: function() { return $translate.instant('action.share') },
                details: function() { return $translate.instant('device.make-public') },
                icon: "share",
                isEnabled: function(device) {
                    return device && (!device.customerId || device.customerId.id === types.id.nullUid);
                }
            });

            deviceActionsList.push(
                {
                    onAction: function ($event, item) {
                        assignToCustomer($event, [ item.id.id ]);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('device.assign-to-customer') },
                    icon: "assignment_ind",
                    isEnabled: function(device) {
                        return device && (!device.customerId || device.customerId.id === types.id.nullUid);
                    }
                }
            );

            deviceActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromCustomer($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('device.unassign-from-customer') },
                    icon: "assignment_return",
                    isEnabled: function(device) {
                        return device && device.customerId && device.customerId.id !== types.id.nullUid && !device.assignedCustomer.isPublic;
                    }
                }
            );

            deviceActionsList.push({
                onAction: function ($event, item) {
                    unassignFromCustomer($event, item, true);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('device.make-private') },
                icon: "reply",
                isEnabled: function(device) {
                    return device && device.customerId && device.customerId.id !== types.id.nullUid && device.assignedCustomer.isPublic;
                }
            });

            deviceActionsList.push(
                {
                    onAction: function ($event, item) {
                        manageCredentials($event, item);
                    },
                    name: function() { return $translate.instant('device.credentials') },
                    details: function() { return $translate.instant('device.manage-credentials') },
                    icon: "security"
                }
            );

            deviceActionsList.push(
                {
                    onAction: function ($event, item) {
                        vm.grid.deleteItem($event, item);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: function() { return $translate.instant('device.delete') },
                    icon: "delete"
                }
            );

            deviceGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        assignDevicesToCustomer($event, items);
                    },
                    name: function() { return $translate.instant('device.assign-devices') },
                    details: function(selectedCount) {
                        return $translate.instant('device.assign-devices-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_ind"
                }
            );

            deviceGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('device.delete-devices') },
                    details: deleteDevicesActionTitle,
                    icon: "delete"
                }
            );



        } else if (vm.devicesScope === 'customer' || vm.devicesScope === 'customer_user') {
            fetchDevicesFunction = function (pageLink, deviceType) {
                return deviceService.getCustomerDevices(customerId, pageLink, true, null, deviceType);
            };
            deleteDeviceFunction = function (deviceId) {
                return deviceService.unassignDeviceFromCustomer(deviceId);
            };
            refreshDevicesParamsFunction = function () {
                return {"customerId": customerId, "topIndex": vm.topIndex};
            };

            if (vm.devicesScope === 'customer') {
                deviceActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, false);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('device.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(device) {
                            return device && !device.assignedCustomer.isPublic;
                        }
                    }
                );
                deviceActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, true);
                        },
                        name: function() { return $translate.instant('action.make-private') },
                        details: function() { return $translate.instant('device.make-private') },
                        icon: "reply",
                        isEnabled: function(device) {
                            return device && device.assignedCustomer.isPublic;
                        }
                    }
                );

                deviceActionsList.push(
                    {
                        onAction: function ($event, item) {
                            manageCredentials($event, item);
                        },
                        name: function() { return $translate.instant('device.credentials') },
                        details: function() { return $translate.instant('device.manage-credentials') },
                        icon: "security"
                    }
                );

                deviceGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignDevicesFromCustomer($event, items);
                        },
                        name: function() { return $translate.instant('device.unassign-devices') },
                        details: function(selectedCount) {
                            return $translate.instant('device.unassign-devices-action-title', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_return"
                    }
                );

                vm.deviceGridConfig.addItemAction = {
                    onAction: function ($event) {
                        addDevicesToCustomer($event);
                    },
                    name: function() { return $translate.instant('device.assign-devices') },
                    details: function() { return $translate.instant('device.assign-new-device') },
                    icon: "add"
                };


            } else if (vm.devicesScope === 'customer_user') {
                deviceActionsList.push(
                    {
                        onAction: function ($event, item) {
                            manageCredentials($event, item);
                        },
                        name: function() { return $translate.instant('device.credentials') },
                        details: function() { return $translate.instant('device.view-credentials') },
                        icon: "security"
                    }
                );

                vm.deviceGridConfig.addItemAction = {};
            }
        }

        vm.deviceGridConfig.refreshParamsFunc = refreshDevicesParamsFunction;
        vm.deviceGridConfig.fetchItemsFunc = fetchDevicesFunction;
        vm.deviceGridConfig.deleteItemFunc = deleteDeviceFunction;

    }

    function deleteDeviceTitle(device) {
        return $translate.instant('device.delete-device-title', {deviceName: device.name});
    }

    function deleteDeviceText() {
        return $translate.instant('device.delete-device-text');
    }

    function deleteDevicesTitle(selectedCount) {
        return $translate.instant('device.delete-devices-title', {count: selectedCount}, 'messageformat');
    }

    function deleteDevicesActionTitle(selectedCount) {
        return $translate.instant('device.delete-devices-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteDevicesText () {
        return $translate.instant('device.delete-devices-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getDeviceTitle(device) {
        return device ? device.name : '';
    }

    function saveDevice(device) {
        var deferred = $q.defer();
        deviceService.saveDevice(device).then(
            function success(savedDevice) {
                $rootScope.$broadcast('deviceSaved');
                var devices = [ savedDevice ];
                customerService.applyAssignedCustomersInfo(devices).then(
                    function success(items) {
                        if (items && items.length == 1) {
                            deferred.resolve(items[0]);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function isCustomerUser() {
        return vm.devicesScope === 'customer_user';
    }

    function assignToCustomer($event, deviceIds) {
        tbDialogs.assignDevicesToCustomer($event, deviceIds).then(
            () => {
                vm.grid.refreshList();
            }
        );
    }

    function addDevicesToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        deviceService.getTenantDevices({limit: pageSize, textSearch: ''}, false).then(
            function success(_devices) {
                var devices = {
                    pageSize: pageSize,
                    data: _devices.data,
                    nextPageLink: _devices.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _devices.hasNext,
                    pending: false
                };
                if (devices.hasNext) {
                    devices.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddDevicesToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: addDevicesToCustomerTemplate,
                    locals: {customerId: customerId, devices: devices},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    vm.grid.refreshList();
                }, function () {
                });
            },
            function fail() {
            });
    }

    function assignDevicesToCustomer($event, items) {
        var deviceIds = [];
        for (var id in items.selections) {
            deviceIds.push(id);
        }
        assignToCustomer($event, deviceIds);
    }

    function unassignFromCustomer($event, device, isPublic) {
        tbDialogs.unassignDeviceFromCustomer($event, device, isPublic).then(
            () => {
                vm.grid.refreshList();
            }
        );
    }

    function unassignDevicesFromCustomer($event, items) {
        var deviceIds = [];
        for (var id in items.selections) {
            deviceIds.push(id);
        }
        tbDialogs.unassignDevicesFromCustomer($event, deviceIds).then(
            () => {
                vm.grid.refreshList();
            }
        );
    }

    function makePublic($event, device) {
        tbDialogs.makeDevicePublic($event, device).then(
            () => {
                vm.grid.refreshList();
            }
        );
    }

    function manageCredentials($event, device) {
        tbDialogs.manageDeviceCredentials($event, device, isCustomerUser());
    }
}
